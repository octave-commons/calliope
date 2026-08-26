(ns calliope.media.dataset-test
  (:require [calliope.media.dataset :as dataset]
            [clojure.test :refer [deftest is testing]])
  (:import [java.io File]
           [java.nio.file Files]))

(defn temp-dir []
  (str (Files/createTempDirectory "calliope-media-test-" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn delete-tree! [root]
  (doseq [file (reverse (file-seq (File. root)))]
    (Files/deleteIfExists (.toPath file))))

(defn write-bytes! [root path bytes]
  (let [file (File. root path)]
    (.mkdirs (.getParentFile file))
    (Files/write (.toPath file) bytes (make-array java.nio.file.OpenOption 0))
    file))

(defn dataset! [root]
  (write-bytes! root "absence/one.mp3" (.getBytes "hello" "UTF-8"))
  (write-bytes! root "absence/two.jpeg" (.getBytes "world" "UTF-8"))
  (dataset/write-manifest! root {:generated "2026-08-26T00:00:00Z"}))

(defmacro with-dataset [[root] & body]
  `(let [~root (temp-dir)]
     (try
       (dataset! ~root)
       ~@body
       (finally (delete-tree! ~root)))))

(deftest manifest-round-trip-and-intact-verification
  (with-dataset [root]
    (let [manifest (dataset/read-manifest root)]
      (is (= dataset/dataset-id (:dataset/id manifest)))
      (is (= dataset/manifest-schema (:schema manifest)))
      (is (= "2026-08-26T00:00:00Z" (:generated manifest)))
      (is (= 2 (count (:entries manifest))))
      (is (= ["absence/one.mp3" "absence/two.jpeg"] (mapv :path (:entries manifest))))
      (is (= {:ok true :checked 2 :missing [] :size-mismatch [] :hash-mismatch [] :extras []}
             (dataset/verify root {:hash? true}))))))

(deftest verification-detects-missing-size-hash-and-extra-files
  (with-dataset [root]
    (Files/delete (.toPath (dataset/resolve-file root "absence/one.mp3")))
    (is (= ["absence/one.mp3"] (:missing (dataset/verify root {:hash? true}))))
    (write-bytes! root "absence/one.mp3" (.getBytes "x" "UTF-8"))
    (is (= [{:path "absence/one.mp3" :expected 5 :actual 1}]
           (:size-mismatch (dataset/verify root {:hash? true}))))
    (write-bytes! root "absence/one.mp3" (.getBytes "hullo" "UTF-8"))
    (is (empty? (:hash-mismatch (dataset/verify root {:hash? false}))))
    (is (= ["absence/one.mp3"] (mapv :path (:hash-mismatch (dataset/verify root {:hash? true})))))
    (write-bytes! root "absence/extra.mp3" (.getBytes "extra" "UTF-8"))
    (is (= ["absence/extra.mp3"] (:extras (dataset/verify root {:hash? true}))))))

(deftest paths-and-roots-normalize-as-specified
  (is (= "absence/one.mp3" (dataset/normalize-dest "tracks/absence/one.mp3")))
  (is (= "absence/one.mp3" (dataset/normalize-dest "absence/one.mp3")))
  (is (= {:root "/tmp/media" :source :env} (dataset/resolve-root "/repo" "/tmp/media")))
  (is (= {:root "/repo/tracks" :source :default} (dataset/resolve-root "/repo" "   "))))

(deftest ledger-verification-uses-media-events-only
  (with-dataset [root]
    (let [report (dataset/verify-against-ledger
                  root
                  [{:event/type :track/discovered :asset :mp3 :dest "tracks/absence/one.mp3" :bytes 5}
                   {:event/type :track/discovered :asset :jpeg :dest "absence/two.jpeg" :bytes 9}
                   {:event/type :track/discovered :asset :mp3 :dest "absent/three.mp3" :bytes 1}
                   {:event/type :track/discovered :asset :json :dest "absence/meta.json" :bytes 1}])]
      (is (= [] (:untracked-in-ledger report)))
      (is (= ["absent/three.mp3"] (:missing-from-manifest report)))
      (is (= [{:path "absence/two.jpeg" :expected 9 :actual 5}]
             (:bytes-drift report))))))

(deftest manifest-read-fails-loudly
  (let [root (temp-dir)
        path (dataset/manifest-path root)]
    (try
      (spit path "not-edn\n")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"line 1" (dataset/read-manifest root)))
      (spit path "{:schema :wrong :entries 0}\n")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"line 1" (dataset/read-manifest root)))
      (spit path "{:dataset/id \"calliope-media\" :schema :calliope.media/manifest-v1 :entries 2 :bytes-total 0 :generated \"now\"}\n")
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"line 1" (dataset/read-manifest root)))
      (finally (delete-tree! root)))))

(deftest sha256-streams-the-known-file-content
  (let [root (temp-dir)]
    (try
      (let [file (write-bytes! root "known.mp3" (.getBytes "hello" "UTF-8"))
            digest (java.security.MessageDigest/getInstance "SHA-256")
            expected (format "%064x" (java.math.BigInteger. 1 (.digest digest (.getBytes "hello" "UTF-8"))))]
        (is (= expected (dataset/sha256-of-file file))))
      (finally (delete-tree! root)))))
