(ns calliope.studio.i3
  "One-shot desktop effects. Native service owns this controller, not model data.
   Applying accepts an opaque stored plan ID, never caller-authored commands."
  (:require [calliope.law.desktop :as law]
            [calliope.studio.desktop :as desktop]
            [clojure.data.json :as json]
            [clojure.java.shell :as shell])
  (:import [java.io IOException]
           [java.security MessageDigest]
           [java.nio.charset StandardCharsets]
           [java.util UUID]))

(defn controller [{:keys [read! execute!]}]
  (when-not (and (ifn? read!) (ifn? execute!))
    (throw (ex-info "Desktop adapter requires explicit read and execute capabilities" {})))
  {:read! read! :execute! execute!
   :state (atom {:connected? true :generation (str (UUID/randomUUID)) :plans {}})})

(defn- disconnect! [controller]
  (swap! (:state controller) assoc :connected? false :plans {}))

(defn- read-current! [controller]
  (if-not (:connected? @(:state controller))
    {:ok? false :error/code :desktop/unavailable}
    (try
      (let [result ((:read! controller))]
        (if (and (:ok? result) (law/valid? :calliope.desktop/snapshot-v1 (:snapshot result)))
          result
          (do (disconnect! controller) {:ok? false :error/code :desktop/unavailable})))
      (catch Exception _ (disconnect! controller) {:ok? false :error/code :desktop/unavailable}))))

(defn preview! [{:keys [state] :as controller} profile]
  (locking state
    (let [read (read-current! controller)]
      (if-not (:ok? read) read
        (let [planned (desktop/plan (:snapshot read) profile)]
          (if-not (:ok? planned) planned
            (let [id (str (UUID/randomUUID))
                  stored {:snapshot (:snapshot read) :plan (:plan planned)
                          :generation (:generation @(:state controller))}]
              (swap! (:state controller) assoc :plans {id stored})
              {:ok? true :plan/id id :preview (:plan planned)})))))))

(defn reconnect! [{:keys [state] :as controller}]
  (locking state
    (reset! (:state controller) {:connected? true :generation (str (UUID/randomUUID)) :plans {}})
    (read-current! controller)))

(defn- partial-result [code applied uncertain?]
  {:ok? false :status (if (or uncertain? (seq applied)) :partial :unavailable)
   :error/code code :applied applied :uncertain? uncertain?})

(defn- execute! [controller operation]
  (try
    ((:execute! controller) operation (desktop/command operation))
    (catch Exception _ {:ok? false :error/code :desktop/unavailable})))

(defn- effect-established? [snapshot operation]
  (let [window (get-in snapshot [:windows (:id operation)])]
    (and window
         (case (:op operation)
           :floating-enable (:floating? window)
           :resize (= (select-keys operation [:width :height]) (select-keys (:rect window) [:width :height]))
           :move (= (select-keys operation [:x :y]) (select-keys (:rect window) [:x :y]))))))

(defn apply!
  "Consume one exact preview. Stale/manual changes cancel before any effects.
   Mid-apply change/disconnect stops further commands; no rollback or reassertion.
   Fixed layout commands never request focus. Success verifies unchanged focus."
  [{:keys [state] :as controller} plan-id]
  (locking state
    (let [stored (get-in @(:state controller) [:plans plan-id])]
      (swap! (:state controller) update :plans dissoc plan-id)
      (if-not stored {:ok? false :error/code :desktop/unknown-plan}
        (let [read (read-current! controller) plan (:plan stored)]
          (cond
            (not (:ok? read)) (partial-result :desktop/unavailable [] false)
            (or (not= (:generation stored) (:generation @(:state controller)))
                (not= (:snapshot stored) (:snapshot read)))
            {:ok? false :status :cancelled :error/code :desktop/stale-plan :applied []}
            :else
            (loop [remaining (:operations plan) baseline (:snapshot read) applied []]
              (if-let [operation (first remaining)]
                (let [before (read-current! controller)]
                  (cond
                    (not (:ok? before)) (partial-result :desktop/unavailable applied false)
                    (not= baseline (:snapshot before)) (partial-result :desktop/user-change applied false)
                    :else
                    (let [result (execute! controller operation)]
                      (if-not (:ok? result)
                        (do (disconnect! controller)
                            (assoc (partial-result (or (:error/code result) :desktop/refused) applied true)
                                   :attempted operation))
                        (let [applied (conj applied operation) after (read-current! controller)]
                          (cond
                            (not (:ok? after)) (partial-result :desktop/unavailable applied false)
                            (not= (:expected/focus plan) (get-in after [:snapshot :focus]))
                            (partial-result :desktop/focus-changed applied false)
                            (not (effect-established? (:snapshot after) operation))
                            (partial-result :desktop/effect-not-established applied false)
                            :else (recur (next remaining) (:snapshot after) applied)))))))
                (if (every? #(= (:rect %) (get-in baseline [:windows (:id %) :rect])) (:targets plan))
                  {:ok? true :status (if (seq applied) :applied :unchanged)
                   :applied applied :snapshot baseline :skipped (:skipped plan)}
                  (partial-result :desktop/layout-not-established applied false))))))))))

(defn- hash-tree [tree]
  (.formatHex (java.util.HexFormat/of)
              (.digest (MessageDigest/getInstance "SHA-256")
                       (.getBytes (pr-str tree) StandardCharsets/UTF_8))))

(defn snapshot-from-tree
  "Reduce i3's tree to geometry/identity/focus; retain raw-tree hash for staleness.
   Window titles/class strings never become command text or layout authority."
  [tree]
  (let [found (atom [])]
    (letfn [(visit [node workspace floating?]
              (let [workspace (if (= "workspace" (:type node)) (select-keys node [:id :rect]) workspace)
                    floating? (or floating? (= "floating_con" (:type node))
                                  (contains? #{"auto_on" "user_on"} (:floating node)))]
                (when (and (:window node) workspace)
                  (swap! found conj {:window {:id (:id node) :workspace (:id workspace) :rect (:rect node)
                                               :floating? (boolean floating?)
                                               :fullscreen? (pos? (or (:fullscreen_mode node) 0))}
                                     :workspace workspace :focused? (true? (:focused node))}))
                (doseq [child (concat (:nodes node) (:floating_nodes node))]
                  (visit child workspace floating?))))]
      (visit tree nil false)
      (let [focus (first (filter :focused? @found))
            snapshot {:revision (hash-tree tree) :focus (get-in focus [:window :id])
                      :workspace (:workspace focus)
                      :windows (into {} (map (juxt (comp :id :window) :window) @found))}]
        (if (and (= 1 (count (filter :focused? @found)))
                 (= (count @found) (count (:windows snapshot)))
                 (law/valid? :calliope.desktop/snapshot-v1 snapshot))
          {:ok? true :snapshot snapshot}
          {:ok? false :error/code :desktop/invalid-snapshot})))))

(defn- native-call [socket type value]
  (try
    (let [args (cond-> ["/usr/bin/timeout" "--kill-after=1s" "3s" "/usr/bin/i3-msg"
                        "-s" socket "-t" type "-r"] value (conj value))
          {:keys [exit out]} (apply shell/sh args)]
      (if (zero? exit) {:ok? true :value (json/read-str out :key-fn keyword)}
          {:ok? false :error/code :desktop/unavailable}))
    (catch IOException _ {:ok? false :error/code :desktop/unavailable})
    (catch Exception _ {:ok? false :error/code :desktop/invalid-response})))

(defn read-snapshot!
  "Read-only, explicitly selected socket. Does not apply a layout."
  [socket]
  (let [result (native-call socket "get_tree" nil)]
    (if (:ok? result) (snapshot-from-tree (:value result)) result)))

(defn native-controller [socket]
  (controller {:read! #(read-snapshot! socket)
               :execute! (fn [_ command]
                           (let [result (native-call socket "command" command)]
                             (if (and (:ok? result) (vector? (:value result)) (seq (:value result))
                                      (every? #(true? (:success %)) (:value result)))
                               {:ok? true}
                               {:ok? false :error/code :desktop/unavailable})))}))
