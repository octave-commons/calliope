(ns calliope.studio.session-domain
  "GPL-3.0-or-later. Pure Calliope transitions over Clio-admitted history."
  (:require [calliope.law.session :as law]))

(def empty-state {:session/revision 0 :requests {} :takes {} :events []})

(defn stream-id [id] (str "studio-session/" id))
(defn subject-id [id] (str "session/" id))

(defn- refuse [code message]
  {:ok? false :error/code code :error/message message})

(defn plan
  "Admit intent before filesystem effects. :capture/armed? is supplied by the
   live service, or from historical arm facts only during validated replay."
  [state actor command]
  (let [old (get-in state [:requests (:request/id command)])
        create? (= :studio/session-create (:command/type command))]
    (cond
      (not (and (law/valid? :calliope.studio/actor-v1 actor) (= :user (:actor/kind actor))
                 (or (nil? (:session/owner state)) (= actor (:session/owner state)))))
      (refuse :command/not-authorized "The configured session owner must be a local user")
      (not (law/valid? :calliope.session/command-v1 command))
      (refuse :command/invalid "Malformed version1 session command")
      old (if (= [actor command] [(:actor old) (:command old)])
            {:ok? true :reused old}
            (refuse :request/conflict "This request identity already describes different intent"))
      (and create? (:session/id state)) (refuse :session/already-exists "This store already has a session")
      (and (not create?) (nil? (:session/id state))) (refuse :session/not-found "Create a session first")
      (or (and (:session/id state) (not= (:session/id state) (:session/id command)))
          (not= (:session/revision state) (:session/revision command)))
      (refuse :session/conflict "Session identity or revision requires an explicit reload")
      (and (= :studio/ingest (:command/type command)) (not (true? (:capture/armed? state))))
      (refuse :capture/not-armed "Explicit capture arming in this process is required")
      :else
      (case (:command/type command)
        :studio/session-create
        {:ok? true :effect :create
         :value (assoc (select-keys command [:session/id :session/title :work/id :session/links])
                       :value/type :session-created)}
        :studio/arm
        {:ok? true :effect :arm
         :value (assoc (select-keys command [:capture/clock :capture/origin]) :value/type :capture-armed)}
        :studio/stop {:ok? true :effect :stop :value {:value/type :capture-stopped}}
        :studio/ingest {:ok? true :effect :ingest}))))

(defn sources
  "Canonical source inventory follows admitted takes, never staging filenames."
  [state]
  (into {}
        (mapcat
         (fn [take]
           (let [audio (:take/audio take) midi (:take/midi take)]
             [[(:render/id audio) {:source/id (:render/id audio) :source/kind :audio
                                  :source/sha256 (:sha256 audio) :source/metadata audio}]
              [(:source/id midi) {:source/id (:source/id midi) :source/kind :midi
                                  :source/sha256 (:sha256 midi) :source/metadata midi}]]))
         (vals (:takes state)))))

(defn take-value
  "Bind a verified ingress take to this session/configuration and exact request
   hashes. It cannot overwrite an existing take or another source's identity."
  [state command take]
  (let [old-sources (sources state)
        refs [[(get-in take [:take/audio :render/id]) :audio (get-in take [:take/audio :sha256])]
              [(get-in take [:take/midi :source/id]) :midi (get-in take [:take/midi :sha256])]]]
    (cond
      (not (law/valid? :calliope.capture/take-v1 take)) (refuse :capture/invalid-take "Take metadata is invalid")
      (not= (:session/id state) (:take/session-id take)) (refuse :capture/invalid-take "Take belongs to another session")
      (not= [(:capture/clock state) (:capture/origin state)] [(:take/clock take) (:take/origin take)])
      (refuse :capture/clock-mismatch "Take configuration differs from the explicit arm")
      (not= [(get-in command [:audio :sha256]) (get-in command [:midi :sha256])]
            [(get-in take [:take/audio :sha256]) (get-in take [:take/midi :sha256])])
      (refuse :capture/hash-mismatch "Take hashes differ from the requested sources")
      (contains? (:takes state) (:take/id take)) (refuse :session/duplicate-take "Take identity is already admitted")
      (= (ffirst refs) (first (second refs))) (refuse :source/id-conflict "Audio and MIDI source identities must remain distinct")
      (some (fn [[id kind hash]]
              (when-let [old (get old-sources id)]
                (not= [kind hash] [(:source/kind old) (:source/sha256 old)]))) refs)
      (refuse :source/id-conflict "Source identity already refers to different media")
      :else {:ok? true :value {:value/type :take-admitted :take take}})))

(defn- corrupt! [message]
  (throw (ex-info message {:error/code :session/corrupt-history})))

(defn apply-record
  "Apply application semantics after Clio validates event identity/causality.
   Request uniqueness, result binding and capture history are Calliope laws."
  [state event]
  (let [{:record/keys [actor command value] :as record} (:event/data event)
        historical (assoc state :capture/armed? (= :armed (:capture/last-state state)))
        admitted (plan historical actor command)
        expected (if (= :ingest (:effect admitted))
                   (take-value state command (:take value)) admitted)]
    (when-not (and (= law/event-type (:event/type event))
                   (law/valid? :calliope.session/record-v1 record)
                   (:ok? admitted) (nil? (:reused admitted)) (:ok? expected)
                   (= value (:value expected))
                   (= (:event/stream event) (stream-id (:session/id command)))
                   (= (:event/subject event) (subject-id (:session/id command)))
                   (= (:event/actor event) (:actor/id actor))
                   (= (:event/seq event) (inc (:session/revision state))))
      (corrupt! "Session history violates command/result/owner/revision semantics"))
    (let [next-state
          (case (:effect admitted)
            :create (merge state (dissoc value :value/type)
                           {:session/owner actor :session/created-at (:event/at event)
                            :capture/last-state :unarmed})
            :arm (merge state (select-keys value [:capture/clock :capture/origin]) {:capture/last-state :armed})
            :stop (assoc state :capture/last-state :stopped)
            :ingest (assoc-in state [:takes (get-in value [:take :take/id])] (:take value)))]
      (-> next-state
          (assoc :session/revision (:event/seq event))
          (update :events conj event)
          (assoc-in [:requests (:request/id command)]
                    {:actor actor :command command :event/id (:event/id event) :value value})))))

(defn replay [events]
  (reduce apply-record empty-state events))
