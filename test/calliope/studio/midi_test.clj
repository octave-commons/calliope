(ns calliope.studio.midi-test
  (:require [calliope.law.capture :as law]
            [calliope.studio.midi :as midi]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]))

(def clock {:sample-rate 48000 :ppq 480 :tempo-us-per-quarter 500000
            :sample-origin 0 :rounding :floor :mapping/id :ppq-samples-v1})

(defn packet [seq tick body]
  {:packet/seq seq :device/id "synthetic-midi" :device/epoch 0 :raw/tick tick :midi body})
(defn on [seq tick channel pitch velocity]
  (packet seq tick {:kind :note-on :channel channel :pitch pitch :velocity velocity}))
(defn off [seq tick channel pitch]
  (packet seq tick {:kind :note-off :channel channel :pitch pitch :velocity 0}))
(defn pedal [seq tick channel value]
  (packet seq tick {:kind :control-change :channel channel :controller 64 :value value}))
(defn normalize [packets end]
  (midi/normalize clock "synthetic-midi" packets end))

(deftest capture-contracts-are-named-closed-edn
  (is (= law/registry (edn/read-string (pr-str law/registry))))
  (doseq [type (keys law/registry)] (is (ifn? (law/validator type)) (str type)))
  (is (law/valid? :calliope.capture/clock-v1 clock))
  (is (not (law/valid? :calliope.capture/clock-v1 (assoc clock :guess-tempo true))))
  (is (law/valid? :calliope.capture/packet-v1 (on 0 0 0 60 96))))

(deftest exact-clock-and-outward-views
  (is (= [0 12000 24000] (mapv #(midi/tick->sample clock %) [0 240 480])))
  (is (= 12007 (midi/tick->sample (assoc clock :sample-origin 7) 240)))
  (is (= 50000000000000 (midi/tick->sample clock 1000000000000)))
  (is (= 45 (midi/tick->sample (assoc clock :sample-rate 44100) 1)))
  (is (= {:start-ms 0 :end-ms 500} (midi/sample-range->ms 48000 0 24000)))
  (is (= {:start-ms 0 :end-ms 1} (midi/sample-range->ms 48000 1 2)))
  (doseq [bad-clock [(assoc clock :ppq 0) (assoc clock :sample-rate -1)
                     (assoc clock :tempo-us-per-quarter 0)
                     (assoc clock :sample-origin law/max-sample)]]
    (is (thrown? clojure.lang.ExceptionInfo (midi/tick->sample bad-clock 480))))
  (doseq [tick [-1 1.5 Long/MAX_VALUE]]
    (is (thrown? clojure.lang.ExceptionInfo (midi/tick->sample clock tick)))))

(deftest note-spans-and-velocity-zero
  (doseq [end [(off 1 480 0 60) (on 1 480 0 60 0)]]
    (let [result (normalize [(on 0 0 0 60 96) end] 48000)
          note (first (:notes result))]
      (is (:ok? result))
      (is (= [0 24000 24000 false]
             ((juxt :key/start-sample :key/end-sample :sound/end-sample :interrupted?) note)))
      (is (= 2 (count (:events result))))))
  (let [input [(on 0 0 0 60 96) (off 1 480 0 60)]
        a (normalize input 48000)]
    (is (= a (normalize input 48000)))
    (is (= [0 24000] (mapv :sample/offset (:events a))))
    (is (= input (mapv #(dissoc % :sample/offset) (:events a))))))

(deftest overlap-is-fifo-and-channels-are-independent
  (let [result (normalize [(on 0 0 0 60 96) (on 1 0 1 60 96)
                           (on 2 120 0 60 80) (off 3 240 0 60)
                           (off 4 360 1 60) (off 5 480 0 60)] 48000)]
    (is (:ok? result))
    (is (= [[0 0 12000] [1 0 18000] [0 6000 24000]]
           (mapv (juxt :channel :key/start-sample :key/end-sample) (:notes result))))))

(deftest sustain-retains-physical-and-sounding-spans
  (let [result (normalize [(on 0 0 0 60 96) (on 1 0 1 64 80)
                           (pedal 2 120 0 64) (off 3 240 0 60)
                           (off 4 240 1 64) (pedal 5 480 0 63)] 48000)]
    (is (:ok? result))
    (is (= [[12000 24000] [12000 12000]]
           (mapv (juxt :key/end-sample :sound/end-sample) (:notes result))))
    (is (= [64 63] (mapv #(get-in % [:midi :value])
                         (filter #(= :control-change (get-in % [:midi :kind])) (:events result)))))))

(deftest malformed-sequences-admit-no-take
  (doseq [input [[(off 0 0 0 60)]
                 [(on 0 0 0 60 96) (off 0 480 0 60)]
                 [(on 0 480 0 60 96) (off 1 0 0 60)]
                 [(assoc (on 0 0 0 60 96) :device/id "undeclared")]
                 [(assoc (on 0 0 0 60 96) :device/epoch 1)]
                 [(on 0 0 0 60 96) (off 1 960 0 60)]
                 [{:kind :stop :sample/offset 0} (on 0 0 0 60 96)]]]
    (let [result (normalize input 24000)]
      (is (false? (:ok? result)) (pr-str input))
      (is (not (contains? result :events)))
      (is (not (contains? result :notes))))))

(deftest disconnect-and-end-are-derived-interruptions
  (let [result (normalize [(on 0 0 0 60 96)
                           {:kind :disconnect :device/id "synthetic-midi"
                            :device/epoch 0 :sample/offset 10000}
                           {:kind :reconnect :device/id "synthetic-midi"
                            :device/epoch 1 :sample-origin 10000}
                           (assoc (on 0 0 0 64 80) :device/epoch 1)
                           (assoc (off 1 240 0 64) :device/epoch 1)] 48000)]
    (is (:ok? result))
    (is (= [true false] (mapv :interrupted? (:notes result))))
    (is (= [10000 22000] (mapv :sound/end-sample (:notes result))))
    (is (= [0 10000 10000 10000 22000] (mapv :sample/offset (:events result))))
    (is (= 1 (count (:interruptions result)))))
  (let [result (normalize [(on 0 0 0 60 96)] 24000)]
    (is (= [24000 true] ((juxt :sound/end-sample :interrupted?) (first (:notes result)))))
    (is (= :end-of-take (get-in result [:interruptions 0 :reason]))))
  (testing "disconnected packets and a backward reconnect refuse"
    (let [d {:kind :disconnect :device/id "synthetic-midi" :device/epoch 0 :sample/offset 10000}]
      (is (false? (:ok? (normalize [d (on 0 240 0 60 96)] 48000))))
      (is (false? (:ok? (normalize [d {:kind :reconnect :device/id "synthetic-midi"
                                      :device/epoch 1 :sample-origin 9999}] 48000)))))))
