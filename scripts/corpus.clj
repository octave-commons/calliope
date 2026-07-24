#!/usr/bin/env bb
;; fork_tales_v2 corpus pipeline — event-sourced lyric ingestion.
;;
;; Pass 1: exact-copy dedup. Every document discovered is hashed (raw sha256
;; plus a normalized body-sha256 that strips Suno ID lines and whitespace
;; drift) and appended as a :doc/discovered event to ledgers/ingest.edn.
;; The deduplicated songbook under docs/lyrics/ is a pure projection over
;; that ledger — never edit docs/lyrics by hand.
;;
;; Later passes (edit distance, embedding similarity) will refine the
;; projection; the ledger stays the source of truth.
;;
;; Usage:
;;   bb scripts/corpus.clj ingest    scan roots, append events to the ledger
;;   bb scripts/corpus.clj project   rebuild docs/lyrics from the ledger
;;   bb scripts/corpus.clj stats     summarize the latest ingest run

(require '[babashka.fs :as fs]
         '[babashka.process :as p]
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
(def lyrics-dir (str (fs/path repo-root "docs" "lyrics")))
(def projections-dir (str (fs/path repo-root "ledgers" "projections")))

(def roots
  [{:root "/home/err/Downloads/Suno Downloads" :space :suno-downloads}
   {:root "/home/err/Music" :space :music}
   {:root "/home/err/devel" :space :devel
    :exclude-globs ["!**/node_modules/**" "!**/.git/**" "!**/dist/**"
                    "!**/target/**" "!**/build/**" "!**/.opencode/**"
                    "!**/.factory/**" "!**/skills/**" "!**/kanban/sources/**"
                    "!**/CHANGELOG*" "!**/.worktrees/**" "!**/out/**"]}])

;; ---------------------------------------------------------------- utils

(defn now-iso [] (str (Instant/now)))
(defn uuid [] (str (UUID/randomUUID)))

(defn sha256-hex ^String [^bytes bs]
  (let [md (.getInstance MessageDigest "SHA-256")]
    (.update md bs)
    (format "%064x" (BigInteger. 1 (.digest md)))))

(defn append-event! [event]
  (spit ledger-path (str (pr-str event) "\n") :append true))

(defn read-events []
  (when (fs/exists? ledger-path)
    (keep (fn [line]
            (when-not (str/blank? line)
              (try (edn/read-string line)
                   (catch Exception _ nil))))
          (str/split-lines (slurp ledger-path)))))

;; ------------------------------------------------------- classification

(defn normalize-body ^String [^String text]
  (-> text
      (str/replace #"\r\n?" "\n")
      (str/replace #"(?m)^ID:\s.*$" "")   ; Suno per-track UUID line
      (str/replace #"(?m)[ \t]+$" "")
      str/trim))

(defn suno-header? [^String head]
  (and (re-find #"(?m)^Title:\s*\S" head)
       (re-find #"(?m)^(ID|Prompt|Tags):" head)))

(def section-tag-re
  ;; Section tags anchored to their own line: [Verse 1], (Chorus - Warden),
  ;; ## VERSE — the Suno/songbook convention. Prose mentions like "(bridge
  ;; process)" or "[hooks]" mid-paragraph do not match.
  #"(?im)^\s*(?:\[(?:verse|chorus|bridge|outro|intro|hook|pre-?chorus|instrumental|interlude|refrain|coda)[^\]]*\]|\((?:verse|chorus|bridge|outro|intro|hook|refrain)\b[^)]*\)|#{1,4}\s*(?:verse|chorus|bridge|outro|intro|hook|refrain)\b\s*(?:\d{1,3}|[ivx]{1,4})?)\s*$")

(defn count-section-tags [^String text]
  (count (re-seq section-tag-re text)))

(defn classify [^String path ^String text]
  (let [head (subs text 0 (min 8192 (count text)))
        p (str/lower-case path)
        fname (str/lower-case (str (fs/file-name path)))
        suno? (suno-header? head)
        tags (count-section-tags text)
        prompt-terms? (boolean (re-find #"(?i)(style prompt|lyric prompt|delivery instructions)" head))
        path-hint? (boolean (re-find #"/(lyrics|poetry|songs|suno)(/|$|_)" p))
        name-hint? (boolean (re-find #"lyric|lullaby|hymn|canticle|psalm|ballad|anthem|chant|songbook" fname))
        analysis? (boolean (re-find #"analysis|metrics|spectrogram|repair|rubric|alignment|asr" p))
        pasted? (boolean (re-find #"sandbox[:_]{1,2}(mnt|/)data|operation_mindfuck_box" head))
        lore-space? (boolean (re-find #"fork_tales|fork-tales|/lore/" p))]
    {:classification
     (cond
       suno? :suno-lyric
       analysis? (if lore-space? :lore :other)
       (>= tags 2) :hand-lyric
       (and (>= tags 1) (or name-hint? path-hint?)) :hand-lyric
       (or (pos? tags) prompt-terms? path-hint? name-hint?) :lyric-adjacent
       lore-space? :lore
       :else :other)
     :basis (cond-> []
              suno? (conj :suno-header)
              (pos? tags) (conj :section-tags)
              prompt-terms? (conj :prompt-terms)
              path-hint? (conj :path-hint)
              name-hint? (conj :name-hint)
              analysis? (conj :analysis-doc))
     :flags (cond-> [] pasted? (conj :pasted-artifact))}))

(defn extract-title [^String path ^String text]
  (or (some-> (re-find #"(?m)^Title:\s*(.+?)\s*$" text) second)
      (some-> (re-find #"(?m)^#\s+(?:Lyrics:\s*)?(.+?)\s*$" text) second)
      (-> (str (fs/file-name path))
          (str/replace #"\.(md|txt)$" "")
          (str/replace #"(?i)\s*\(\d+\)$" "")
          (str/replace #"(?i)^(lyrics_|new_lyrics_|04_songs_|payload_|reports_)" "")
          (str/replace #"(?i)^\d{8}_\d{6}_seed\d+_" "")
          (str/replace #"(?i)^[0-9a-f]{12}_" "")
          (str/replace #"(?i)^ημ_op_mf_part_\d+_" ""))))

;; ---------------------------------------------------------------- ingest

(defn enumerate-files [{:keys [root exclude-globs]}]
  (let [args (cond-> ["rg" "--files" root "-g" "*.md" "-g" "*.txt" "--no-messages" "--no-ignore"]
               (seq exclude-globs) (into (mapcat (fn [g] ["-g" g]) exclude-globs)))
        {:keys [out exit]} (apply p/sh args)]
    (when (zero? exit)
      (remove str/blank? (str/split-lines out)))))

(defn ingest-file! [run-id path]
  (let [f (fs/file path)]
    (when (and (fs/regular-file? f) (<= (fs/size f) (* 50 1024 1024)))
      (let [bs (fs/read-all-bytes f)
            text (String. ^bytes bs "UTF-8")
            body (normalize-body text)
            {:keys [classification basis flags]} (classify path text)]
        (append-event!
         (cond-> {:event/id (uuid)
                  :event/type :doc/discovered
                  :run/id run-id
                  :ts (now-iso)
                  :path path
                  :sha256 (sha256-hex bs)
                  :body-sha256 (sha256-hex (.getBytes body "UTF-8"))
                  :bytes (alength bs)
                  :classification classification
                  :title (extract-title path text)}
           (seq basis) (assoc :basis basis)
           (seq flags) (assoc :flags flags)))
        classification))))

(defn ingest! []
  (fs/create-dirs (fs/parent ledger-path))
  (let [run-id (uuid)
        files (into [] (mapcat enumerate-files) roots)]
    (append-event! {:event/id (uuid) :event/type :ingest/run-started
                    :run/id run-id :ts (now-iso)
                    :roots (mapv :root roots) :files-planned (count files)})
    (println "Scanning" (count files) "files…")
    (let [counts (reduce (fn [acc [i path]]
                           (when (zero? (mod i 2000)) (println " …" i))
                           (if-let [c (ingest-file! run-id path)]
                             (update acc c (fnil inc 0))
                             acc))
                         {} (map-indexed vector files))]
      (append-event! {:event/id (uuid) :event/type :ingest/run-completed
                      :run/id run-id :ts (now-iso)
                      :counts counts :files-planned (count files)})
      (println "Run" run-id "complete:" counts))))

;; -------------------------------------------------------------- project

(defn source-rank [^String path]
  (cond
    (str/starts-with? path "/home/err/Music/") 1
    (str/includes? path "/orgs/octave-commons/fork_tales/") 2
    (str/starts-with? path "/home/err/devel/Lore/") 3
    (str/starts-with? path "/home/err/devel/LORE/") 3
    (str/starts-with? path "/home/err/devel/Music/") 4
    (str/starts-with? path "/home/err/devel/") 5
    :else 6))

(defn slugify [^String title]
  (when title
    (let [t (-> title
                (str/replace "ημ" "eta-mu")
                (str/replace "Π" "pi") (str/replace "η" "eta") (str/replace "μ" "mu")
                str/lower-case
                (str/replace #"[^a-z0-9]+" "-")
                (str/replace #"(^-+|-+$)" ""))]
      (when-not (str/blank? t)
        (subs t 0 (min 80 (count t)))))))

(defn latest-run-id [events]
  (->> events (filter #(= :ingest/run-completed (:event/type %))) last :run/id))

(defn project! []
  (let [events (read-events)
        run-id (latest-run-id events)]
    (when-not run-id (throw (ex-info "no completed ingest run in ledger" {})))
    (let [docs (->> events
                    (filter #(and (= :doc/discovered (:event/type %))
                                  (= run-id (:run/id %))))
                    (filter #(#{:suno-lyric :hand-lyric} (:classification %))))
          groups (group-by :body-sha256 docs)
          canonical (fn [xs] (->> xs (sort-by (juxt (comp source-rank :path)
                                                    (comp count :path) :path)) first))]
      (when (fs/exists? lyrics-dir) (fs/delete-tree lyrics-dir))
      (fs/create-dirs lyrics-dir)
      (fs/create-dirs projections-dir)
      (loop [entries (vals groups) used #{} idx (sorted-map) n 0 dupes 0]
        (if-let [group (first entries)]
          (let [canon (canonical group)
                base (or (slugify (:title canon))
                         (str "song-" (subs (:body-sha256 canon) 0 8)))
                slug (if (used base)
                       (str base "-" (subs (:body-sha256 canon) 0 8))
                       base)
                ext (or (second (re-find #"\.(md|txt)$" (:path canon))) "md")
                dest (str (fs/path lyrics-dir (str slug "." ext)))]
            (fs/copy (:path canon) dest)
            (recur (rest entries) (conj used slug)
                   (assoc idx slug
                          {:title (:title canon)
                           :classification (:classification canon)
                           :body-sha256 (:body-sha256 canon)
                           :sha256s (vec (distinct (map :sha256 group)))
                           :file (str "docs/lyrics/" slug "." ext)
                           :flags (vec (distinct (mapcat :flags group)))
                           :sources (vec (sort (map :path group)))})
                   (inc n) (+ dupes (dec (count group)))))
          (do
            (let [idx-file (str (fs/path projections-dir "songs-v1.edn"))]
              (spit idx-file (with-out-str (pprint/pprint idx)))
              (spit (str (fs/path lyrics-dir "index.edn"))
                    (with-out-str (pprint/pprint idx)))
              (append-event! {:event/id (uuid) :event/type :projection/computed
                              :ts (now-iso) :projection :songs-v1 :run/id run-id
                              :unique-songs n :duplicates-collapsed dupes
                              :index idx-file})
              (println "Projected" n "unique songs;"
                       dupes "duplicate files collapsed."
                       "Index:" idx-file))))))))

;; ---------------------------------------------------------------- stats

(defn stats! []
  (let [events (read-events)
        run-id (latest-run-id events)
        docs (filter #(and (= :doc/discovered (:event/type %)) (= run-id (:run/id %))) events)
        by-class (frequencies (map :classification docs))
        lyrics (filter #(#{:suno-lyric :hand-lyric} (:classification %)) docs)
        unique (count (distinct (map :body-sha256 lyrics)))]
    (println "Latest run:" run-id)
    (println "Documents discovered:" (count docs))
    (pprint/pprint by-class)
    (println "Lyric files:" (count lyrics) "| unique bodies:" unique)))

(let [cmd (first *command-line-args*)]
  (case cmd
    "ingest" (ingest!)
    "project" (project!)
    "stats" (stats!)
    (println "usage: bb scripts/corpus.clj [ingest|project|stats]")))
