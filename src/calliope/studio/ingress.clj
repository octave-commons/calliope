(ns calliope.studio.ingress
  "Authorized bounded source snapshots. No event append or canonical publication.
   Linux descriptor helper is fixed trusted code, never supplied by a command."
  (:require [calliope.law.capture :as law]
            [calliope.studio.midi :as midi]
            [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.io ByteArrayInputStream Closeable IOException InputStreamReader]
           [java.nio.file Files Path LinkOption SecureDirectoryStream]
           [java.nio.file.attribute BasicFileAttributes BasicFileAttributeView]
           [java.nio.charset StandardCharsets]
           [java.util UUID Base64]
           [java.util.concurrent TimeUnit]
           [javax.sound.sampled AudioSystem AudioFormat$Encoding UnsupportedAudioFileException]
           [javax.sound.midi MidiSystem Sequence MetaMessage ShortMessage InvalidMidiDataException]))

(def max-source-bytes (* 8 1024 1024))
(def no-links (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn- refuse! [code message]
  (throw (ex-info message {:error/code code})))

(defrecord Grant [^Path root root-key root-dev root-ino ^SecureDirectoryStream stream binding closed]
  Closeable
  (close [this]
    (locking this
      (when (compare-and-set! closed false true) (.close stream)))))

(defn open-grant!
  "Native arm boundary calls this with its trusted root/identity, never body data.
   Close on stop/shutdown. An old grant is unusable in a new process/session."
  [^Path root binding]
  (when-not (and (law/valid? :calliope.capture/uuid-v1 (:session/id binding))
                 (law/valid? :calliope.capture/uuid-v1 (:process/id binding))
                 (law/valid? :calliope.studio/actor-v1 (:actor binding))
                 (law/valid? :calliope.capture/clock-v1 (:clock binding))
                 (law/valid? :calliope.capture/origin-v1 (:origin binding))
                 (= :user (get-in binding [:actor :actor/kind])))
    (refuse! :command/not-authorized "A configured local user/session/process is required"))
  (let [root (.normalize (.toAbsolutePath root))
        before (Files/readAttributes root BasicFileAttributes no-links)]
    (when-not (.isDirectory before)
      (refuse! :capture/confinement-unavailable "Staging root must be a direct directory"))
    (let [stream (Files/newDirectoryStream root)]
      (try
        (when-not (and (instance? SecureDirectoryStream stream)
                       (.startsWith (System/getProperty "os.name") "Linux"))
          (refuse! :capture/confinement-unavailable "Secure directory handles and Linux descriptors are required"))
        (let [anchored (.readAttributes ^BasicFileAttributeView
                                        (.getFileAttributeView ^SecureDirectoryStream stream BasicFileAttributeView))
              unix (Files/readAttributes root "unix:dev,ino,fileKey" no-links)]
          (when-not (and (.fileKey before) (= (.fileKey before) (.fileKey anchored))
                         (= (.fileKey before) (get unix "fileKey"))
                         (= (.fileKey before) (.fileKey (Files/readAttributes root BasicFileAttributes no-links))))
            (refuse! :capture/staging-changed "Staging root changed while granting capture"))
          (->Grant root (.fileKey before) (get unix "dev") (get unix "ino") stream binding (atom false)))
        (catch Exception e (.close stream) (throw e))))))

(defn- leaf? [name]
  (and (string? name) (not (#{"." ".."} name))
       (not (re-find #"[/\\:\u0000]" name))))

(defn- root-check! [^Grant grant]
  (try
    (when-not (= (:root-key grant)
                 (.fileKey (Files/readAttributes ^Path (:root grant) BasicFileAttributes no-links)))
      (refuse! :capture/staging-changed "Granted staging root was replaced"))
    (catch java.nio.file.NoSuchFileException _
      (refuse! :capture/staging-changed "Granted staging root is missing"))))

(defn- leaf-check! [^Grant grant source]
  (let [attrs (.readAttributes ^BasicFileAttributeView
                               (.getFileAttributeView ^SecureDirectoryStream (:stream grant)
                                                      (.getPath (.getFileSystem ^Path (:root grant))
                                                                ^String (:filename source) (make-array String 0))
                                                      BasicFileAttributeView no-links))]
    (when-not (and (.isRegularFile attrs) (<= 1 (.size attrs) max-source-bytes))
      (refuse! :capture/unsafe-source "Source must be a bounded regular file directly under staging"))))

(defn- bounded-text [stream limit]
  (with-open [r (InputStreamReader. stream StandardCharsets/UTF_8)]
    (let [buffer (char-array 8192) out (StringBuilder.)]
      (loop []
        (let [n (.read r buffer)]
          (if (neg? n) (str out)
              (do
                (when (> (+ (.length out) n) limit)
                  (refuse! :capture/confinement-unavailable "Descriptor helper response exceeds bound"))
                (.append out buffer 0 n)
                (recur))))))))

(defn- read-async [stream limit]
  (let [result (promise)]
    (Thread/startVirtualThread
     (fn [] (deliver result (try {:text (bounded-text stream limit)}
                                (catch Exception e {:error e})))))
    result))

(defn snapshot-sources!
  "Snapshot the pair through metadata-only O_PATH descriptors, then exact regular
   inode descriptors. Public effect seam for zero-payload-open rejection tests."
  [^Grant grant sources]
  (let [script (slurp (io/resource "studio/secure_ingress.py"))
        builder (ProcessBuilder. ^java.util.List ["/usr/bin/python3" "-I" "-S" "-c" script])
        _ (.clear (.environment builder))
        _ (.put (.environment builder) "LANG" "C.UTF-8")
        process (.start builder)
        stdout (read-async (.getInputStream process) (* 24 1024 1024))
        stderr (read-async (.getErrorStream process) 8192)]
    (try
      (with-open [writer (io/writer (.getOutputStream process) :encoding "UTF-8")]
        (.write writer (json/write-str {"root" (str (:root grant)) "root-dev" (:root-dev grant)
                                       "root-ino" (:root-ino grant) "sources" sources})))
      (when-not (.waitFor process 10 TimeUnit/SECONDS)
        (refuse! :capture/source-timeout "Confined source snapshot timed out"))
      (let [out (deref stdout 1000 nil) err (deref stderr 1000 nil)]
        (when-not (and (zero? (.exitValue process)) out err (nil? (:error out)) (nil? (:error err)))
          (refuse! :capture/confinement-unavailable "Descriptor helper did not complete safely"))
        (let [response (json/read-str (:text out))]
          (when-not (true? (get response "ok"))
            (refuse! (keyword (get response "code" "capture/source-unavailable"))
                     (get response "message" "Confined source snapshot refused")))
          (mapv (fn [source]
                  {:bytes (.decode (Base64/getDecoder) ^String (get source "base64"))
                   :sha256 (get source "sha256")}) (get response "sources"))))
      (finally
        (when (.isAlive process) (.destroyForcibly process))
        (.close (.getInputStream process)) (.close (.getErrorStream process))))))

(defn- wav-metadata [^bytes bytes]
  (try
    (when-not (and (>= (alength bytes) 44)
                   (= "RIFF" (String. bytes 0 4 StandardCharsets/US_ASCII))
                   (= "WAVE" (String. bytes 8 4 StandardCharsets/US_ASCII)))
      (refuse! :capture/invalid-audio "Expected a RIFF/WAVE source"))
    (with-open [stream (AudioSystem/getAudioInputStream (ByteArrayInputStream. bytes))]
      (let [format (.getFormat stream) frames (.getFrameLength stream)
            data (.readAllBytes stream) rate (.getSampleRate format) channels (.getChannels format)]
        (when-not (and (= AudioFormat$Encoding/PCM_SIGNED (.getEncoding format))
                       (= 16 (.getSampleSizeInBits format)) (not (.isBigEndian format))
                       (<= 1 channels 2) (= (* channels 2) (.getFrameSize format))
                       (= rate (double (long rate))) (<= 8000 rate 192000)
                       (= rate (.getFrameRate format)) (pos? frames)
                       (= (* frames (.getFrameSize format)) (alength data)))
          (refuse! :capture/invalid-audio "Expected complete bounded 16-bit little-endian PCM WAV"))
        {:format :wav :sample-rate (long rate) :channels channels :frames frames}))
    (catch UnsupportedAudioFileException _ (refuse! :capture/invalid-audio "Unsupported WAV source"))))

(defn- midi-packets [^bytes bytes clock end-sample]
  (try
    (let [format (MidiSystem/getMidiFileFormat (ByteArrayInputStream. bytes))
          sequence (MidiSystem/getSequence (ByteArrayInputStream. bytes))]
      (when-not (and (#{0 1} (.getType format)) (= Sequence/PPQ (.getDivisionType sequence))
                     (= (:ppq clock) (.getResolution sequence)))
        (refuse! :capture/clock-mismatch "Only declared PPQ SMF type0/type1 is supported"))
      (let [events (sort-by (juxt :tick :track :index)
                            (for [[ti track] (map-indexed vector (.getTracks sequence))
                                  ei (range (.size track)) :let [e (.get track ei)]]
                              {:tick (.getTick e) :track ti :index ei :message (.getMessage e)}))
            tempos (for [{:keys [message]} events
                         :when (and (instance? MetaMessage message) (= 81 (.getType ^MetaMessage message)))]
                     (let [data (.getData ^MetaMessage message)]
                       (when-not (= 3 (alength data))
                         (refuse! :capture/invalid-midi "Malformed tempo message"))
                       (reduce #(+ (* %1 256) (bit-and (int %2) 255)) 0 data)))]
        (when-not (every? #(= (:tempo-us-per-quarter clock) %) (if (seq tempos) tempos [500000]))
          (refuse! :capture/clock-mismatch "Variable or mismatched tempo is unsupported"))
        (vec
         (map-indexed
          (fn [seq packet] (assoc packet :packet/seq seq))
          (keep
           (fn [{:keys [tick message]}]
             (when (> (midi/tick->sample clock tick) end-sample)
               (refuse! :capture/invalid-midi "SMF time exceeds the WAV source"))
             (cond
               (instance? MetaMessage message) nil
               (instance? ShortMessage message)
               (let [m ^ShortMessage message command (.getCommand m)
                     body (cond
                            (= command ShortMessage/NOTE_ON)
                            {:kind :note-on :channel (.getChannel m) :pitch (.getData1 m) :velocity (.getData2 m)}
                            (= command ShortMessage/NOTE_OFF)
                            {:kind :note-off :channel (.getChannel m) :pitch (.getData1 m) :velocity (.getData2 m)}
                            (= command ShortMessage/CONTROL_CHANGE)
                            {:kind :control-change :channel (.getChannel m) :controller (.getData1 m) :value (.getData2 m)}
                            :else (refuse! :capture/unsupported-midi "This capture mapping supports notes and CC only"))]
                 {:device/id "synthetic-midi" :device/epoch 0 :raw/tick tick :midi body})
               :else (refuse! :capture/unsupported-midi "Unsupported MIDI message class"))) events)))))
    (catch InvalidMidiDataException _ (refuse! :capture/invalid-midi "Malformed SMF source"))))

(defn- stable-id [basis]
  (str (UUID/nameUUIDFromBytes (.getBytes (pr-str basis) StandardCharsets/UTF_8))))

(defn- snapshot-take [grant state command]
  (let [[audio-source midi-source] (snapshot-sources! grant [(:audio command) (:midi command)])
        audio (wav-metadata (:bytes audio-source)) clock (:capture/clock state)
        _ (when-not (= (:sample-rate clock) (:sample-rate audio))
            (refuse! :capture/clock-mismatch "Declared sample rate differs from WAV"))
        end (+ (:sample-origin clock) (:frames audio))
        packets (midi-packets (:bytes midi-source) clock end)
        normalized (midi/normalize clock "synthetic-midi" packets end)]
    (when-not (:ok? normalized) (refuse! (:error/code normalized) (:error/message normalized)))
    (let [take {:take/id (stable-id [:take (:session/id state) (:request/id command)])
                :take/session-id (:session/id state)
                :take/audio (assoc audio :render/id (stable-id [:render (:work/id state) (:sha256 audio-source)])
                                        :sha256 (:sha256 audio-source))
                :take/midi {:source/id (stable-id [:midi (:work/id state) (:sha256 midi-source)])
                            :sha256 (:sha256 midi-source) :format :smf}
                :take/clock clock :take/events (:events normalized) :take/notes (:notes normalized)
                :take/interruptions (:interruptions normalized) :take/origin (:capture/origin state)}]
      (when-not (law/valid? :calliope.capture/take-v1 take)
        (refuse! :capture/invalid-take "Normalized take violates the named contract"))
      {:ok? true :take take :sources {:audio audio-source :midi midi-source}})))

(defn ui-ingest!
  "UI entrypoint only. Caller is the native service with its trusted projection
   and configured actor. The provider/system entrypoints below always refuse.
   Durable session admission/idempotency and publication belong to CS-003."
  [^Grant grant state actor command]
  (try
    (locking grant
      (cond
        (not (and (= actor (get-in grant [:binding :actor])) (= :user (:actor/kind actor))))
        (refuse! :command/not-authorized "Only the configured native UI user may ingest")
        (not (and (not @(:closed grant)) (true? (:capture/armed? state))
                   (= (:process/id state) (get-in grant [:binding :process/id]))))
        (refuse! :capture/not-armed "Explicit capture arming in this process is required")
        (not (law/valid? :calliope.capture/ingest-command-v1 command))
        (refuse! :command/invalid "Malformed version1 ingest command")
        (not (and (= (:session/id state) (:session/id command) (get-in grant [:binding :session/id]))
                   (= (:session/revision state) (:session/revision command))))
        (refuse! :session/conflict "Session identity/revision requires reload")
        (not (and (law/valid? :calliope.capture/clock-v1 (:capture/clock state))
                   (law/valid? :calliope.capture/origin-v1 (:capture/origin state))
                   (law/valid? :calliope.studio/non-blank-string (:work/id state))))
        (refuse! :capture/invalid-clock "Session lacks declared capture configuration")
        (not (and (= (:capture/clock state) (get-in grant [:binding :clock]))
                   (= (:capture/origin state) (get-in grant [:binding :origin]))))
        (refuse! :capture/clock-mismatch "Capture configuration differs from the explicit arm grant")
        (not (every? #(leaf? (:filename %)) [(:audio command) (:midi command)]))
        (refuse! :capture/unsafe-source "Source names must be staging-relative leaves")
        :else (do (root-check! grant)
                  (doseq [source [(:audio command) (:midi command)]] (leaf-check! grant source))
                  (snapshot-take grant state command))))
    (catch clojure.lang.ExceptionInfo e
      (if-let [code (:error/code (ex-data e))]
        {:ok? false :error/code code :error/message (.getMessage e)}
        (throw e)))
    (catch IOException _ {:ok? false :error/code :capture/source-unavailable
                          :error/message "Confined source snapshot is unavailable"})))

(defn provider-ingest! [& _]
  {:ok? false :error/code :command/not-authorized :error/message "Provider completion cannot import sources"})

(defn system-ingest! [& _]
  {:ok? false :error/code :command/not-authorized :error/message "System completion cannot import sources"})
