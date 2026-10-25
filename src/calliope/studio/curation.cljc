(ns calliope.studio.curation
  "GPL-3.0-or-later. Pure phrase curation; proposals never imply decisions."
  (:require [calliope.law.curation :as law]
            [calliope.studio.content :as content]))

(def initial {:markers {} :clips {} :selected/clip nil :annotations {} :proposals {}
              :decisions [] :accepted-labels {}})
(defn refuse [code message] {:ok? false :error/code code :error/message message})
(defn curation? [command] (law/valid? :calliope.curation/command-v1 command))
(defn ceil-div [n d] (+ (quot n d) (if (zero? (rem n d)) 0 1)))
(defn milliseconds [range sample-rate]
  {:start-ms (quot (* 1000 (:start-sample range)) sample-rate)
   :end-ms (ceil-div (* 1000 (:end-sample range)) sample-rate)})
(defn render [state take]
  (let [audio (:take/audio take)]
    {:render/id (:render/id audio) :render/work-id (:work/id state)
     :render/sha256 (:sha256 audio) :render/duration-ms (ceil-div (* 1000 (:frames audio)) (:sample-rate audio))
     :render/codec "wav" :render/source {:source/renderer "studio-capture"}}))
(defn range-valid? [range take]
  (and take (< (:start-sample range) (:end-sample range))
       (<= (:end-sample range) (get-in take [:take/audio :frames]))))
(defn same-clip? [state proposal]
  (= (:proposal/source proposal) (get-in state [:clips (:clip/id proposal)])))

(defn plan [state actor command]
  (let [type (:command/type command) take (get-in state [:takes (:take/id command)])
        clip (get-in state [:clips (:clip/id command)])
        proposal (get-in state [:proposals (:proposal/id command)])
        completion? (law/completion? command)]
    (cond
      (not (curation? command)) (refuse :command/invalid "Invalid curation command")
      (not= (if completion? :agent :user) (:actor/kind actor))
      (refuse :command/not-authorized "Only local user intent or bounded provider completion is admitted")
      (and (contains? #{:studio/mark :studio/select-clip} type) (not (range-valid? (:range command) take)))
      (refuse :clip/invalid-range "Range must be nonempty and inside its admitted take")
      (and (= :studio/mark type) (contains? (:markers state) (:marker/id command)))
      (refuse :marker/already-exists "Marker identity already exists")
      (and (= :studio/mark type) (not= :armed (:capture/last-state state)))
      (refuse :capture/not-armed "Mark a current phrase while capture is armed")
      (and (= :studio/select-clip type)
           (or (not= (:source/sha256 command) (get-in take [:take/audio :sha256]))
               (and clip (not= (:take/id clip) (:take/id command)))))
      (refuse :clip/source-mismatch "Selection must retain its original take and source hash")
      (and (contains? #{:studio/annotate :studio/request-proposal} type) (nil? clip))
      (refuse :clip/not-found "Select an admitted clip first")
      (and (= :studio/request-proposal type) proposal)
      (refuse :proposal/already-exists "Proposal identity already exists")
      (and (or completion? (= :studio/decide type)) (nil? proposal))
      (refuse :proposal/not-found "Proposal identity is unknown")
      (and (or completion? (= :studio/decide type)) (not (same-clip? state proposal)))
      (refuse :proposal/stale-clip "The clip changed after this proposal request")
      (and completion? (not= :requested (:proposal/status proposal)))
      (refuse :proposal/already-completed "This request already has a completion")
      (and (= :studio/decide type) (not= :provisional (:proposal/status proposal)))
      (refuse :proposal/not-provisional "Only an undecided provisional result can be decided")
      :else {:ok? true :effect :curate :value {:value/type :curation-applied :curation/command command}})))

(defn apply-command [state actor command event-id at]
  (let [type (:command/type command) take (get-in state [:takes (:take/id command)])
        clip-id (:clip/id command) proposal-id (:proposal/id command)
        clip (get-in state [:clips clip-id]) proposal (get-in state [:proposals proposal-id])]
    (case type
      :studio/mark
      (let [marker {:marker/id (:marker/id command)
                    :marker/subject {:subject/type :render :subject/id (get-in take [:take/audio :render/id])}
                    :marker/kind :phrase :marker/range (milliseconds (:range command) (get-in take [:take/audio :sample-rate]))
                    :marker/epistemic :provisional :marker/created-by actor :marker/created-at at}]
        (when-not (:ok? (content/marker-check marker (render state take)))
          (throw (ex-info "Invalid marker projection" {:error/code :session/corrupt-history})))
        (assoc-in state [:markers (:marker/id command)]
                  {:marker marker :take/id (:take/id command) :range (:range command) :source/event event-id}))
      :studio/select-clip
      (let [selected {:clip/id clip-id :clip/source {:source/type :render :source/id (get-in take [:take/audio :render/id])
                                                    :source/sha256 (:source/sha256 command)}
                      :clip/range (milliseconds (:range command) (get-in take [:take/audio :sample-rate]))
                      :clip/title (:clip/title command) :clip/status :provisional
                      :clip/created-by actor :clip/created-at at}]
        (when-not (:ok? (content/clip-check selected (render state take)))
          (throw (ex-info "Invalid clip projection" {:error/code :session/corrupt-history})))
        (-> state
            (assoc :selected/clip clip-id)
            (assoc-in [:clips clip-id]
                      {:clip selected :take/id (:take/id command) :range (:range command)
                       :sample-rate (get-in take [:take/audio :sample-rate])
                       :source/hashes [(get-in take [:take/audio :sha256]) (get-in take [:take/midi :sha256])]
                       :source/event event-id})))
      :studio/annotate
      (update-in state [:annotations clip-id] (fnil conj [])
                 {:text (:annotation/text command) :labels (:annotation/labels command)
                  :actor actor :at at :event/id event-id})
      :studio/request-proposal
      (assoc-in state [:proposals proposal-id]
                {:proposal/id proposal-id :clip/id clip-id :proposal/prompt (:proposal/prompt command)
                 :proposal/status :requested :proposal/source clip :proposal/request-event event-id})
      :studio/proposal-result
      (update-in state [:proposals proposal-id] assoc :proposal/status :provisional
                 :proposal/output (:proposal/output command) :proposal/result-event event-id)
      :studio/proposal-failed
      (update-in state [:proposals proposal-id] assoc :proposal/status :failed
                 :failure/code (:failure/code command) :failure/message (:failure/message command)
                 :proposal/result-event event-id)
      :studio/decide
      (cond-> (-> state
                  (update :decisions (fnil conj [])
                          {:proposal/id proposal-id :decision/value (:decision/value command)
                           :decision/actor actor :decision/at at :decision/event event-id
                           :decision/source-event (:proposal/result-event proposal)
                           :clip/id (:clip/id proposal) :clip/source (:proposal/source proposal)})
                  (assoc-in [:proposals proposal-id :proposal/status] :decided))
        (= :accept (:decision/value command))
        (update-in [:accepted-labels (:clip/id proposal)]
                   #(vec (sort (set (concat % (get-in proposal [:proposal/output :labels]))))))))))
