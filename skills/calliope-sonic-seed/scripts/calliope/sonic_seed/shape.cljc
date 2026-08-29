(ns calliope.sonic-seed.shape
  (:require [calliope.sonic-seed.law :as law]))

;; Portable Malli schema data. The bundled runtime uses the predicates below so
;; the skill remains self-contained; Calliope/Foresight hosts may compile these
;; shapes with Malli at their boundary.
(def request-schema
  [:map {:closed true}
   [:seed/key [:string {:min 1}]]
   [:render/min-seconds [:double {:min law/suno-min-seconds}]]
   [:render/sample-rate [:= law/default-sample-rate]]
   [:render/channels [:= law/default-channels]]])

(def note-schema
  [:map {:closed true}
   [:step [:int {:min 0 :max 15}]]
   [:note [:int {:min 0 :max 127}]]
   [:velocity [:int {:min 1 :max 127}]]
   [:length-steps [:int {:min 1 :max 16}]]])

(def seed-schema
  [:map {:closed true}
   [:seed/schema [:= law/schema-id]]
   [:generator/id [:= law/generator-id]]
   [:seed/key [:string {:min 1}]]
   [:seed/sha256 [:re #"[0-9a-f]{64}"]]
   [:music/bpm [:int {:min 40 :max 240}]]
   [:music/root-midi [:int {:min 0 :max 127}]]
   [:music/scale [:vector {:min 1} :int]]
   [:music/steps [:= law/steps-per-bar]]
   [:music/notes [:vector note-schema]]
   [:music/drums
    [:map {:closed true}
     [:kick [:vector :int]]
     [:snare [:vector :int]]
     [:hats [:vector :int]]]]
   [:render/min-seconds [:double {:min law/suno-min-seconds}]]
   [:render/sample-rate [:= law/default-sample-rate]]
   [:render/channels [:= law/default-channels]]
   [:render/bars [:int {:min 1}]]
   [:render/duration-seconds [:double {:min law/suno-min-seconds}]]])

(defn sha256?
  [value]
  (boolean (and (string? value)
                (re-matches #"[0-9a-f]{64}" value))))

(defn step?
  [value]
  (and (int? value) (<= 0 value) (< value law/steps-per-bar)))

(defn valid-note?
  [{:keys [step note velocity length-steps]}]
  (and (step? step)
       (int? note) (<= 0 note 127)
       (int? velocity) (<= 1 velocity 127)
       (int? length-steps) (<= 1 length-steps (- law/steps-per-bar step))))

(defn valid-seed?
  [seed]
  (let [bpm (:music/bpm seed)
        bars (:render/bars seed)
        duration (:render/duration-seconds seed)
        drum-steps (mapcat identity (vals (:music/drums seed)))]
    (and (= law/schema-id (:seed/schema seed))
         (= law/generator-id (:generator/id seed))
         (seq (:seed/key seed))
         (sha256? (:seed/sha256 seed))
         (int? bpm) (<= 40 bpm 240)
         (int? (:music/root-midi seed)) (<= 0 (:music/root-midi seed) 127)
         (seq (:music/scale seed))
         (every? #(and (int? %) (<= 0 % 12)) (:music/scale seed))
         (= law/steps-per-bar (:music/steps seed))
         (every? valid-note? (:music/notes seed))
         (= #{:kick :snare :hats} (set (keys (:music/drums seed))))
         (every? step? drum-steps)
         (= law/default-sample-rate (:render/sample-rate seed))
         (= law/default-channels (:render/channels seed))
         (>= (:render/min-seconds seed) law/suno-min-seconds)
         (int? bars) (pos? bars)
         (>= duration (:render/min-seconds seed))
         (law/whole-bar-duration? bpm bars duration))))
