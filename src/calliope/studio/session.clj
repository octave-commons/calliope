(ns calliope.studio.session
  "GPL-3.0-or-later. In-process native session boundary; no devices or providers.
   Clio owns generic event identity, admission, locking, durability and recovery."
  (:require [calliope.law.session :as law]
            [calliope.studio.session-domain :as domain]
            [calliope.studio.session-index :as index]
            [calliope.studio.sources :as sources]
            [calliope.studio.ingress :as ingress]
            [clio.extern.jvm.fs :as fs]
            [clio.infra.event :as event]
            [clio.infra.ledger :as ledger]
            [clio.infra.runtime :as runtime]
            [clio.infra.schema-store :as schema-store]
            [clio.domain.schema :as schema]
            [clio.law.append :as append-law])
  (:import [java.nio.file Files Path LinkOption]
           [java.nio.file.attribute BasicFileAttributes PosixFilePermissions]
           [java.util UUID]))

(def no-links (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn- refuse! [code message]
  (throw (ex-info message {:error/code code})))

(defn- user! [actor]
  (when-not (and (law/valid? :calliope.studio/actor-v1 actor) (= :user (:actor/kind actor)))
    (refuse! :command/not-authorized "A configured local user is required")))

(defn- authorized! [store actor]
  (user! actor)
  (when-not (= (:actor store) actor)
    (refuse! :command/not-authorized "Only this native service's configured user may command it")))

(defn- configured [^Path root actor]
  {:root root :actor actor :process/id (str (UUID/randomUUID))
   :ledger/path (.resolve root "events.edn") :index/path (.resolve root "index.sqlite")
   :command-lock/path (.resolve root "commands.lock")
   :source/root (.resolve root "sources") :schema/root (.resolve root "schemas")
   :staging/root (.resolve root "staging")
   :root-key (.fileKey (Files/readAttributes root BasicFileAttributes no-links))
   :state (atom domain/empty-state) :grant (atom nil) :pending (atom nil)
   :capture/state (atom :unarmed) :closed? (atom false)})

(defn- guard! [store]
  (when @(:closed? store) (refuse! :session/closed "The session service is closed"))
  (let [attrs (Files/readAttributes ^Path (:root store) BasicFileAttributes no-links)]
    (when-not (and (.isDirectory attrs) (= (:root-key store) (.fileKey attrs)))
      (refuse! :session/store-changed "The configured session directory changed")))
  (doseq [key [:source/root :schema/root :staging/root]
          :let [attrs (Files/readAttributes ^Path (get store key) BasicFileAttributes no-links)]]
    (when-not (.isDirectory attrs) (refuse! :session/store-changed "A session directory was replaced"))))

(defn- with-command-lock [store f]
  (guard! store)
  ;; This distinct application mutex covers source publication plus command
  ;; admission across JVMs. Its lock implementation is Clio's owning adapter;
  ;; Clio independently locks the event ledger during its own admission/fence.
  (let [token (fs/acquire-lock! (str (:command-lock/path store)))]
    (try (guard! store) (f)
         (finally (fs/release-lock! token)))))

(defn- disarm! [store capture-state]
  (when-let [grant @(:grant store)] (.close ^java.io.Closeable grant))
  (reset! (:grant store) nil)
  (reset! (:capture/state store) capture-state))

(defn close! [{:keys [state] :as store}]
  (locking state
    (disarm! store :unarmed)
    (reset! (:closed? store) true)))

(defn staging-root [store] (:staging/root store))
(defn history [store] (:events @(:state store)))
(defn pending-recovery [store] @(:pending store))

(defn view
  "Read the last successful projection. Historical arms never create live grants."
  [store]
  (let [state @(:state store)]
    {:session (when (:session/id state) (dissoc state :events :requests))
     :event/ids (mapv :event/id (:events state))
     :capture/state @(:capture/state store) :recovery/pending? (boolean @(:pending store))}))

(defn query-rows [store query]
  (index/query-rows (:index/path store) query))

(defn source-path!
  "Resolve an admitted source ID only after checking its current actual bytes."
  [store source-id]
  (guard! store)
  (if-let [source (get (domain/sources @(:state store)) source-id)]
    (str (sources/verify! (:source/root store) (:source/sha256 source)))
    (refuse! :source/not-found "Source identity is not in the admitted session")))

(defn- read-state! [store]
  (runtime/ensure-durable! (:runtime store) (str (:ledger/path store)))
  (domain/replay (:canonical/events (runtime/canonicalize-files (:runtime store) [(str (:ledger/path store))]))))

(defn- verify-sources! [store state]
  (doseq [digest (set (map :source/sha256 (vals (domain/sources state))))]
    (sources/ensure-durable! (:source/root store) digest))
  state)

(defn- project! [store state]
  (verify-sources! store state)
  (index/rebuild! (:index/path store) state)
  (reset! (:state store) state)
  state)

(defn reload!
  "Explicit recovery/reload fences history, verifies sources and rebuilds SQLite.
   It clears live capture, never opens devices, and does not append an event."
  [{:keys [state] :as store}]
  (locking state
    (disarm! store :unarmed)
    (with-command-lock store #(project! store (read-state! store)))
    (reset! (:pending store) nil)
    (view store)))

(defn create!
  "Create a fresh private store. Existing paths are never adopted or overwritten."
  [^Path root actor]
  (user! actor)
  (try
    (let [root (.normalize (.toAbsolutePath root))]
      (fs/require-directory-sync-support! (str (.getParent root)))
      (Files/createDirectory root (into-array java.nio.file.attribute.FileAttribute
                                             [(PosixFilePermissions/asFileAttribute
                                               (PosixFilePermissions/fromString "rwx------"))]))
      (doseq [directory [root (.resolve root "sources") (.resolve root "staging")]]
        (fs/ensure-dir! (str directory)))
      (let [rt (runtime/open (str (.resolve root "schemas")) law/catalog)]
        (ledger/create-ledger! (str (.resolve root "events.edn")))
        (fs/create-exclusive! (str (.resolve root "commands.lock")))
        (let [store (assoc (configured (.toRealPath root (make-array LinkOption 0)) actor) :runtime rt)]
          (with-command-lock store #(project! store domain/empty-state))
          store)))
    (catch Exception cause
      (throw (ex-info "Could not create a fresh session store"
                      {:error/code (or (:error/code (ex-data cause)) :session/store-unavailable)} cause)))))

(defn open!
  "Recover existing history before publishing any current catalog revision.
   Missing history/schemas/sources fail explicitly; no empty session is invented."
  [^Path root actor]
  (user! actor)
  (try
    (let [root (.toRealPath root (make-array LinkOption 0)) base (configured root actor)]
      (with-command-lock
        base
        (fn []
          (let [revisions (schema-store/load-revisions (str (:schema/root base)))]
            (when-not (seq revisions) (refuse! :session/schemas-unavailable "Historical session schemas are unavailable"))
            (ledger/ensure-durable! revisions (str (:ledger/path base)))
            (let [state (domain/replay (:canonical/events
                                        (ledger/canonicalize-files revisions [(str (:ledger/path base))])))
                  store (assoc base :runtime (runtime/open (str (:schema/root base)) law/catalog))]
              (project! store state)
              store)))))
    (catch Exception cause
      (throw (ex-info "Session recovery failed; existing artifacts are preserved"
                      {:error/code (or (:error/code (ex-data cause)) (:clio/error (ex-data cause)) :session/store-unavailable)} cause)))))

(defn- request-id [command]
  (when (law/valid? :calliope.capture/uuid-v1 (:request/id command)) (:request/id command)))

(defn- error-result [command cause]
  {:result/status :error :request/id (request-id command)
   :error/code (or (:error/code (ex-data cause)) (:clio/error (ex-data cause)) :session/storage-unavailable)
   :error/message (or (ex-message cause) "Session operation failed")})

(defn- refusal-result [command refusal]
  (merge {:result/status :error :request/id (request-id command)} (select-keys refusal [:error/code :error/message])))

(defn- success-result [command record]
  {:result/status :ok :request/id (:request/id command)
   :event/id (:event/id record) :result/value (:value record)})

(defn- uncertain-result [command candidate cause]
  {:result/status :uncertain :request/id (:request/id command) :append/recovery candidate
   :error/code (or (:error/code (ex-data cause)) (:clio/error (ex-data cause)) :session/append-uncertain)
   :error/message "Persistence did not finish successfully; inspect or retry this exact recovery candidate"})

(defn- live-state [store state]
  (assoc state :capture/armed? (and (= :armed @(:capture/state store))
                                   (some? @(:grant store)) (not @(:closed @(:grant store))))
               :process/id (:process/id store)))

(defn ui-command!
  "Trusted UI-user command entrypoint. All cheap authority/shape/retry/arm/revision
   checks precede source payload access. Provider work never owns this lock."
  [{:keys [state] :as store} actor command]
  (let [candidate (volatile! nil) persistence-started? (volatile! false) prepared-grant (volatile! nil)]
    (try
      (authorized! store actor)
      (when-not (law/valid? :calliope.session/command-v1 command)
        (refuse! :command/invalid "Malformed version1 session command"))
      (locking state
        (when @(:closed? store) (refuse! :session/closed "The session service is closed"))
        (when (= :studio/stop (:command/type command)) (disarm! store :stopped))
        (when @(:pending store) (refuse! :session/recovery-required "Resolve the uncertain append before another command"))
        (let [preflight (domain/plan (live-state store @(:state store)) actor command)]
          (cond
            (not (:ok? preflight)) (refusal-result command preflight)
            (:reused preflight) (success-result command (:reused preflight))
            :else
            (with-command-lock
              store
              (fn []
                (let [state (read-state! store) admitted (domain/plan (live-state store state) actor command)]
                  (cond
                    (not (:ok? admitted)) (refusal-result command admitted)
                    (:reused admitted)
                    (do (project! store state) (success-result command (:reused admitted)))
                    :else
                    (let [_ (fs/require-directory-sync-support! (str (:root store)))
                          _ (fs/require-directory-sync-support! (str (:source/root store)))
                          _ (when (= :arm (:effect admitted))
                              (vreset! prepared-grant
                                       (ingress/open-grant! (:staging/root store)
                                                            {:session/id (:session/id command) :process/id (:process/id store)
                                                             :actor actor :clock (:capture/clock command) :origin (:capture/origin command)})))
                          snapshot (when (= :ingest (:effect admitted))
                                     (ingress/ui-ingest! @(:grant store) (live-state store state) actor command))
                          value-result (if snapshot
                                         (if (:ok? snapshot) (domain/take-value state command (:take snapshot)) snapshot)
                                         admitted)]
                      (if-not (:ok? value-result)
                        (refusal-result command value-result)
                        (let [record {:record/version 1 :record/actor actor :record/command command :record/value (:value value-result)}
                              next-event (event/make-event (:schema/current (:runtime store)) law/event-type
                                                           {:event/stream (domain/stream-id (:session/id command))
                                                            :event/seq (inc (:session/revision state))
                                                            :event/causes (if-let [previous (peek (:events state))] [(:event/id previous)] [])
                                                            :event/actor (:actor/id actor) :event/subject (domain/subject-id (:session/id command))
                                                            :event/data record})
                              next-state (domain/apply-record state next-event)
                              recovery {:ledger/path (str (:ledger/path store)) :event next-event}]
                          (vreset! candidate recovery)
                          (vreset! persistence-started? true)
                          (when snapshot (sources/publish! (:source/root store) ((juxt :audio :midi) (:sources snapshot))))
                          (runtime/retry-append! (:runtime store) recovery)
                          (project! store next-state)
                          (reset! (:pending store) nil)
                          (when @prepared-grant
                            (disarm! store :unarmed)
                            (reset! (:grant store) @prepared-grant)
                            (vreset! prepared-grant nil)
                            (reset! (:capture/state store) :armed))
                          (success-result command (get-in next-state [:requests (:request/id command)]))))))))))))
      (catch Exception cause
        (if (and @persistence-started? @candidate)
          (do (disarm! store :unarmed) (reset! (:pending store) @candidate)
              (uncertain-result command @candidate cause))
          (error-result command cause)))
      (finally (when @prepared-grant (.close ^java.io.Closeable @prepared-grant))))))

(defn provider-command! [_ _ command]
  (refusal-result command {:error/code :command/not-authorized :error/message "Provider completion cannot issue user session commands"}))

(defn system-command! [_ _ command]
  (refusal-result command {:error/code :command/not-authorized :error/message "System completion cannot issue user session commands"}))

(defn retry!
  "Explicitly fence the exact Clio candidate. Never reconstruct identity/time or
   recreate an arm grant. Candidates cannot choose another ledger or actor."
  [{:keys [state] :as store} actor recovery]
  (let [command (get-in recovery [:event :event/data :record/command]) attempted? (volatile! false)]
    (try
      (authorized! store actor)
      (append-law/validate-recovery! recovery)
      (when-not (and (= (str (:ledger/path store)) (:ledger/path recovery))
                     (= actor (get-in recovery [:event :event/data :record/actor]))
                     (or (nil? @(:pending store)) (= recovery @(:pending store))))
        (refuse! :command/not-authorized "Recovery must retain this service's original candidate and actor"))
      (locking state
        (disarm! store :unarmed)
        (with-command-lock
          store
          (fn []
            (let [rt (runtime/refresh (:runtime store)) candidate-event (:event recovery)
                  _ (schema/validate-event! (:schema/revisions rt) candidate-event)
                  state (domain/replay (:canonical/events (runtime/canonicalize-files rt [(str (:ledger/path store))])))
                  existing (get-in state [:requests (:request/id command)])
                  next-state (if existing
                               (do (when-not (= candidate-event (some #(when (= (:event/id %) (:event/id candidate-event)) %) (:events state)))
                                     (refuse! :request/conflict "Recovery candidate conflicts with an admitted request"))
                                   state)
                               (domain/apply-record state candidate-event))]
              (verify-sources! store next-state)
              (vreset! attempted? true)
              (runtime/retry-append! rt recovery)
              (project! store next-state)
              (reset! (:pending store) nil)
              (success-result command (get-in next-state [:requests (:request/id command)]))))))
      (catch Exception cause
        (if @attempted?
          (do (reset! (:pending store) recovery) (uncertain-result command recovery cause))
          (error-result command cause))))))
