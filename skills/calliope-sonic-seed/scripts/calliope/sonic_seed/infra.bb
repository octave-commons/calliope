(ns calliope.sonic-seed.infra-bb
  (:require [calliope.sonic-seed.domain :as domain]
            [calliope.sonic-seed.law :as law]
            [calliope.sonic-seed.shape :as shape])
  (:import [java.nio ByteBuffer ByteOrder]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files LinkOption OpenOption Path Paths]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(def ^:private byte-array-class (Class/forName "[B"))
(def ^:private no-link-options (make-array LinkOption 0))
(def ^:private no-file-attributes (make-array FileAttribute 0))
(def ^:private no-open-options (make-array OpenOption 0))

(defn- bytes?
  [value]
  (instance? byte-array-class value))

(defn- ->bytes
  [value]
  (cond
    (bytes? value) value
    (string? value) (.getBytes ^String value StandardCharsets/UTF_8)
    :else (byte-array (map unchecked-byte value))))

(defn sha256
  [value]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (->bytes value))
    (apply str (map #(format "%02x" (bit-and (int %) 255))
                    (.digest digest)))))

(defn- be16 [value]
  [(bit-and (bit-shift-right value 8) 255)
   (bit-and value 255)])

(defn- be32 [value]
  [(bit-and (bit-shift-right value 24) 255)
   (bit-and (bit-shift-right value 16) 255)
   (bit-and (bit-shift-right value 8) 255)
   (bit-and value 255)])

(defn- ascii [value]
  (mapv int (.getBytes ^String value StandardCharsets/US_ASCII)))

(defn- variable-length
  [value]
  (loop [value value
         result [(bit-and value 127)]]
    (let [value' (bit-shift-right value 7)]
      (if (zero? value')
        result
        (recur value'
               (into [(bit-or 128 (bit-and value' 127))] result))))))

(defn- midi-event
  [tick bytes]
  {:tick tick :bytes bytes})

(defn- pattern-midi-events
  [seed]
  (let [step-ticks 24
        bar-ticks (* law/steps-per-bar step-ticks)
        bars (:render/bars seed)
        notes (:music/notes seed)
        drums (:music/drums seed)]
    (vec
     (mapcat
      (fn [bar]
        (let [offset (* bar bar-ticks)
              note-events
              (mapcat (fn [{:keys [step note velocity length-steps]}]
                        [(midi-event (+ offset (* step step-ticks))
                                     [144 note velocity])
                         (midi-event (+ offset (* (+ step length-steps) step-ticks))
                                     [128 note 0])])
                      notes)
              drum-events
              (mapcat (fn [[kind midi-note velocity]]
                        (mapcat (fn [step]
                                  [(midi-event (+ offset (* step step-ticks))
                                               [153 midi-note velocity])
                                   (midi-event (+ offset (* step step-ticks) 12)
                                               [137 midi-note 0])])
                                (get drums kind)))
                      [[:kick 36 104] [:snare 38 90] [:hats 42 54]])]
          (concat note-events drum-events)))
      (range bars)))))

(defn midi-bytes
  [seed]
  (let [ppq 96
        bpm (:music/bpm seed)
        micros-per-quarter (long (Math/round (double (/ 60000000 bpm))))
        tempo (midi-event 0 [255 81 3
                             (bit-and (bit-shift-right micros-per-quarter 16) 255)
                             (bit-and (bit-shift-right micros-per-quarter 8) 255)
                             (bit-and micros-per-quarter 255)])
        events (sort-by (juxt :tick :bytes)
                        (conj (pattern-midi-events seed) tempo))
        [body last-tick]
        (reduce (fn [[bytes previous] {:keys [tick] event-bytes :bytes}]
                  [(into bytes (concat (variable-length (- tick previous)) event-bytes))
                   tick])
                [[] 0]
                events)
        body (into body (concat (variable-length (- (* (:render/bars seed)
                                                        law/steps-per-bar
                                                        24)
                                                     last-tick))
                                [255 47 0]))
        header (concat (ascii "MThd") (be32 6) (be16 0) (be16 1) (be16 ppq))
        track (concat (ascii "MTrk") (be32 (count body)) body)]
    (->bytes (concat header track))))

(defn- deterministic-noise
  [seed-int sample-index]
  (let [value (mod (* (inc (+ seed-int sample-index)) 48271) 2147483647)]
    (- (* 2.0 (/ value 2147483647.0)) 1.0)))

(defn- active-note
  [notes step-position]
  (first (filter (fn [{:keys [step length-steps]}]
                   (and (<= step step-position)
                        (< step-position (+ step length-steps))))
                 notes)))

(defn wav-bytes
  [seed]
  (let [sample-rate (:render/sample-rate seed)
        duration (:render/duration-seconds seed)
        frame-count (long (Math/round (double (* sample-rate duration))))
        data-bytes (* frame-count 2)
        result (doto (ByteBuffer/allocate (+ 44 data-bytes))
                 (.order ByteOrder/LITTLE_ENDIAN))
        bpm (:music/bpm seed)
        steps-per-second (* (/ bpm 60.0) 4.0)
        notes (:music/notes seed)
        drums (:music/drums seed)
        kick (set (:kick drums))
        snare (set (:snare drums))
        hats (set (:hats drums))
        seed-int (reduce (fn [value digit]
                           (+ (* value 16)
                              (.indexOf "0123456789abcdef" (str digit))))
                         0
                         (take 8 (:seed/sha256 seed)))]
    (.put result (.getBytes "RIFF" StandardCharsets/US_ASCII))
    (.putInt result (+ 36 data-bytes))
    (.put result (.getBytes "WAVE" StandardCharsets/US_ASCII))
    (.put result (.getBytes "fmt " StandardCharsets/US_ASCII))
    (.putInt result 16)
    (.putShort result (short 1))
    (.putShort result (short 1))
    (.putInt result sample-rate)
    (.putInt result (* sample-rate 2))
    (.putShort result (short 2))
    (.putShort result (short 16))
    (.put result (.getBytes "data" StandardCharsets/US_ASCII))
    (.putInt result data-bytes)
    (doseq [sample-index (range frame-count)]
      (let [time (/ sample-index sample-rate)
            absolute-step (* time steps-per-second)
            step-position (mod absolute-step law/steps-per-bar)
            step (long (Math/floor step-position))
            step-phase (- step-position step)
            note (active-note notes step-position)
            note-start (if note (:step note) step)
            note-seconds (/ (- step-position note-start) steps-per-second)
            note-hz (if note
                      (* 440.0 (Math/pow 2.0 (/ (- (:note note) 69) 12.0)))
                      0.0)
            tone (if note
                   (* 0.23
                      (Math/exp (* -1.6 note-seconds))
                      (+ (* 0.74 (Math/sin (* 2 Math/PI note-hz time)))
                         (* 0.26 (Math/sin (* 4 Math/PI note-hz time)))))
                   0.0)
            kick-sample (if (contains? kick step)
                          (* 0.33
                             (Math/exp (* -13.0 step-phase))
                             (Math/sin (* 2 Math/PI
                                          (+ 48 (* 34 (Math/exp (* -10 step-phase))))
                                          time)))
                          0.0)
            noise (deterministic-noise seed-int sample-index)
            snare-sample (if (contains? snare step)
                           (* 0.12 noise (Math/exp (* -18.0 step-phase)))
                           0.0)
            hat-sample (if (contains? hats step)
                         (* 0.035 noise (Math/exp (* -34.0 step-phase)))
                         0.0)
            mixed (max -1.0 (min 1.0 (+ tone kick-sample snare-sample hat-sample)))
            pcm (long (Math/round (* mixed 32767)))]
        (.putShort result (short pcm))))
    (.array result)))

(defn- exists?
  [path]
  (Files/exists path no-link-options))

(defn- file-sha256
  [file]
  (sha256 (Files/readAllBytes file)))

(defn- write-immutable!
  [file content]
  (let [intended (->bytes content)]
    (if (exists? file)
      (let [existing (Files/readAllBytes file)
            existing-hash (sha256 existing)
            intended-hash (sha256 intended)]
        (when-not (= existing-hash intended-hash)
          (throw (ex-info "Content-addressed artifact already exists with different bytes"
                          {:sonic-seed/error :immutable-artifact-conflict
                           :path (str file)
                           :existing-sha256 existing-hash
                           :intended-sha256 intended-hash}))))
      (Files/write file intended no-open-options))))

(defn render!
  [{:keys [seed-key output-dir min-seconds]
    :or {output-dir "calliope-seeds"
         min-seconds (long law/suno-min-seconds)}}]
  (let [request {:seed/key seed-key
                 :seed/sha256 (sha256 seed-key)
                 :render/min-seconds min-seconds
                 :render/sample-rate law/default-sample-rate
                 :render/channels law/default-channels}
        seed (domain/generate-seed request)]
    (when-not (shape/valid-seed? seed)
      (throw (ex-info "Generated seed violates its portable shape or laws"
                      {:sonic-seed/error :invalid-seed
                       :seed seed})))
    (let [seed-id (sha256 (pr-str seed))
          directory (-> (Paths/get output-dir (make-array String 0))
                        .toAbsolutePath
                        .normalize
                        (.resolve seed-id))
          seed-file (.resolve directory "seed.edn")
          midi-file (.resolve directory "seed.mid")
          wav-file (.resolve directory "seed.wav")
          receipt-file (.resolve directory "receipt.edn")]
      (Files/createDirectories directory no-file-attributes)
      (write-immutable! seed-file (str (pr-str seed) "\n"))
      (write-immutable! midi-file (midi-bytes seed))
      (write-immutable! wav-file (wav-bytes seed))
      (let [receipt
            (sorted-map
             :receipt/schema law/receipt-schema-id
             :seed/id seed-id
             :seed/key seed-key
             :seed/sha256 (:seed/sha256 seed)
             :generator/id law/generator-id
             :render/min-seconds min-seconds
             :render/duration-seconds (:render/duration-seconds seed)
             :render/bars (:render/bars seed)
             :artifacts
             (sorted-map
              :seed (sorted-map :path "seed.edn" :sha256 (file-sha256 seed-file))
              :midi (sorted-map :path "seed.mid" :sha256 (file-sha256 midi-file))
              :wav (sorted-map :path "seed.wav" :sha256 (file-sha256 wav-file))))]
        (write-immutable! receipt-file (str (pr-str receipt) "\n"))
        (assoc receipt :output/directory (str directory))))))

(defn- parse-number
  [flag value]
  (try
    (let [parsed (Double/parseDouble value)]
      (if (= parsed (Math/floor parsed))
        (long parsed)
        parsed))
    (catch NumberFormatException _
      (throw (ex-info (str flag " must be numeric")
                      {:sonic-seed/error :invalid-argument
                       :flag flag
                       :value value})))))

(defn- parse-args
  [args]
  (loop [remaining args
         options {:output-dir "calliope-seeds"
                  :min-seconds (long law/suno-min-seconds)}]
    (if-let [flag (first remaining)]
      (let [value (second remaining)]
        (when-not value
          (throw (ex-info (str "Missing value for " flag)
                          {:sonic-seed/error :missing-argument-value
                           :flag flag})))
        (recur (nnext remaining)
               (case flag
                 "--seed-key" (assoc options :seed-key value)
                 "--out" (assoc options :output-dir value)
                 "--min-seconds" (assoc options :min-seconds
                                         (parse-number flag value))
                 (throw (ex-info (str "Unknown argument " flag)
                                 {:sonic-seed/error :unknown-argument
                                  :flag flag})))))
      options)))

(defn -main
  [& args]
  (let [options (parse-args args)]
    (when-not (seq (:seed-key options))
      (throw (ex-info "--seed-key is required"
                      {:sonic-seed/error :missing-seed-key})))
    (when (< (:min-seconds options) law/suno-min-seconds)
      (throw (ex-info "--min-seconds cannot be lower than the Suno contract"
                      {:sonic-seed/error :duration-below-suno-minimum
                       :minimum law/suno-min-seconds
                       :requested (:min-seconds options)})))
    (println (pr-str (render! options)))))

(apply -main *command-line-args*)
