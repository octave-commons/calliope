(ns fork-tales.classifier.dsl-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [fork-tales.classifier.dsl :as dsl]
            [malli.core :as m]))

(def program
  (-> "classifiers/theme-discovery-v1.edn"
      io/resource
      slurp
      edn/read-string))

(deftest example-program-is-runnable
  (testing "the example is valid Malli data with coherent registry references"
    (is (dsl/valid? program))
    (is (empty? (dsl/lint program)))
    (is (dsl/runnable? program))))

(deftest compile-plan-resolves-features-and-producers
  (let [plan (dsl/compile-plan
              program
              :fork-tales/random-ten-theme-discovery-v1)]
    (is (= :ollama (get-in plan [:model :model/provider])))
    (is (= :random (get-in plan [:selector :selector/type])))
    (is (= 10 (get-in plan [:selector :selector/count])))
    (is (= :context/songs
           (get-in plan [:context :context/output-key])))
    (is (= :concept/discovered
           (get-in plan [:classifier :classifier/emits :event/type])))
    (is (= [:llama-cpp]
           (mapv :model/provider (:fallback-models plan))))
    (is (= #{:fork-tales/production-style-v1}
           (set (keys (:features plan)))))
    (is (= [:fork-tales/production-style-v1]
           (get-in plan
                   [:feature-producers :fork-tales/production-style-v1])))))

(deftest compile-deterministic-feature-extractor
  (let [plan (dsl/compile-extractor-plan
              program
              :fork-tales/song-sections-v1)
        sample {:object/id "work/example"
                :feature/id :fork-tales/song-sections-v1
                :feature/value
                [{:section/id "work/example/section/0"
                  :section/type :verse
                  :section/label "Verse"
                  :section/ordinal 0
                  :section/start-line 1
                  :section/end-line 2
                  :section/text "first line\nsecond line"}]}]
    (is (= :deterministic
           (get-in plan [:extractor :extractor/type])))
    (is (= :fork-tales/explicit-song-sections-v1
           (get-in plan [:extractor :extractor/resolver])))
    (is (m/validate (:output-schema plan) sample))))

(deftest compile-llm-feature-extractor
  (let [plan (dsl/compile-extractor-plan
              program
              :fork-tales/production-style-v1)]
    (is (= :llm (get-in plan [:extractor :extractor/type])))
    (is (= :ollama (get-in plan [:model :model/provider])))
    (is (= "gemma4:e2b" (get-in plan [:model :model/name])))
    (is (= :context/work
           (get-in plan [:context :context/output-key])))
    (is (= :provider-native
           (get-in plan [:prompt :prompt/output-contract])))))

(deftest closed-contracts-reject-unknown-keys
  (let [broken (assoc-in
                program
                [:models :fork-tales/gemma4-e4b-ollama :model/unknown]
                true)]
    (is (false? (dsl/valid? broken)))
    (is (some? (dsl/explain broken)))))

(deftest lint-rejects-registry-id-drift
  (let [broken (assoc-in
                program
                [:features :fork-tales/production-style-v1 :feature/id]
                :fork-tales/not-the-registry-key)
        issues (dsl/lint broken)]
    (is (some #(= :registry/id-mismatch (:issue/code %)) issues))))

(deftest lint-rejects-missing-references
  (let [broken (assoc-in
                program
                [:extractors :fork-tales/production-style-v1
                 :extractor/produces]
                #{:fork-tales/missing-feature})
        issues (dsl/lint broken)]
    (is (some #(and (= :reference/missing (:issue/code %))
                    (= :fork-tales/missing-feature
                       (get-in % [:issue/data :reference])))
              issues))))

(deftest prompt-must-declare-context-output
  (let [broken (update-in
                program
                [:prompts :fork-tales/shared-concepts-v1 :prompt/variables]
                disj
                :context/songs)
        issues (dsl/lint broken)]
    (is (some #(= :prompt/context-variable-missing (:issue/code %))
              issues))))

(deftest context-inputs-must-be-bound
  (let [broken (assoc-in
                program
                [:contexts :fork-tales/ten-lyric-sections-v1
                 :context/steps 2 :step/input]
                :missing-binding)
        issues (dsl/lint broken)]
    (is (some #(= :context/input-unbound (:issue/code %)) issues))))

(deftest feature-refs-compile-inside-output-contracts
  (is (some? (dsl/feature-value-schema
              program
              :fork-tales/song-sections-v1)))
  (is (some? (dsl/output-schema
              program
              :fork-tales/song-sections-result-v1))))
