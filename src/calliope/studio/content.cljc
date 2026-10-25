(ns calliope.studio.content
  "Pure content admission over named structural schemas.
   Context maps are caller-supplied immutable catalog snapshots; no I/O occurs."
  (:require [calliope.law.studio :as law]
            [calliope.media.manifest :as manifest]))

(defn- shape? [type value]
  (and (contains? law/registry type) ((law/validator type) value)))

(defn- finite-number? [value]
  (and (number? value) (< ##-Inf value ##Inf)))

(defn- ordered-range? [{:keys [start-ms end-ms]}]
  (< start-ms end-ms))

(defn- distinct-positions? [entries]
  (let [positions (map :entry/position entries)]
    (= (count positions) (count (set positions)))))

(defn- portable-value? [value]
  (cond
    (or (nil? value) (string? value) (keyword? value) (boolean? value)) true
    (number? value) (finite-number? value)
    (map? value) (every? portable-value? (mapcat identity value))
    (or (vector? value) (set? value)) (every? portable-value? value)
    :else false))

(defn- query-values-valid? [[op & args]]
  (case op
    (:and :or) (every? query-values-valid? args)
    :not (query-values-valid? (first args))
    (:gte :lte) (finite-number? (second args))
    (let [value (second args)]
      (or (not (number? value)) (finite-number? value)))))

(def ^:private timestamp-keys
  {:calliope.studio/clip-v1 :clip/created-at
   :calliope.studio/marker-v1 :marker/created-at
   :calliope.studio/arrangement-v1 :arrangement/created-at
   :calliope.studio/export-v1 :export/created-at
   :calliope.studio/rating-v1 :rating/at
   :calliope.studio/playlist-v1 :playlist/created-at
   :calliope.studio/smart-list-v1 :smart-list/created-at
   :calliope.studio/workspace-v1 :workspace/created-at})

(defn valid?
  "Full intrinsic admission: structural shape plus cross-field/date rules.
   Use the check functions below when referenced-object context is required."
  [type value]
  (boolean
   (and
    (shape? type value)
    (if-let [key (get timestamp-keys type)] (manifest/timestamp? (get value key)) true)
    (case type
      :calliope.studio/instant-v1 (manifest/timestamp? value)
      :calliope.studio/number-v1 (finite-number? value)
      :calliope.studio/query-value-v1
      (or (not (number? value)) (finite-number? value))
      :calliope.studio/encoding-v1 (portable-value? (:encoding/settings value))
      :calliope.studio/time-range-v1 (ordered-range? value)
      :calliope.studio/clip-v1
      (let [{:keys [start-ms end-ms]} (:clip/range value)
            {:keys [in-ms out-ms]} (:clip/fades value)]
        (and (ordered-range? (:clip/range value))
             (<= (or in-ms 0) (- end-ms start-ms))
             (<= (or out-ms 0) (- end-ms start-ms (or in-ms 0)))
             (if (contains? value :clip/gain-db)
               (finite-number? (:clip/gain-db value)) true)))
      :calliope.studio/marker-v1
      (and (not= (contains? value :marker/at-ms) (contains? value :marker/range))
           (if-let [range (:marker/range value)] (ordered-range? range) true)
           (or (not= :accepted (:marker/epistemic value))
               (= :user (get-in value [:marker/created-by :actor/kind]))))
      :calliope.studio/arrangement-v1 (distinct-positions? (:arrangement/entries value))
      :calliope.studio/export-v1
      (and (= (count (:export/source-hashes value))
              (count (set (:export/source-hashes value))))
           (portable-value? (get-in value [:export/encoding :encoding/settings])))
      :calliope.studio/playlist-v1 (distinct-positions? (:playlist/entries value))
      :calliope.studio/where-v1 (query-values-valid? value)
      :calliope.studio/query-v1 (query-values-valid? (:where value))
      :calliope.studio/smart-list-v1 (query-values-valid? (get-in value [:smart-list/query :where]))
      :calliope.studio/workspace-v1
      (if-let [where (:workspace/filters value)] (query-values-valid? where) true)
      true))))

(defn- refuse [code]
  {:ok? false :errors [{:error/code code}]})

(defn clip-check
  "Admit a non-destructive clip against its exact immutable render metadata.
   Does not read media bytes; a storage adapter must verify those separately."
  [clip render]
  (cond
    (not (valid? :calliope.studio/clip-v1 clip)) (refuse :invalid-clip)
    (not (valid? :calliope.studio/render-v1 render)) (refuse :invalid-render)
    (not= (get-in clip [:clip/source :source/id]) (:render/id render)) (refuse :source-id-mismatch)
    (not= (get-in clip [:clip/source :source/sha256]) (:render/sha256 render)) (refuse :source-hash-mismatch)
    (> (get-in clip [:clip/range :end-ms]) (:render/duration-ms render)) (refuse :range-outside-source)
    :else {:ok? true :value clip}))

(defn marker-check
  "Admit a marker against a render, or an arrangement plus its resolved duration.
   Arrangement timing resolution belongs to the later playback/read-model adapter."
  ([marker subject]
   (marker-check marker subject (:render/duration-ms subject)))
  ([marker subject duration-ms]
   (let [type (get-in marker [:marker/subject :subject/type])
         subject-id (case type :render (:render/id subject) :arrangement (:arrangement/id subject) nil)
         subject-type (case type :render :calliope.studio/render-v1
                                :arrangement :calliope.studio/arrangement-v1 nil)]
     (cond
       (not (valid? :calliope.studio/marker-v1 marker)) (refuse :invalid-marker)
       (not (valid? subject-type subject)) (refuse :invalid-subject)
       (not= (get-in marker [:marker/subject :subject/id]) subject-id) (refuse :subject-id-mismatch)
       (not (and (integer? duration-ms) (pos? duration-ms))) (refuse :missing-subject-duration)
       (and (= type :render) (not= duration-ms (:render/duration-ms subject))) (refuse :duration-mismatch)
       (> (or (:marker/at-ms marker) (get-in marker [:marker/range :end-ms])) duration-ms)
       (refuse :marker-outside-subject)
       :else {:ok? true :value marker}))))

(defn rating-for?
  "A scoped rating applies only to the identical subject type AND identity."
  [rating subject-ref]
  (boolean (and (valid? :calliope.studio/rating-v1 rating)
                (valid? :calliope.studio/subject-ref-v1 subject-ref)
                (= (:rating/subject rating) subject-ref))))

(defn arrangement-check
  "Resolve accepted clips, preserve source/work identity and sort authored positions.
   Missing/misidentified references and forbidden cross-work combinations fail."
  [arrangement clips-by-id renders-by-id]
  (if-not (valid? :calliope.studio/arrangement-v1 arrangement)
    (refuse :invalid-arrangement)
    (let [entries (vec (sort-by :entry/position (:arrangement/entries arrangement)))
          clips (mapv #(get clips-by-id (:entry/clip-id %)) entries)
          renders (mapv #(get renders-by-id (get-in % [:clip/source :source/id])) clips)]
      (cond
        (not (every? true? (map #(= (:entry/clip-id %1) (:clip/id %2)) entries clips)))
        (refuse :missing-or-misidentified-clip)
        (not (every? #(= :accepted (:clip/status %)) clips)) (refuse :clip-not-accepted)
        (not (every? :ok? (map clip-check clips renders))) (refuse :invalid-clip-source)
        :else
        (let [work-ids (set (map :render/work-id renders))]
          (if (and (not (:arrangement/cross-work? arrangement)) (> (count work-ids) 1))
            (refuse :cross-work-not-authorized)
            {:ok? true :value arrangement :entries entries
             :work-ids work-ids :source-hashes (set (map :render/sha256 renders))}))))))

(defn export-check
  "Bind export metadata to the exact arrangement revision and complete source set.
   Rendering, file-byte verification and publication are separate adapters."
  [export arrangement clips-by-id renders-by-id]
  (let [resolved (arrangement-check arrangement clips-by-id renders-by-id)]
    (cond
      (not (valid? :calliope.studio/export-v1 export)) (refuse :invalid-export)
      (not (:ok? resolved)) (refuse :invalid-arrangement-context)
      (not= (:export/arrangement-id export) (:arrangement/id arrangement)) (refuse :arrangement-id-mismatch)
      (not= (:export/arrangement-version export) (:arrangement/version arrangement)) (refuse :arrangement-version-mismatch)
      (not= (set (:export/source-hashes export)) (:source-hashes resolved)) (refuse :incomplete-or-extra-source-hashes)
      :else {:ok? true :value export})))
