(ns calliope.companion.store
  "Filesystem adapter; reads metadata files only, never audio or remote URLs."
  (:require [calliope.suno.metadata :as metadata]
            [calliope.companion.review :as review]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io FileOutputStream PushbackReader RandomAccessFile]
           [java.nio.file Files StandardCopyOption CopyOption FileVisitOption LinkOption]
           [java.nio.file.attribute FileAttribute PosixFilePermissions]
           [java.security MessageDigest]
           [java.math BigInteger]
           [java.time Instant]
           [java.util UUID]))

(defn path [root relative] (io/file root relative))
(defn now [] (str (Instant/now)))
(defn sha256 [text]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                         (.getBytes ^String text "UTF-8")))))

(defn read-events
  ([file] (read-events file (constantly true)))
  ([file predicate]
   (if-not (.isFile (io/file file)) []
     (with-open [reader (PushbackReader. (io/reader file))]
       (loop [out []]
         (let [event (edn/read {:eof ::eof} reader)]
           (cond (= ::eof event) out
                 (map? event) (recur (if (predicate event) (conj out event) out))
                 :else (throw (ex-info "Invalid ledger form" {:file (str file)})))))))))

(defn metadata-event? [event] (= :suno/metadata-observed (:event/type event)))

(defn- locked! [file f]
  (io/make-parents file)
  (with-open [handle (RandomAccessFile. (io/file file) "rw")
              channel (.getChannel handle)
              _lock (.lock channel)]
    (f)))

(defn- append-events! [file events]
  (when (seq events)
    (let [separator (with-open [handle (RandomAccessFile. (io/file file) "r")]
                      (if (zero? (.length handle)) ""
                        (do (.seek handle (dec (.length handle)))
                            (if (= 10 (.read handle)) "" "\n"))))
          bytes (.getBytes (str separator (str/join "\n" (map pr-str events)) "\n") "UTF-8")]
      (with-open [out (FileOutputStream. (io/file file) true)]
        (.write out bytes) (.sync (.getFD out))))))

(defn- write-projection! [root projection]
  (let [target (path root "ledgers/projections/suno-meta-v1.edn")]
    (io/make-parents target)
    (let [posix? (contains? (.supportedFileAttributeViews (.getFileSystem (.toPath target))) "posix")
          attrs (into-array FileAttribute
                            (when posix? [(PosixFilePermissions/asFileAttribute
                                           (PosixFilePermissions/fromString "rw-rw-rw-"))]))
          temp (Files/createTempFile (.toPath (.getParentFile target)) "suno-" ".edn" attrs)
          permissions (when (and posix? (.exists target))
                        (Files/getPosixFilePermissions (.toPath target) (make-array LinkOption 0)))]
      (try
        (spit (.toFile temp) (str (pr-str projection) "\n"))
        (when permissions (Files/setPosixFilePermissions temp permissions))
        (Files/move temp (.toPath target)
                    (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING StandardCopyOption/ATOMIC_MOVE]))
        (finally (Files/deleteIfExists temp))))))

(defonce ^:private catalog-cache (atom {}))
(defn catalog [root]
  (let [file (path root "ledgers/ingest.edn") key (.getCanonicalPath file)
        stamp [(.length file) (when (.exists file)
                               (str (Files/getLastModifiedTime (.toPath file) (make-array LinkOption 0))))]
        cached (get @catalog-cache key)]
    (if (= stamp (:stamp cached)) (:projection cached)
      (let [projection (metadata/replay (read-events file metadata-event?))]
        ;; Store only under the stamp read before replay. A concurrent append invalidates this entry.
        (swap! catalog-cache assoc key {:stamp stamp :projection projection}) projection))))
(defn reviews [root]
  (review/replay (read-events (path root "ledgers/studio.edn"))))
(defn project! [root]
  (let [ledger (path root "ledgers/ingest.edn")]
    (locked! ledger #(let [projection (catalog root)]
                       (write-projection! root projection) projection))))

(defn source-files [source]
  (let [source (io/file source)]
    (when-not (.exists source) (throw (ex-info "Metadata source is missing" {:source (str source)})))
    (with-open [paths (Files/walk (.toPath source) (make-array FileVisitOption 0))]
      (->> (iterator-seq (.iterator paths))
           (remove #(Files/isSymbolicLink %))
           (map #(.toFile ^java.nio.file.Path %))
           (filter #(.isFile ^java.io.File %))
           (filter #(re-find #"(?i)\.(json|txt)$" (.getName ^java.io.File %)))
           (sort-by str) vec))))

(defn- observe-file [file]
  (let [raw (slurp file :encoding "UTF-8")
        text? (str/ends-with? (str/lower-case (.getName ^java.io.File file)) ".txt")]
    (if (and text? (not (re-find #"(?m)^ID:[ \t]*\S+" raw)))
      {:observations [] :skipped 1}
      (let [payload (if text? (metadata/parse-text raw) (json/read-str raw))
            source {:source/name (.getName ^java.io.File file)
                    :source/sha256 (sha256 raw) :source/format (if text? :suno-text :suno-json)
                    :source/variation (metadata/source-variation (.getName ^java.io.File file))}]
        {:skipped 0
         :observations (mapv (fn [clip] {:clip (metadata/normalize clip)
                                        :observation/sha256 (sha256 (pr-str (metadata/safe-data clip)))
                                        :source source})
                             (metadata/payload-clips payload))}))))

(defn import! [root source]
  (let [results (mapv (fn [file]
                        (try (observe-file file)
                             (catch Exception e {:error {:file (.getName ^java.io.File file)
                                                         :message (.getMessage e)}})))
                      (source-files source))
        observations (mapcat :observations results)
        ledger (path root "ledgers/ingest.edn")]
    (locked! ledger
             (fn []
               (let [events (read-events ledger metadata-event?)
                     known (into #{} (comp (filter #(= :suno/metadata-observed (:event/type %)))
                                            (map #(vector (:schema %) (get-in % [:clip :clip/id])
                                                          (:observation/sha256 %)))) events)
                     fresh (:fresh
                            (reduce (fn [{:keys [seen] :as state} observation]
                                      (let [key [metadata/schema (get-in observation [:clip :clip/id])
                                                 (:observation/sha256 observation)]]
                                        (if (contains? seen key) state
                                          (-> state (update :seen conj key) (update :fresh conj observation)))))
                                    {:seen known :fresh []} observations))
                     run-id (str (UUID/randomUUID))
                     new-events (mapv #(merge % {:event/id (str (UUID/randomUUID))
                                                :event/type :suno/metadata-observed
                                                :schema metadata/schema :run/id run-id
                                                :ts (now) :epistemic-tier :observed}) fresh)
                     projection (metadata/replay (concat events new-events))]
                 (append-events! ledger new-events)
                 (write-projection! root projection)
                 {:files (count results) :observations (count observations)
                  :appended (count new-events) :clips (count (:clips projection))
                  :skipped (reduce + 0 (keep :skipped results))
                  :errors (vec (keep :error results))})))))

(defn save-review!
  ([root record] (save-review! root record ::current))
  ([root record expected-head]
  (let [clip (get-in (catalog root) [:clips (:clip/id record)])]
    (when-not clip (throw (ex-info "Import this Suno clip before reviewing it" {:id (:clip/id record)})))
    (review/validate record (:clip/duration clip))
    (let [id (:clip/id record) ledger (path root "ledgers/studio.edn")]
      (locked! ledger
               (fn []
                 (let [state (reviews root) head (get-in state [:heads id])
                       _ (when-not (= ::current expected-head) (review/assert-head! state id expected-head))
                       patch (review/patch-between (review/record-at state id head) record)]
                   (when (seq patch)
                     (let [event {:event/id (str (UUID/randomUUID)) :event/type :listening/review-recorded
                                  :schema :calliope.listening/review-v2 :ts (now) :author :user
                                  :epistemic-tier :observed :clip/id id :review/parent head :review/patch patch}]
                       (review/apply-event state event)
                       (append-events! ledger [event]) event)))))))))

(defn move-history!
  "Append an undo, redo, or explicit checkout. No revision or older event is removed."
  [root id action target expected-head]
  (let [ledger (path root "ledgers/studio.edn")]
    (locked! ledger
             (fn []
               (let [state (reviews root) head (get-in state [:heads id])
                     _ (review/assert-head! state id expected-head)
                     to (case action :undo (review/undo-target state id)
                              :redo (review/redo-target state id) :checkout target
                              (throw (ex-info "Unknown history action" {:action action})))]
                 (when (and (not= head to) (or (not= action :redo) to))
                   (let [event {:event/id (str (UUID/randomUUID)) :event/type :listening/history-moved
                                :schema :calliope.listening/history-v1 :ts (now) :author :user
                                :epistemic-tier :observed :clip/id id :history/action action
                                :history/from head :history/to to}]
                     (review/apply-event state event)
                     (append-events! ledger [event]) event)))))))
