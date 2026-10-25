(ns calliope.studio.sources
  "GPL-3.0-or-later. Immutable binary objects, published before event references.
   The application owns this directory; untrusted ingress stays in ingress.clj."
  (:require [calliope.law.capture :as law]
            [clio.extern.jvm.fs :as fs])
  (:import [java.io ByteArrayOutputStream IOException]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel]
           [java.nio.file Files Path LinkOption OpenOption StandardOpenOption FileAlreadyExistsException NoSuchFileException]
           [java.nio.file.attribute BasicFileAttributes FileAttribute]
           [java.security MessageDigest]
           [java.util Arrays HexFormat]))

(def max-bytes (* 8 1024 1024))
(def no-links (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn- refuse! [code message]
  (throw (ex-info message {:error/code code})))

(defn sha256 [^bytes bytes]
  (.formatHex (HexFormat/of) (.digest (MessageDigest/getInstance "SHA-256") bytes)))

(defn path ^Path [^Path root digest]
  (when-not (law/valid? :calliope.studio/sha256-v1 digest)
    (refuse! :source/invalid-hash "Canonical source identity must be a SHA-256 digest"))
  (.resolve root ^String digest))

(defn- read-bytes! [^Path file]
  (let [attrs (Files/readAttributes file BasicFileAttributes no-links)]
    (when-not (and (.isRegularFile attrs) (<= 1 (.size attrs) max-bytes))
      (refuse! :source/invalid-object "Canonical source is not a bounded regular file"))
    (with-open [channel (FileChannel/open file (into-array OpenOption [StandardOpenOption/READ LinkOption/NOFOLLOW_LINKS]))
                out (ByteArrayOutputStream.)]
      (let [buffer (ByteBuffer/allocate 65536)]
        (loop [size 0]
          (.clear buffer)
          (let [n (.read channel buffer)]
            (if (neg? n)
              (do (when-not (= size (.size attrs) (.size channel))
                    (refuse! :source/changed "Canonical source changed while being verified"))
                  (.toByteArray out))
              (do (when (> (+ size n) max-bytes)
                    (refuse! :source/invalid-object "Canonical source exceeds the supported bound"))
                  (.write out (.array buffer) 0 n)
                  (recur (+ size n))))))))))

(defn verify!
  "Verify actual raw bytes; a matching metadata hash alone is insufficient."
  [root digest]
  (let [file (path root digest)]
    (try
      (when-not (= digest (sha256 (read-bytes! file)))
        (refuse! :source/hash-mismatch "Canonical source bytes no longer match their identity"))
      file
      (catch NoSuchFileException cause
        (throw (ex-info "A referenced canonical source is missing" {:error/code :source/missing} cause)))
      (catch IOException cause
        (throw (ex-info "Canonical source could not be verified" {:error/code :source/unavailable} cause))))))

(defn ensure-durable! [^Path root digest]
  (let [file (verify! root digest)]
    (with-open [channel (FileChannel/open file (into-array OpenOption [StandardOpenOption/READ LinkOption/NOFOLLOW_LINKS]))]
      (fs/force-file! channel))
    (fs/sync-directory! (str root))
    file))

(defn publish!
  "Force each temporary inode, link without replacement to its digest name,
   then force the parent. A failed operation never promises rollback of visible
   bytes. Original staging files remain unchanged; orphan objects are preserved."
  [^Path root snapshots]
  (doseq [{:keys [bytes sha256]} snapshots]
    (when-not (and (bytes? bytes) (<= 1 (alength ^bytes bytes) max-bytes)
                   (law/valid? :calliope.studio/sha256-v1 sha256)
                   (= sha256 (calliope.studio.sources/sha256 bytes)))
      (refuse! :source/invalid-snapshot "Source snapshot does not match its bounded binary identity")))
  (fs/require-directory-sync-support! (str root))
  (doseq [{:keys [bytes sha256]} snapshots
          :let [target (path root sha256)
                temporary (Files/createTempFile root ".source-" ".pending" (make-array FileAttribute 0))]]
    (try
      (with-open [channel (FileChannel/open temporary (into-array OpenOption [StandardOpenOption/WRITE]))]
        (let [buffer (ByteBuffer/wrap ^bytes bytes)]
          (while (.hasRemaining buffer) (.write channel buffer)))
        (fs/force-file! channel))
      (try
        (Files/createLink target temporary)
        (catch FileAlreadyExistsException _
          (when-not (Arrays/equals ^bytes bytes ^bytes (read-bytes! target))
            (refuse! :source/collision "An existing digest path contains different bytes"))))
      (fs/sync-directory! (str root))
      (finally (Files/deleteIfExists temporary))))
  (mapv #(path root (:sha256 %)) snapshots))
