(ns calliope.companion.review
  "Independent, repeated human observations scoped to one renderer clip."
  (:require [calliope.suno.metadata :as metadata]
            [clojure.string :as str]))

(def dispositions #{:unreviewed :keep :salvage :reject})
(def rating-dimensions #{:enjoyment :publishability :weirdness :salvageability :technical-quality})
(def editable-fields #{:note :labels :projects :disposition :ratings :span :marker-input})
(def empty-history {:latest {} :history {} :heads {} :nodes {} :roots {} :redo-preference {} :event-ids #{}})
(defn empty-review [id]
  {:clip/id id :note "" :labels [] :projects [] :disposition :unreviewed :ratings {}})
(defn terms [text]
  (->> (str/split (or text "") #",") (map str/trim) (remove str/blank?) distinct vec))

(defn validate [review duration]
  (when-not (metadata/clip-id? (:clip/id review))
    (throw (ex-info "Review requires a Suno clip UUID" {})))
  (when-not (contains? dispositions (:disposition review))
    (throw (ex-info "Unknown disposition" {})))
  (when-not (and (string? (:note review))
                 (every? #(and (vector? %) (every? string? %))
                         [(:labels review) (:projects review)]))
    (throw (ex-info "Review note, labels, and projects have invalid types" {})))
  (when-not (and (map? (:ratings review))
                 (every? (fn [[dimension rating]]
                           (and (contains? rating-dimensions dimension)
                                (integer? rating) (<= 0 rating 5))) (:ratings review)))
    (throw (ex-info "Ratings require known dimensions and integer scores 0–5; omit an unrated dimension" {})))
  (when (contains? review :marker-input)
    (when-not (and (map? (:marker-input review))
                   (= #{:start :end} (set (keys (:marker-input review))))
                   (every? string? (vals (:marker-input review))))
      (throw (ex-info "Marker input requires start and end text" {}))))
  (when (and (contains? review :span) (not (map? (:span review))))
    (throw (ex-info "Marker span must be a map" {})))
  (when-let [{:keys [start end]} (:span review)]
    (when-not (and (metadata/finite-number? start) (<= 0 start)
                   (or (nil? end) (and (metadata/finite-number? end) (< start end)))
                   (or (nil? duration) (<= (or end start) duration)))
      (throw (ex-info "Marker needs a nonnegative start, an optional later end, and must fit the known duration" {}))))
  review)

(defn record-at [state id revision]
  (if (nil? revision) (empty-review id)
    (let [node (get-in state [:nodes revision])]
      (when-not (= id (:clip/id node))
        (throw (ex-info "History revision is missing or belongs to another clip" {:clip/id id :revision revision})))
      (:record node))))

(defn patch-between [before after]
  (into {} (for [field editable-fields :when (not= (get before field) (get after field))]
             [field (get after field)])))

(defn undo-target [state id] (get-in state [:nodes (get-in state [:heads id]) :parent]))
(defn can-undo? [state id] (some? (get-in state [:heads id])))
(defn children-at [state id revision]
  (if revision (get-in state [:nodes revision :children] []) (get-in state [:roots id] [])))
(defn redo-target [state id]
  (let [head (get-in state [:heads id]) children (children-at state id head)
        preferred (get-in state [:redo-preference id head])]
    (if (some #{preferred} children) preferred (last children))))

(defn assert-head! [state id expected]
  (when-not (= expected (get-in state [:heads id]))
    (throw (ex-info "History changed in another writer; reload before retrying. Your draft is retained."
                    {:clip/id id :expected expected :actual (get-in state [:heads id])}))))

(defn apply-event
  "Replay revisions and cursor moves. Every branch remains addressable; old v1 snapshots are revisions too."
  [state event]
  (let [state (merge empty-history state) type (:event/type event)
        listening? (contains? #{:listening/review-recorded :listening/history-moved} type)
        revision (:event/id event)
        _ (when (and listening? (or (not (string? revision)) (str/blank? revision)
                                   (contains? (:event-ids state) revision)))
            (throw (ex-info "Invalid or duplicate listening event ID" {:event/id revision})))
        state (cond-> state listening? (update :event-ids conj revision))]
    (case type
      :listening/review-recorded
      (let [legacy? (= :calliope.listening/review-v1 (:schema event))
            id (if legacy? (get-in event [:review :clip/id]) (:clip/id event))
            revision (:event/id event)
            parent (if legacy? (get-in state [:heads id]) (:review/parent event))]
        (when-not (and (string? revision) (not (str/blank? revision))
                       (not (get-in state [:nodes revision]))
                       (or legacy? (and (= :calliope.listening/review-v2 (:schema event))
                                        (contains? event :review/parent))))
          (throw (ex-info "Invalid or duplicate listening revision" {:event/id revision})))
        (assert-head! state id parent)
        (let [base (record-at state id parent) patch (:review/patch event)
              record (if legacy? (:review event)
                       (do
                         (when-not (and (map? patch) (seq patch) (every? editable-fields (keys patch)))
                           (throw (ex-info "Review patch requires changed editable fields" {:event/id revision})))
                         (reduce-kv (fn [r k v] (if (nil? v) (dissoc r k) (assoc r k v))) base patch)))]
          (validate record nil)
          (-> state
              (assoc-in [:nodes revision] {:id revision :clip/id id :parent parent :record record
                                          :children [] :ts (:ts event) :event event})
              (update-in (if parent [:nodes parent :children] [:roots id]) (fnil conj []) revision)
              (assoc-in [:heads id] revision) (assoc-in [:latest id] record)
              (assoc-in [:redo-preference id parent] revision)
              (update-in [:history id] (fnil conj []) event))))

      :listening/history-moved
      (let [id (:clip/id event) from (:history/from event) to (:history/to event)
            action (:history/action event)]
        (when-not (and (= :calliope.listening/history-v1 (:schema event))
                       (metadata/clip-id? id) (contains? event :history/from)
                       (contains? event :history/to))
          (throw (ex-info "History move requires a clip and explicit endpoints" {})))
        (assert-head! state id from)
        (let [record (record-at state id to)]
          (when-not (and (not= from to)
                         (case action
                           :undo (and (can-undo? state id) (= to (undo-target state id)))
                           :redo (boolean (some #{to} (children-at state id from)))
                           :checkout true
                           false))
            (throw (ex-info "Invalid history move" {:action action :from from :to to})))
          (cond-> (-> state (assoc-in [:heads id] to)
                      (update-in [:history id] (fnil conj []) event))
            to (assoc-in [:latest id] record)
            (nil? to) (update :latest dissoc id)
            (= action :undo) (assoc-in [:redo-preference id to] from)
            (= action :redo) (assoc-in [:redo-preference id from] to))))
      state)))

(defn replay [events] (reduce apply-event empty-history events))

(defn timestamp [text]
  (when-not (str/blank? text)
    (let [value (str/trim text) parts (str/split value #":")]
      (when-not (and (re-matches #"\d+(?:\.\d+)?(?::\d+(?:\.\d+)?){0,2}" value)
                     (<= 1 (count parts) 3)
                     (every? #(boolean (re-matches #"\d+(?:\.\d+)?" %)) parts)
                     (every? #(< #?(:clj (Double/parseDouble %) :cljs (js/parseFloat %)) 60)
                             (if (= 1 (count parts)) [] (rest parts))))
        (throw (ex-info "Use seconds, m:ss, or h:mm:ss" {:value text})))
      (reduce (fn [n part] (+ (* 60 n) #?(:clj (Double/parseDouble part) :cljs (js/parseFloat part))))
              0 parts))))
