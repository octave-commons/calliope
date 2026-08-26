(ns calliope.law.media-test
  (:require [calliope.law.media :as media]
            [clojure.test :refer [deftest is]]))

(def envelope
  {:dataset/id "calliope-media"
   :schema :calliope.media/manifest-v1
   :entries 1
   :bytes-total 5
   :generated "2026-08-26T00:00:00Z"})

(def entry
  {:path "absence/01234567.mp3"
   :bytes 5
   :sha256 (apply str (repeat 64 "a"))})

(deftest media-manifest-contracts-accept-valid-data
  (is (media/valid? :calliope.media/manifest-envelope-v1 envelope))
  (is (media/valid? :calliope.media/manifest-entry-v1 entry))
  (is (= {:dataset/id "calliope-media"
          :schema :calliope.media/manifest-v1
          :generated "2026-08-26T00:00:00Z"
          :entries [entry]}
         (media/decode-manifest {:dataset/id "calliope-media"
                                 :schema :calliope.media/manifest-v1
                                 :generated "2026-08-26T00:00:00Z"
                                 :entries [entry]}))))

(deftest media-manifest-contracts-reject-invalid-data
  (is (not (media/valid? :calliope.media/manifest-entry-v1 (assoc entry :sha256 "abc"))))
  (is (not (media/valid? :calliope.media/manifest-entry-v1 (assoc entry :path "/absence/x.mp3"))))
  (is (not (media/valid? :calliope.media/manifest-entry-v1 (assoc entry :path "absence/x.wav"))))
  (is (not (media/valid? :calliope.media/manifest-entry-v1 (assoc entry :bytes 0))))
  (is (not (media/valid? :calliope.media/manifest-entry-v1 (assoc entry :bytes -1))))
  (is (not (media/valid? :calliope.media/manifest-envelope-v1 (assoc envelope :schema :wrong))))
  (is (map? (media/decode-manifest (assoc envelope :entries [entry])))))

(deftest media-manifest-contracts-cover-text-and-metadata-content
  (doseq [path ["absence/01234567.json" "text/song.md" "text/song.txt"]]
    (is (media/valid? :calliope.media/manifest-entry-v1 (assoc entry :path path))
        path)))
