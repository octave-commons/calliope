#!/usr/bin/env bb
;; fork_tales_v2 pass 4 — Suno renderer metadata ingestion.
;;
;; Passes 1-3 (scripts/corpus.clj) ingest lyric *text* and copy track assets.
;; The renderer JSON that ships beside newer downloads (full Suno API clip
;; objects: isLiked, style tags, generation prompt, model badges, control
;; sliders) has never been parsed. This pass observes it.
;;
;; Epistemic tier: everything emitted here is :observed renderer metadata.
;; Suno is a media/evidence source, not an ontology authority (AGENTS.md).
;; Likes, tags, and prompts describe what the renderer recorded, never what
;; a song *is*. Similarity is a signal, never a merge.
;;
;; Usage:
;;   bb scripts/suno_meta.clj ingest [--root DIR]  observe metadata, append events, rebuild projection
;;   bb scripts/suno_meta.clj project              rebuild projection from the ledger only
;;   bb scripts/suno_meta.clj stats                summarize the projection
;;   bb scripts/suno_meta.clj liked                list liked clips
;;   bb scripts/suno_meta.clj tags                 tag vocabulary with clip counts
;;   bb scripts/suno_meta.clj show TERM            full records for clips matching TERM (id or title)
;;
;; Default root: /home/err/Downloads/Suno Downloads

(require '[babashka.fs :as fs]
         '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])
(import '[java.math BigInteger]
        '[java.security MessageDigest]
        '[java.time Instant]
        '[java.util UUID])

(def repo-root
  (-> (fs/parent (fs/parent (fs/absolutize *file*))) str))

(def ledger-path (str (fs/path repo-root "ledgers" "ingest.edn")))
(def projection-path (str (fs/path repo-root "ledgers" "projections" "suno-meta-v1.edn")))
(def default-root "/home/err/Downloads/Suno Downloads")

;; ---------------------------------------------------------------- utils

(defn now-iso [] (str (Instant/now)))
(defn uuid [] (str (UUID/randomUUID)))

(defn sha8-hex ^String [^bytes bs]
  (let [md (.getInstance MessageDigest "SHA-256")]
    (.update md bs)
    (subs (format "%064x" (BigInteger. 1 (.digest md))) 0 8)))

(defn append-event! [event]
  (spit ledger-path (str (pr-str event) "\n") :append true))

(defn read-ledger-events []
  (when (fs/exists? ledger-path)
    (keep (fn [line]
            (when-not (str/blank? line)
              (try (edn/read-string line)
                   (catch Exception _ nil))))
          (str/split-lines (slurp ledger-path)))))

(defn suno-clip-events []
  (filter #(= :suno/clip-observed (:event/type %)) (read-ledger-events)))

;; ------------------------------------------------------------ name parsing
;; The extension writes "<title> (1).mp3" (space before paren) for media but
;; "<title>(1).json" (no space) for metadata. Normalize both: strip a trailing
;; "(N)" with optional preceding space -> variation N; bare name -> variation 0.

(def variation-re #"(?i)\s*\((\d+)\)$")

(defn split-variation [^String stem]
  (if-let [[_ n] (re-find variation-re stem)]
    {:base (str/trim (str/replace stem variation-re ""))
     :variation (parse-long n)}
    {:base stem :variation 0}))

(defn file-stem [path]
  (first (str/split (str (fs/file-name path)) #"\.(?=[^.]+$)")))

;; ------------------------------------------------------------ txt fallback
;; Older downloads ship only "<title>.txt":
;;   Title: <title>
;;   ID: <uuid>
;;   Tags: <style string>     (newer txt files only)
;;   Prompt:
;;   <lyrics, possibly empty>

(defn parse-txt [^String text]
  (let [norm (str/replace text #"\r\n?" "\n")
        title (some-> (re-find #"(?m)^Title:\s*(.+)$" norm) second str/trim)
        id    (some-> (re-find #"(?m)^ID:\s*(\S+)" norm) second)
        tags  (some-> (re-find #"(?m)^Tags:\s*(.+)$" norm) second str/trim)
        prompt (some-> (str/split norm #"(?m)^Prompt:\s*$" 2) second str/trim)]
    {:title title :id id :tags tags :prompt prompt}))

;; ------------------------------------------------------------ tag tokens
;; Renderer tags are a free-form style string: "Glitch-choir lullaby, 78 BPM.
;; Soft sub-bass, ...". Tokenize on commas/semicolons, lowercase, keep raw.
;; Structured interpretation (BPM, genre, texture) belongs to classifier
;; programs, not to this observed layer.

(defn tag-tokens [^String tags-raw]
  (if (str/blank? tags-raw)
    []
    (->> (str/split tags-raw #"[,;\n]+")
         (map str/trim)
         (remove str/blank?)
         (map str/lower-case)
         vec)))

;; ------------------------------------------------------------ clip records

(defn json->clip [json-path folder]
  (let [raw (slurp json-path)
        m (json/parse-string raw true)
        meta (:metadata m)
        {:keys [variation]} (split-variation (file-stem json-path))
        model (or (get-in m [:metadata :model_badges :songcard :display_name])
                  (get-in m [:metadata :model_badges :songrow :display_name]))]
    {:clip/id (or (:id m) (str "sha8:" (sha8-hex (.getBytes raw))))
     :clip/title (:title m)
     :clip/variation variation
     :clip/liked (boolean (:isLiked m))
     :clip/tags-raw (or (:tags meta) "")
     :clip/tag-tokens (tag-tokens (:tags meta))
     :clip/prompt (or (:prompt meta) "")
     :clip/duration (:duration meta)
     :clip/model model
     :clip/type (or (:type meta) "gen")
     :clip/control-sliders (:control_sliders meta)
     :clip/negative-tags (:negative_tags meta)
     :clip/cover? (boolean (:isCover m))
     :clip/remaster? (boolean (:isRemaster m))
     :clip/folder folder
     :clip/json-sha8 (sha8-hex (.getBytes raw))
     :clip/source :json}))

(defn txt->clip [txt-path folder]
  (let [raw (slurp txt-path)
        {:keys [title id tags prompt]} (parse-txt raw)
        {:keys [variation]} (split-variation (file-stem txt-path))]
    {:clip/id (or id (str "sha8:" (sha8-hex (.getBytes raw))))
     :clip/title title
     :clip/variation variation
     :clip/liked nil                      ; not observable from txt
     :clip/tags-raw (or tags "")
     :clip/tag-tokens (tag-tokens tags)
     :clip/prompt (or prompt "")
     :clip/duration nil
     :clip/model nil
     :clip/type "txt-only"
     :clip/control-sliders nil
     :clip/negative-tags nil
     :clip/cover? nil
     :clip/remaster? nil
     :clip/folder folder
     :clip/json-sha8 (sha8-hex (.getBytes raw))
     :clip/source :txt}))

(defn- by-ext [files ext]
  (filter #(str/ends-with? % ext) files))

(defn- assets-for [files variation]
  ;; media files carry " (N)" with a space; variation 0 is the bare stem
  (let [want (fn [ext]
               (first (filter (fn [f]
                                (and (str/ends-with? f ext)
                                     (= variation (:variation (split-variation (file-stem f))))))
                              files)))]
    (cond-> {}
      (want ".mp3")  (assoc :mp3 (want ".mp3"))
      (want ".jpeg") (assoc :jpeg (want ".jpeg")))))

(defn scan-folder [dir]
  (let [folder (str (fs/file-name dir))
        files (->> (fs/list-dir dir) (filter fs/regular-file?) (map str))
        json-clips (map #(json->clip % folder) (by-ext files ".json"))
        json-ids (set (map :clip/id json-clips))
        txt-clips (->> (by-ext files ".txt")
                       (map #(txt->clip % folder))
                       ;; txt mirrors json content in json-era folders; keep
                       ;; only clips the json generation did not observe
                       (remove #(contains? json-ids (:clip/id %)))
                       ;; orphan duplicate txts ("(1).txt" vs " (1).txt")
                       (group-by :clip/id)
                       (map (comp first val)))
        attach (fn [clip]
                 (assoc clip :clip/assets
                        (assets-for files (:clip/variation clip))))]
    (map attach (concat json-clips txt-clips))))

(defn scan-root [root]
  (->> (fs/list-dir root)
       (filter fs/directory?)
       (map str)
       sort
       (mapcat scan-folder)))

;; ------------------------------------------------------------ idempotence
;; Re-runs append only *changed* observations: a clip already observed with
;; the same content hash produces no new event. The ledger stays append-only;
;; the projection always reflects the latest observation per clip.

(defn known-observations []
  (into #{}
        (map (fn [e] [(get-in e [:clip :clip/id])
                      (get-in e [:clip :clip/json-sha8])]))
        (suno-clip-events)))

;; ------------------------------------------------------------ projection

(defn build-projection [events]
  (let [latest (->> events
                    (sort-by :ts)
                    (reduce (fn [acc e] (assoc acc (get-in e [:clip :clip/id]) (:clip e))) {}))
        clips (into (sorted-map) latest)
        ids (keys clips)
        liked (into [] (comp (filter :clip/liked) (map :clip/id)) (vals clips))
        by-tag (reduce (fn [acc clip]
                         (reduce (fn [a t] (update a t (fnil conj []) (:clip/id clip)))
                                 acc (:clip/tag-tokens clip)))
                       (sorted-map) (vals clips))
        by-model (reduce (fn [acc clip]
                           (if-let [m (:clip/model clip)]
                             (update acc m (fnil conj []) (:clip/id clip))
                             acc))
                         (sorted-map) (vals clips))
        by-source (reduce (fn [acc clip]
                            (update acc (:clip/source clip) (fnil conj []) (:clip/id clip)))
                          {} (vals clips))]
    {:projection/version 1
     :generated-at (now-iso)
     :clip-count (count ids)
     :clips clips
     :index {:liked liked
             :tags by-tag
             :models by-model
             :sources by-source}}))

(defn write-projection! [events]
  (let [proj (build-projection events)]
    (fs/create-dirs (fs/parent projection-path))
    (spit projection-path (with-out-str (pprint/pprint proj)))
    proj))

(defn load-projection []
  (when (fs/exists? projection-path)
    (edn/read-string (slurp projection-path))))

;; ------------------------------------------------------------ commands

(defn ingest! [root]
  (let [root (str (fs/absolutize root))]
    (when-not (fs/directory? root)
      (println "ERROR: root is not a directory:" root)
      (System/exit 1))
    (let [run-id (uuid)
          known (known-observations)
          clips (scan-root root)
          fresh (remove #(contains? known [(:clip/id %) (:clip/json-sha8 %)]) clips)]
      (append-event! {:event/id (uuid) :event/type :suno-meta/run-started
                      :run/id run-id :ts (now-iso)
                      :root root :clips-scanned (count clips)})
      (doseq [clip fresh]
        (append-event! {:event/id (uuid) :event/type :suno/clip-observed
                        :run/id run-id :ts (now-iso)
                        :epistemic-tier :observed
                        :clip clip}))
      (let [proj (write-projection! (suno-clip-events))]
        (append-event! {:event/id (uuid) :event/type :suno-meta/run-completed
                        :run/id run-id :ts (now-iso)
                        :clips-scanned (count clips)
                        :clips-appended (count fresh)
                        :projection-clip-count (:clip-count proj)})
        (println "Scanned:" (count clips) "clips from" root)
        (println "Appended:" (count fresh) "new observations"
                 (str "(" (- (count clips) (count fresh)) " unchanged)"))
        (println "Projection:" projection-path
                 (str "(" (:clip-count proj) " clips, "
                      (count (get-in proj [:index :liked])) " liked, "
                      (count (get-in proj [:index :tags])) " tag tokens)"))))))

(defn project! []
  (let [proj (write-projection! (suno-clip-events))]
    (println "Projection rebuilt:" projection-path
             (str "(" (:clip-count proj) " clips)"))))

(defn- require-projection []
  (or (load-projection)
      (do (println "ERROR: projection missing — run `bb scripts/suno_meta.clj ingest` first.")
          (System/exit 1))))

(defn stats! []
  (let [proj (require-projection)
        clips (vals (:clips proj))
        liked (get-in proj [:index :liked])
        tags (get-in proj [:index :tags])
        models (get-in proj [:index :models])
        sources (get-in proj [:index :sources])
        durations (keep :clip/duration clips)]
    (println "Suno renderer metadata — projection v" (:projection/version proj)
             "generated" (:generated-at proj))
    (println (str "  clips:        " (:clip-count proj)))
    (println (str "  liked:        " (count liked)
                  (when (seq clips)
                    (format " (%.1f%% of json-observed)"
                            (* 100.0 (/ (count liked)
                                        (max 1 (count (:json sources)))))))))
    (println (str "  by source:    " (into {} (map (fn [[k v]] [k (count v)]) sources))))
    (println (str "  models:       " (into {} (map (fn [[k v]] [k (count v)]) models))))
    (when (seq durations)
      (println (format "  duration:     %.0fs total, %.0fs mean"
                       (reduce + durations)
                       (/ (reduce + durations) (count durations)))))
    (println (str "  tag tokens:   " (count tags)))
    (println "  top tags:")
    (doseq [[t ids] (->> tags (sort-by (comp count second) >) (take 25))]
      (println (format "    %4d  %s" (count ids) t)))))

(defn liked! []
  (let [proj (require-projection)
        clips (:clips proj)]
    (doseq [id (get-in proj [:index :liked])
            :let [c (get clips id)]]
      (println (str (:clip/title c)
                    (when (pos? (:clip/variation c)) (str " (" (:clip/variation c) ")"))
                    "  —  " (:clip/folder c)
                    (when-not (str/blank? (:clip/tags-raw c))
                      (str "\n    tags: " (:clip/tags-raw c))))))
    (println (str "(" (count (get-in proj [:index :liked])) " liked clips)"))))

(defn tags! []
  (let [proj (require-projection)]
    (doseq [[t ids] (->> (get-in proj [:index :tags])
                         (sort-by (comp count second) >))]
      (println (format "%4d  %s" (count ids) t)))))

(defn show! [term]
  (let [proj (require-projection)
        needle (str/lower-case term)
        hits (filter (fn [[id c]]
                       (or (= id term)
                           (str/includes? (str/lower-case (or (:clip/title c) "")) needle)))
                     (:clips proj))]
    (if (empty? hits)
      (println "No clips match" (pr-str term))
      (doseq [[_ c] hits]
        (pprint/pprint c)
        (println "---")))))

;; ------------------------------------------------------------ entry

(let [args *command-line-args*
      cmd (first args)
      flag (fn [k] (second (drop-while #(not= k %) args)))]
  (case cmd
    "ingest" (ingest! (or (flag "--root") default-root))
    "project" (project!)
    "stats" (stats!)
    "liked" (liked!)
    "tags" (tags!)
    "show" (if-let [term (second args)]
             (show! term)
             (println "usage: bb scripts/suno_meta.clj show TERM"))
    (println "usage: bb scripts/suno_meta.clj [ingest [--root DIR]|project|stats|liked|tags|show TERM]")))
