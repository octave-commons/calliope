(ns calliope.studio.synthetic
  "Explicit deterministic test signals, never instrument-performance evidence."
  (:require [calliope.law.capture :as law])
  (:import [java.io ByteArrayOutputStream]
           [java.nio ByteBuffer ByteOrder]
           [java.nio.file Files Path StandardOpenOption LinkOption]
           [java.nio.file.attribute BasicFileAttributes]
           [java.security MessageDigest]
           [java.util Arrays]
           [javax.sound.midi Sequence ShortMessage MetaMessage MidiEvent MidiSystem]))

(def clock {:sample-rate 48000 :ppq 480 :tempo-us-per-quarter 500000
            :sample-origin 0 :rounding :floor :mapping/id :ppq-samples-v1})

(defn sha256 [^bytes bytes]
  (.formatHex (java.util.HexFormat/of) (.digest (MessageDigest/getInstance "SHA-256") bytes)))

(defn- wav-bytes [seed]
  (let [frames 192000 size (* frames 4)
        b (doto (ByteBuffer/allocate (+ 44 size)) (.order ByteOrder/LITTLE_ENDIAN))
        phase (* 2.0 Math/PI (/ (mod seed 360) 360.0))]
    (.put b (.getBytes "RIFF" java.nio.charset.StandardCharsets/US_ASCII))
    (.putInt b (+ 36 size))
    (.put b (.getBytes "WAVEfmt " java.nio.charset.StandardCharsets/US_ASCII))
    (.putInt b 16) (.putShort b (short 1)) (.putShort b (short 2))
    (.putInt b 48000) (.putInt b 192000) (.putShort b (short 4)) (.putShort b (short 16))
    (.put b (.getBytes "data" java.nio.charset.StandardCharsets/US_ASCII)) (.putInt b size)
    (dotimes [i frames]
      (let [t (/ i 48000.0)
            guitar (* 8000.0 (StrictMath/exp (* -1.0 (mod t 0.5)))
                      (StrictMath/sin (+ phase (* 2.0 Math/PI 220.0 t))))
            voice (* 6000.0 (StrictMath/sin (* Math/PI (/ t 4.0)))
                     (StrictMath/sin (+ phase (* 2.0 Math/PI 330.0 t))))]
        (.putShort b (short (StrictMath/round guitar)))
        (.putShort b (short (StrictMath/round voice)))))
    (.array b)))

(defn- midi-bytes []
  (let [sequence (Sequence. Sequence/PPQ 480)
        track (.createTrack sequence)
        tempo (doto (MetaMessage.) (.setMessage 81 (byte-array [7 -95 32]) 3))]
    (.add track (MidiEvent. tempo 0))
    (doseq [[channel pitch start end] [[0 60 0 240] [0 64 480 720]
                                       [0 67 960 1200] [0 72 1440 1680]
                                       [1 48 0 480] [1 55 960 1440]]]
      (doseq [[command tick velocity] [[ShortMessage/NOTE_ON start 96]
                                       [ShortMessage/NOTE_OFF end 0]]]
        (.add track (MidiEvent. (doto (ShortMessage.) (.setMessage command channel pitch velocity)) tick))))
    (with-open [out (ByteArrayOutputStream.)]
      (MidiSystem/write sequence 1 out)
      (.toByteArray out))))

(defn fixture [seed]
  (when-not (law/valid? :calliope.capture/nat-v1 seed)
    (throw (ex-info "Fixture seed must be a supported natural integer" {:error/code :capture/invalid-seed})))
  {:audio/bytes (wav-bytes seed) :midi/bytes (midi-bytes) :clock clock
   :origin {:kind :synthetic :generator/id :studio-fixture-v1 :seed seed
            :channel-roles [:guitar-fixture :voice-fixture]}})

(defn write-fixture!
  "Explicitly create staged fixtures; never overwrite different existing bytes."
  [^Path root seed]
  (let [data (fixture seed)]
    (into {}
          (for [[kind key filename] [[:audio :audio/bytes "synthetic-audio.wav"]
                                      [:midi :midi/bytes "synthetic-midi.mid"]]
                :let [bytes (get data key) path (.resolve root ^String filename)]]
            (do
              (if (Files/exists path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                (when-not (and (.isRegularFile (Files/readAttributes path BasicFileAttributes
                                                                    (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
                               (Arrays/equals ^bytes bytes (Files/readAllBytes path)))
                  (throw (ex-info "Staged fixture exists with different content/type"
                                  {:error/code :capture/staging-collision})))
                (Files/write path ^bytes bytes (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE])))
              [kind {:filename filename :sha256 (sha256 bytes)}])))))
