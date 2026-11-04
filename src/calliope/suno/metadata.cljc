(ns calliope.suno.metadata
  "Portable Suno observations. Renderer settings describe intent, not heard sound."
  (:require [clojure.string :as str]))

(def schema :calliope.suno/clip-v1)
(def id-pattern #"(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

(defn clip-id? [x] (and (string? x) (boolean (re-matches id-pattern x))))
(defn finite-number? [x]
  (and (number? x) #?(:clj (Double/isFinite (double x)) :cljs (js/Number.isFinite x))))

(defn key-name [k] (if (keyword? k) (name k) (str k)))
(defn private-key? [k]
  (boolean (re-find #"(?i)(authorization|cookie|password|secret|token|api[_-]?key|email|phone|address)"
                    (key-name k))))

(def profile-keys #{"user_id" "display_name" "handle" "avatar_image_url"
                    "is_handle_updated" "is_following_creator"})

(defn safe-data
  "Keep unfamiliar renderer fields, removing account/credential fields recursively."
  ([x] (safe-data x true))
  ([x root?]
   (cond
     (map? x) (into (sorted-map)
                    (for [[k v] x
                          :when (not (or (private-key? k)
                                         (and root? (contains? profile-keys (key-name k)))))]
                      [(key-name k) (safe-data v false)]))
     (sequential? x) (mapv #(safe-data % false) x)
     :else x)))

(defn field [m k] (get m (name k)))
(defn normalize [input]
  (when-not (map? input) (throw (ex-info "Clip must be a JSON object" {})))
  (let [raw (safe-data input)
        id (field raw :id)
        title (field raw :title)
        metadata (field raw :metadata)
        duration (field metadata :duration)]
    (when-not (clip-id? id) (throw (ex-info "Clip requires a Suno UUID" {:id id})))
    (when-not (string? title) (throw (ex-info "Clip requires a title string" {:id id})))
    (when-not (map? metadata) (throw (ex-info "Clip requires a metadata object" {:id id})))
    (doseq [k [:prompt :tags :negative_tags]
            :let [v (field metadata k)] :when (some? v)]
      (when-not (string? v) (throw (ex-info "Invalid metadata text" {:id id :field k}))))
    (when (and (some? duration) (not (and (finite-number? duration) (<= 0 duration))))
      (throw (ex-info "Invalid duration" {:id id})))
    (when (and (some? (field metadata :control_sliders))
               (not (map? (field metadata :control_sliders))))
      (throw (ex-info "Controls must be an object" {:id id})))
    {:clip/id (str/lower-case id)
     :clip/title title
     :clip/url (str "https://suno.com/song/" (str/lower-case id))
     :clip/lyrics (field metadata :prompt)
     :clip/styles (field metadata :tags)
     :clip/negative-styles (field metadata :negative_tags)
     :clip/duration duration
     :clip/controls (field metadata :control_sliders)
     :clip/model (or (field raw :model_name)
                     (get-in raw ["metadata" "model_badges" "songrow" "display_name"])
                     (get-in raw ["metadata" "model_badges" "songcard" "display_name"]))
     :clip/created-at (field raw :created_at)
     :clip/liked (cond (contains? raw "is_liked") (field raw :is_liked)
                       (contains? raw "isLiked") (field raw :isLiked))
     :clip/raw raw}))

(defn payload-clips [payload]
  (cond
    (sequential? payload) payload
    (and (map? payload) (clip-id? (or (:id payload) (get payload "id")))) [payload]
    (map? payload) (let [clips (or (:clips payload) (get payload "clips"))]
                     (if (sequential? clips) clips
                         (throw (ex-info "Expected a clip, clip array, or clips envelope" {}))))
    :else (throw (ex-info "Unsupported metadata payload" {}))))

(defn parse-text [text]
  (let [text (str/replace text #"\r\n?" "\n")
        header (fn [k] (some-> (re-find (re-pattern (str "(?m)^" k ":[ \\t]*(.+)$")) text)
                              second str/trim))]
    {"id" (header "ID") "title" (header "Title")
     "metadata" {"tags" (header "Tags")
                  "prompt" (some-> (str/split text #"(?m)^Prompt:[ \t]*$" 2) second str/trim)}}))

(defn source-variation [filename]
  (some-> (re-find #"\((\d+)\)\.[^.]+$" filename) second
          #?(:clj Long/parseLong :cljs js/parseInt)))

(defn replay [events]
  {:projection/schema :calliope.suno/projection-v1
   :clips (reduce (fn [clips event]
                    (if (= :suno/metadata-observed (:event/type event))
                      (let [id (get-in event [:clip :clip/id])
                            previous (get clips id)
                            incoming (assoc (:clip event) :clip/source (:source event))]
                        ;; A legacy lyric TXT must not erase an observed JSON record.
                        (if (and (= :suno-json (get-in previous [:clip/source :source/format]))
                                 (= :suno-text (get-in event [:source :source/format])))
                          clips (assoc clips id incoming)))
                      clips))
                  (sorted-map) events)})

(defn search-text [clip review]
  (str/lower-case
   (str/join " " [(:clip/id clip) (:clip/title clip) (:clip/styles clip)
                   (:clip/lyrics clip) (:clip/model clip)
                   (str/join " " (:labels review)) (str/join " " (:projects review))
                   (:note review)])))
