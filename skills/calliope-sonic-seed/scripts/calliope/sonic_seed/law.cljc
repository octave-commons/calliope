(ns calliope.sonic-seed.law)

(def schema-id "calliope.sonic-seed/v1")
(def receipt-schema-id "calliope.sonic-seed.receipt/v1")
(def generator-id "calliope.sonic-seed/nbb-1")
(def suno-min-seconds 6.0)
(def default-sample-rate 44100)
(def default-channels 1)
(def steps-per-bar 16)
(def beats-per-bar 4)

(defn bar-duration-seconds
  [bpm]
  (* beats-per-bar (/ 60.0 bpm)))

(defn bars-for-duration
  [bpm min-seconds]
  (long (Math/ceil (/ min-seconds (bar-duration-seconds bpm)))))

(defn render-duration-seconds
  [bpm bars]
  (* bars (bar-duration-seconds bpm)))

(defn suno-duration-valid?
  [duration-seconds]
  (>= duration-seconds suno-min-seconds))

(defn whole-bar-duration?
  [bpm bars duration-seconds]
  (< (Math/abs (- duration-seconds
                  (render-duration-seconds bpm bars)))
     1.0e-9))
