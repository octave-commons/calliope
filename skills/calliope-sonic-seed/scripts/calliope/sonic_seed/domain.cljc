(ns calliope.sonic-seed.domain
  (:require [calliope.sonic-seed.law :as law]))

(def ^:private modulus 2147483647)
(def ^:private multiplier 48271)

(def ^:private bpms [72 78 82 84 88 92 96 104])
(def ^:private roots [36 38 41 43 45 48])
(def ^:private scales [[0 3 5 7 10]
                       [0 2 3 7 9]
                       [0 2 5 7 10]
                       [0 3 5 8 10]])
(def ^:private lengths [1 1 1 2 2 4])
(def ^:private hex-values
  (zipmap "0123456789abcdef" (range 16)))

(defn- hex-prefix-int
  [hex]
  (reduce (fn [value digit]
            (+ (* value 16) (get hex-values digit 0)))
          0
          (take 8 hex)))

(defn- initial-state
  [seed-sha256]
  (inc (mod (hex-prefix-int seed-sha256) (dec modulus))))

(defn- next-state
  [state]
  (mod (* state multiplier) modulus))

(defn- draw
  [state bound]
  (let [state' (next-state state)]
    [state' (mod state' bound)]))

(defn- choose
  [state values]
  (let [[state' index] (draw state (count values))]
    [state' (nth values index)]))

(defn- notes
  [initial root scale]
  (loop [state initial
         step 0
         result []]
    (if (= step law/steps-per-bar)
      [state result]
      (let [[state density] (draw state 100)]
        (if (>= density 62)
          (recur state (inc step) result)
          (let [[state degree] (choose state scale)
                [state octave?] (draw state 100)
                [state velocity] (draw state 55)
                [state length] (choose state lengths)
                length (min length (- law/steps-per-bar step))]
            (recur state
                   (inc step)
                   (conj result
                         (sorted-map
                          :step step
                          :note (+ root degree (if (< octave? 18) 12 0))
                          :velocity (+ 55 velocity)
                          :length-steps length)))))))))

(defn- hats
  [initial]
  (loop [state initial
         steps (range 0 law/steps-per-bar 2)
         result []]
    (if-let [step (first steps)]
      (let [[state density] (draw state 100)]
        (recur state
               (rest steps)
               (cond-> result (< density 78) (conj step))))
      [state result])))

(defn generate-seed
  [request]
  (let [seed-key (:seed/key request)
        seed-sha256 (:seed/sha256 request)
        min-seconds (:render/min-seconds request)
        sample-rate (:render/sample-rate request)
        channels (:render/channels request)
        state (initial-state seed-sha256)
        [state bpm] (choose state bpms)
        [state root] (choose state roots)
        [state scale] (choose state scales)
        [state note-events] (notes state root scale)
        [state extra-kick?] (draw state 2)
        [_ hat-events] (hats state)
        bars (law/bars-for-duration bpm min-seconds)
        duration (law/render-duration-seconds bpm bars)]
    (sorted-map
     :seed/schema law/schema-id
     :generator/id law/generator-id
     :seed/key seed-key
     :seed/sha256 seed-sha256
     :music/bpm bpm
     :music/root-midi root
     :music/scale (vec scale)
     :music/steps law/steps-per-bar
     :music/notes (vec note-events)
     :music/drums (sorted-map
                   :kick (cond-> [0 8] (= 1 extra-kick?) (conj 6))
                   :snare [4 12]
                   :hats (vec hat-events))
     :render/min-seconds min-seconds
     :render/sample-rate sample-rate
     :render/channels channels
     :render/bars bars
     :render/duration-seconds duration)))
