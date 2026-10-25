(ns calliope.studio.session-recovery
  "GPL-3.0-or-later. Adapter-owned pending candidate, not an admitted event or
   an execution receipt. Keep interrupted intent inspectable until explicit retry."
  (:require [clio.extern.jvm.fs :as fs]
            [clio.law.append :as append-law]
            [clio.shape.edn :as edn])
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file Files Path LinkOption FileAlreadyExistsException]
           [java.nio.file.attribute BasicFileAttributes FileAttribute]))

(def max-metadata-bytes (* 16 1024 1024))
(def no-links (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn prepare
  "Validate/encode before persistence begins. Normalized metadata has a separate
   bound from the raw WAV/MIDI ingress limit. No media bytes belong here."
  [candidate]
  (append-law/validate-recovery! candidate)
  (let [text (str (pr-str candidate) "\n")]
    (when (> (alength (.getBytes text StandardCharsets/UTF_8)) max-metadata-bytes)
      (throw (ex-info "Normalized request metadata exceeds the storage bound"
                      {:error/code :session/metadata-too-large})))
    text))

(defn read-candidate! [^Path file]
  (when (Files/exists file no-links)
    (let [before (Files/readAttributes file BasicFileAttributes no-links)]
      (when-not (and (.isRegularFile before) (<= 1 (.size before) max-metadata-bytes))
        (throw (ex-info "Pending recovery artifact is not a bounded regular file"
                        {:error/code :session/invalid-recovery})))
      (let [text (fs/read-text (str file))
            after (Files/readAttributes file BasicFileAttributes no-links)]
        (when-not (= [(.fileKey before) (.size before) (.lastModifiedTime before)]
                     [(.fileKey after) (.size after) (.lastModifiedTime after)])
          (throw (ex-info "Pending recovery artifact changed while being read"
                          {:error/code :session/invalid-recovery})))
        (append-law/validate-recovery! (edn/read-one text))))))

(defn- same-candidate! [actual expected]
  (when-not (= actual expected)
    (throw (ex-info "A different pending request must be resolved first"
                    {:error/code :session/recovery-required}))))

(defn publish!
  "Publish without replacing another candidate; force bytes and parent before
   any source publication/event append. Caller holds the application mutex."
  [^Path file candidate text]
  (if-let [old (read-candidate! file)]
    (same-candidate! old candidate)
    (let [temporary (Files/createTempFile (.getParent file) ".pending-" ".edn" (make-array FileAttribute 0))]
      (try
        (fs/write-text! (str temporary) text)
        (try (Files/createLink file temporary)
             (catch FileAlreadyExistsException _ (same-candidate! (read-candidate! file) candidate)))
        (finally (Files/deleteIfExists temporary)))))
  (fs/sync-directory! (str (.getParent file)))
  candidate)

(defn clear!
  "Remove only the acknowledged exact candidate; the admitted event retains its
   complete identity/data. A failed force is not reported as successful cleanup."
  [^Path file candidate]
  (when-let [old (read-candidate! file)] (same-candidate! old candidate))
  (Files/deleteIfExists file)
  (fs/sync-directory! (str (.getParent file)))
  nil)
