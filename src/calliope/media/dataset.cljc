(ns calliope.media.dataset
  "Manifest-addressed external media dataset operations.

  A dataset is any directory containing `MANIFEST.edn` and its media bytes. The
  `CALLIOPE_MEDIA_ROOT` environment variable selects that directory; otherwise
  the repository's `tracks/` directory is used. Media bytes are externally
  synchronized, while the manifest makes their identity and integrity portable."
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  #?(:clj (:import [java.io BufferedInputStream BufferedReader File FileInputStream InputStreamReader]
                   [java.math BigInteger]
                   [java.security MessageDigest]
                   [java.time Instant])))

;; ---------------------------------------------------------------- constants

(def dataset-id "calliope-media")
(def manifest-schema :calliope.media/manifest-v1)
(def env-var "CALLIOPE_MEDIA_ROOT")
(def default-dataset-dir "tracks")
(def manifest-name "MANIFEST.edn")
(def media-extensions #{"mp3" "jpeg"})

;; --------------------------------------------------------------- resolution

#?(:clj
   (defn find-repo-root
     "Walk from `start` upward to the directory containing `deps.edn`.
     Returns its canonical string path, or throws when no repository is found."
     [start]
     (loop [dir (let [f (File. (str start))]
                  (if (.isDirectory f) f (.getParentFile f)))]
       (cond
         (nil? dir) (throw (ex-info "No repository root containing deps.edn" {:start (str start)}))
         (.isFile (File. dir "deps.edn")) (.getCanonicalPath dir)
         :else (recur (.getParentFile dir)))))
   :cljs
   (defn find-repo-root [& _]
     (throw (ex-info "Media datasets require a JVM filesystem" {}))))

(defn resolve-root
  "Resolve a dataset root. A non-blank value in `env` takes precedence over the
  repository default and the returned `:source` preserves that provenance."
  ([repo-root] (resolve-root repo-root nil))
  ([repo-root env]
   (if (str/blank? env)
     {:root (str repo-root "/" default-dataset-dir) :source :default}
     {:root env :source :env})))

(defn manifest-path
  "Return the manifest path for a dataset root."
  [root]
  (str root "/" manifest-name))

;; --------------------------------------------------------------- hashing

#?(:clj
   (defn sha256-of-file
     "Stream `f` through SHA-256 in 64KB chunks and return lowercase hex."
     [f]
     (let [digest (MessageDigest/getInstance "SHA-256")
           buffer (byte-array 65536)]
       (with-open [in (BufferedInputStream. (FileInputStream. (File. (str f))))]
         (loop [read (.read in buffer)]
           (when (pos? read)
             (.update digest buffer 0 read)
             (recur (.read in buffer)))))
       (format "%064x" (BigInteger. 1 (.digest digest)))))
   :cljs
   (defn sha256-of-file [& _]
     (throw (ex-info "Media datasets require a JVM filesystem" {}))))

;; --------------------------------------------------------------- manifest

#?(:clj
   (defn- relative-path [^File root ^File file]
     (-> (.relativize (.toPath root) (.toPath file)) str (str/replace "\\" "/")))
   :cljs
   (defn- relative-path [& _] nil))

(defn- extension [path]
  (some->> (re-find #"(?i)\.([^.]+)$" (str path)) second str/lower-case))

#?(:clj
   (defn- media-files [root]
     (let [root-file (.getCanonicalFile (File. (str root)))]
       (->> (file-seq root-file)
            (filter #(.isFile ^File %))
            (remove #(= manifest-name (.getName ^File %)))
            (filter #(contains? media-extensions (extension (.getName ^File %))))))))

#?(:clj
   (defn scan-entries
     "Recursively hash media files under `root`, returning path-sorted entries.
     Paths are POSIX dataset-relative paths; every non-media file is ignored."
     [root]
     (let [root-file (.getCanonicalFile (File. (str root)))]
       (->> (media-files root)
            (map (fn [^File file]
                   {:path (relative-path root-file file)
                    :bytes (.length file)
                    :sha256 (sha256-of-file file)}))
            (sort-by :path)
            vec)))
   :cljs
   (defn scan-entries [& _]
     (throw (ex-info "Media datasets require a JVM filesystem" {}))))

#?(:clj
   (defn scan-media-paths
     "Path-sorted dataset-relative media paths under `root`, without hashing."
     [root]
     (let [root-file (.getCanonicalFile (File. (str root)))]
       (->> (media-files root)
            (map #(relative-path root-file ^File %))
            (sort-by identity)
            vec))))

(defn- parse-line [line-number line]
  (try
    (edn/read-string line)
    (catch #?(:clj Exception :cljs :default) cause
      (throw (ex-info (str "Malformed manifest line " line-number)
                      {:line line-number} cause)))))

#?(:clj
   (defn read-manifest
     "Read a line-oriented manifest and validate its envelope and entry count.
     Invalid EDN, schema, or count failures name their offending line."
     [root]
     (let [path (manifest-path root)]
       (with-open [reader (BufferedReader. (InputStreamReader. (FileInputStream. (File. path))))]
         (let [lines (doall (line-seq reader))]
           (when-not (seq lines)
             (throw (ex-info "Malformed manifest line 1" {:line 1 :path path})))
           (let [envelope (parse-line 1 (first lines))
                 entries (mapv (fn [line-number line]
                                 (parse-line line-number line))
                               (range 2 (+ 2 (count (rest lines))))
                               (rest lines))]
             (when-not (= manifest-schema (:schema envelope))
               (throw (ex-info "Malformed manifest line 1: wrong schema"
                               {:line 1 :path path :schema (:schema envelope)})))
             (when-not (= (:entries envelope) (count entries))
               (throw (ex-info "Malformed manifest line 1: entry count mismatch"
                               {:line 1 :path path :expected (:entries envelope)
                                :actual (count entries)})))
             {:dataset/id (:dataset/id envelope)
              :schema (:schema envelope)
              :generated (:generated envelope)
              :entries entries})))))
   :cljs
   (defn read-manifest [& _]
     (throw (ex-info "Media datasets require a JVM filesystem" {}))))

#?(:clj
   (defn write-manifest!
     "Scan `root` and write its deterministic line-oriented manifest."
     [root {:keys [generated]}]
     (let [entries (scan-entries root)
           bytes-total (reduce + 0 (map :bytes entries))
           path (manifest-path root)
           envelope {:dataset/id dataset-id
                     :schema manifest-schema
                     :entries (count entries)
                     :bytes-total bytes-total
                     :generated generated}]
       (spit path (str (str/join "\n" (map pr-str (cons envelope entries))) "\n"))
       {:entries (count entries) :bytes-total bytes-total :path path}))
   :cljs
   (defn write-manifest! [& _]
     (throw (ex-info "Media datasets require a JVM filesystem" {}))))

(defn generate-manifest!
  "Write a manifest stamped with the current ISO-8601 instant."
  [root]
  #?(:clj (write-manifest! root {:generated (str (Instant/now))})
     :cljs (throw (ex-info "Media datasets require a JVM filesystem" {}))))

;; --------------------------------------------------------------- verification

(defn entry-for
  "Return the manifest entry for dataset-relative `relpath`, if present."
  [manifest relpath]
  (some #(when (= relpath (:path %)) %) (:entries manifest)))

#?(:clj
   (defn resolve-file
     "Return the File for dataset-relative `relpath` beneath `root`."
     [root relpath]
     (File. (File. (str root)) relpath))
   :cljs
   (defn resolve-file [& _]
     (throw (ex-info "Media datasets require a JVM filesystem" {}))))

#?(:clj
   (defn verify
     "Verify manifest entries against disk. Extra media files are reported but
     do not change `:ok`; missing, size, and optional hash failures do."
     [root {:keys [hash?]}]
     (let [manifest (read-manifest root)
           checked (:entries manifest)
           report (reduce (fn [acc {:keys [path bytes sha256]}]
                            (let [file (resolve-file root path)
                                  actual-hash (when (and hash? (.isFile ^File file))
                                                (sha256-of-file file))]
                              (cond
                                (not (.isFile ^File file))
                                (update acc :missing conj path)

                                (not= bytes (.length ^File file))
                                (update acc :size-mismatch conj {:path path :expected bytes :actual (.length ^File file)})

                                (and hash? (not= sha256 actual-hash))
                                (update acc :hash-mismatch conj {:path path :expected sha256 :actual actual-hash})

                                :else acc)))
                          {:missing [] :size-mismatch [] :hash-mismatch []}
                          checked)
          listed (set (map :path checked))
          extras (->> (scan-media-paths root) (remove listed) vec)]
       (assoc report
              :ok (every? empty? (vals report))
              :checked (count checked)
              :extras extras)))
   :cljs
   (defn verify [& _]
     (throw (ex-info "Media datasets require a JVM filesystem" {}))))

(defn normalize-dest
  "Convert a historical repo-relative track destination to a dataset-relative path."
  [dest]
  (str/replace-first dest #"^tracks/" ""))

(defn verify-against-ledger
  "Compare media manifest entries with supplied `:track/discovered` events.
  JSON events are intentionally excluded because the media manifest tracks only
  externally synchronized MP3 and JPEG bytes."
  [root events]
  (let [manifest (read-manifest root)
        entries (:entries manifest)
        manifest-by-path (into {} (map (juxt :path identity)) entries)
        events-by-path (into {}
                             (map (fn [event] [(normalize-dest (:dest event)) event]))
                             (filter #(and (= :track/discovered (:event/type %))
                                           (contains? #{:mp3 :jpeg} (:asset %)))
                                     events))
        untracked (->> entries (map :path) (remove events-by-path) vec)
        missing (->> (keys events-by-path) (remove manifest-by-path) sort vec)
        drift (->> events-by-path
                   (keep (fn [[path event]]
                           (when-let [entry (manifest-by-path path)]
                             (when (not= (:bytes event) (:bytes entry))
                               {:path path :expected (:bytes event) :actual (:bytes entry)}))))
                   (sort-by :path)
                   vec)]
    {:untracked-in-ledger untracked
     :missing-from-manifest missing
     :bytes-drift drift}))
