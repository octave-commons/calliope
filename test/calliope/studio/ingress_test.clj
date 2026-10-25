(ns calliope.studio.ingress-test
  (:require [calliope.law.capture :as law]
            [calliope.classifier.runtime :as runtime]
            [calliope.studio.ingress :as ingress]
            [calliope.studio.synthetic :as synthetic]
            [calliope.studio.midi-test :refer [clock]]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]])
  (:import [java.nio.file Files Path LinkOption]
           [java.nio.file.attribute FileAttribute]
           [java.util Arrays]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           [javax.sound.midi MidiSystem MidiEvent MetaMessage ShortMessage]))

(def operator {:actor/id "simulated-studio-test-operator" :actor/kind :user})
(def session-id "10000000-0000-4000-8000-000000000001")
(def process-id "20000000-0000-4000-8000-000000000001")
(def request-id "30000000-0000-4000-8000-000000000001")
(def origin {:kind :synthetic :generator/id :studio-fixture-v1 :seed 42
             :channel-roles [:guitar-fixture :voice-fixture]})
(def state {:session/id session-id :session/revision 2 :work/id "test-work"
            :process/id process-id :capture/armed? true :capture/clock clock
            :capture/origin origin})

(defn temp-root [] (Files/createTempDirectory "calliope-ingress-" (make-array FileAttribute 0)))
(defn delete-tree! [^Path root]
  (when (Files/exists root (make-array LinkOption 0))
    (with-open [paths (Files/walk root (make-array java.nio.file.FileVisitOption 0))]
      (doseq [path (reverse (vec (.toArray paths)))] (Files/deleteIfExists ^Path path)))))
(defn command [files]
  (merge {:command/type :studio/ingest :command/version 1 :request/id request-id
          :session/id session-id :session/revision 2} files))
(defn grant [root]
  (ingress/open-grant! root {:session/id session-id :process/id process-id :actor operator
                             :clock clock :origin origin}))

(deftest deterministic-fixture-preserves-separate-real-formats
  (let [a (synthetic/fixture 42) b (synthetic/fixture 42)]
    (is (Arrays/equals ^bytes (:audio/bytes a) ^bytes (:audio/bytes b)))
    (is (Arrays/equals ^bytes (:midi/bytes a) ^bytes (:midi/bytes b)))
    (is (= clock (:clock a)))
    (is (= origin (:origin a))))
  (let [root (temp-root)]
    (try
      (let [files (synthetic/write-fixture! root 42)
            before (mapv #(Files/readAllBytes (.resolve root ^String (:filename %)))
                         [(:audio files) (:midi files)])]
        (with-open [g (grant root)]
          (let [result (ingress/ui-ingest! g state operator (command files))
                again (ingress/ui-ingest! g state operator (command files))
                take (:take result)]
            (is (:ok? result) (pr-str result))
            (is (law/valid? :calliope.capture/take-v1 take))
            (is (= take (:take again)))
            (is (= [48000 2 192000] ((juxt :sample-rate :channels :frames) (:take/audio take))))
            (is (= :smf (get-in take [:take/midi :format])))
            (is (not= (get-in take [:take/audio :render/id]) (get-in take [:take/midi :source/id])))
            (is (= [0 24000 48000 72000]
                   (mapv :key/start-sample (filter #(zero? (:channel %)) (:take/notes take)))))
            (is (= 6 (count (:take/notes take))))
            (doseq [[bytes source] (map vector before [(:audio files) (:midi files)])]
              (is (Arrays/equals ^bytes bytes (Files/readAllBytes (.resolve root ^String (:filename source)))))))))
      (finally (delete-tree! root)))))

(deftest preflight-refusals-open-zero-source-payloads
  (let [root (temp-root) opens (atom 0)]
    (try
      (let [files (synthetic/write-fixture! root 42) cmd (command files)]
        (with-open [g (grant root)]
          (with-redefs [ingress/snapshot-sources! (fn [& _] (swap! opens inc) (throw (Exception. "Payload must remain unopened")))]
            (doseq [[s actor c code]
                    [[(assoc state :capture/armed? false) operator cmd :capture/not-armed]
                     [(assoc state :capture/armed? false :capture/stopped? true) operator cmd :capture/not-armed]
                     [(assoc state :process/id "restarted-process") operator cmd :capture/not-armed]
                     [state (assoc operator :actor/kind :agent) cmd :command/not-authorized]
                     [state (assoc operator :actor/kind :system) cmd :command/not-authorized]
                     [state (assoc operator :actor/id "different-user") cmd :command/not-authorized]
                     [state operator (assoc cmd :actor/kind :user) :command/invalid]
                     [state operator (assoc cmd :entrypoint :ui-user) :command/invalid]
                     [state operator (assoc cmd :session/revision 1) :session/conflict]
                     [state operator (assoc cmd :session/id "10000000-0000-4000-8000-000000000099") :session/conflict]]]
              (is (= code (:error/code (ingress/ui-ingest! g s actor c)))))
            (is (= :command/not-authorized (:error/code (ingress/provider-ingest! g state operator cmd))))
            (is (= :command/not-authorized (:error/code (ingress/system-ingest! g state operator cmd))))
            (doseq [name ["/outside.wav" "../outside.wav" "nested/a.wav" ".." "."
                         "https://example.test/a.wav" "C:\\outside.wav" "a:b.wav" "a\u0000b.wav"
                         (str root "-sibling/outside.wav")]]
              (is (= :capture/unsafe-source
                     (:error/code (ingress/ui-ingest! g state operator (assoc-in cmd [:audio :filename] name))))))
            (is (zero? @opens)))))
      (finally (delete-tree! root)))))

(deftest links-nonregular-entries-and-replaced-roots-refuse-before-payloads
  (let [root (temp-root) outside (temp-root) opens (atom 0)]
    (try
      (let [files (synthetic/write-fixture! root 42) cmd (command files)
            outside-file (.resolve outside "outside.wav")]
        (Files/write outside-file (byte-array [1 2 3]) (make-array java.nio.file.OpenOption 0))
        (Files/createSymbolicLink (.resolve root "linked.wav") outside-file (make-array FileAttribute 0))
        (Files/createDirectory (.resolve root "directory.wav") (make-array FileAttribute 0))
        (with-open [g (grant root)]
          (with-redefs [ingress/snapshot-sources! (fn [& _] (swap! opens inc) (throw (Exception. "Must not open")))]
            (doseq [name ["linked.wav" "directory.wav"]]
              (is (= :capture/unsafe-source
                     (:error/code (ingress/ui-ingest! g state operator (assoc-in cmd [:audio :filename] name))))))
            (let [moved (.resolve outside "old-root")]
              (Files/move root moved (make-array java.nio.file.CopyOption 0))
              (Files/createDirectory root (make-array FileAttribute 0))
              (is (= :capture/staging-changed (:error/code (ingress/ui-ingest! g state operator cmd)))))
            (is (zero? @opens)))))
      (finally (delete-tree! root) (delete-tree! outside)))))

(deftest hashes-and-declared-clock-are-verified-against-actual-bytes
  (let [root (temp-root)]
    (try
      (let [files (synthetic/write-fixture! root 42) cmd (command files)]
        (with-open [g (grant root)]
          (is (= :capture/hash-mismatch
                 (:error/code (ingress/ui-ingest! g state operator
                                                 (assoc-in cmd [:audio :sha256] (apply str (repeat 64 "0")))))))
          (is (= :capture/clock-mismatch
                 (:error/code (ingress/ui-ingest! g (assoc-in state [:capture/clock :ppq] 960) operator cmd))))
          (is (= :capture/invalid-audio
                 (:error/code (ingress/ui-ingest! g state operator (assoc cmd :audio (:midi files))))))
          (is (= :capture/invalid-midi
                 (:error/code (ingress/ui-ingest! g state operator (assoc cmd :midi (:audio files))))))))
      (finally (delete-tree! root)))))

(deftest real-smf-tempo-changes-and-unsupported-events-refuse
  (let [root (temp-root)]
    (try
      (let [files (synthetic/write-fixture! root 42)
            bytes (Files/readAllBytes (.resolve root "synthetic-midi.mid"))]
        (doseq [[name message expected]
                [["variable.mid" (doto (MetaMessage.) (.setMessage 81 (byte-array [9 39 -64]) 3)) :capture/clock-mismatch]
                 ["program.mid" (doto (ShortMessage.) (.setMessage ShortMessage/PROGRAM_CHANGE 0 1 0)) :capture/unsupported-midi]]]
          (let [sequence (MidiSystem/getSequence (ByteArrayInputStream. bytes))
                _ (.add (first (.getTracks sequence)) (MidiEvent. message 480))
                output (ByteArrayOutputStream.)
                _ (MidiSystem/write sequence 1 output)
                raw (.toByteArray output)
                _ (Files/write (.resolve root ^String name) raw (make-array java.nio.file.OpenOption 0))
                cmd (command (assoc files :midi {:filename name :sha256 (synthetic/sha256 raw)}))]
            (with-open [g (grant root)]
              (is (= expected (:error/code (ingress/ui-ingest! g state operator cmd))))))))
      (finally (delete-tree! root)))))

(deftest actual-descriptor-race-suite
  (let [{:keys [exit out err]} (shell/sh "/usr/bin/python3" "-I" "-S" "test/calliope/studio/secure_ingress_test.py")]
    (is (zero? exit) (str out err))))

(deftest model-outcomes-never-hold-the-ingress-boundary
  (let [root (temp-root) entered (promise) release (promise)]
    (try
      (let [files (synthetic/write-fixture! root 42) cmd (command files)]
        (with-open [g (grant root)]
          (with-redefs [runtime/invoke-model-http (fn [& _] (deliver entered true) @release)]
            (let [worker (future (runtime/invoke-model-http {}))]
              (try
                (is (= true (deref entered 5000 :timeout)))
                (let [capture (future (ingress/ui-ingest! g state operator cmd))]
                  (is (:ok? (deref capture 5000 {:ok? false})))
                  (is (not (realized? release))))
                (finally (deliver release :model-finished) @worker))))
          (doseq [failure [:failed :unavailable]]
            (let [calls (atom 0)]
              (with-redefs [runtime/invoke-model-http (fn [& _] (swap! calls inc)
                                                      (throw (ex-info "Unavailable test provider" {:status failure})))]
                (is (:ok? (ingress/ui-ingest! g state operator cmd)))
                (is (zero? @calls)))))))
      (finally (deliver release :cleanup) (delete-tree! root)))))
