(ns fork-tales.classifier.dsl-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [fork-tales.classifier.dsl :as dsl]))

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

(deftest compile-plan-resolves-runtime-pieces
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
           (mapv :model/provider (:fallback-models plan))))))

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
                [:models :fork-tales/gemma4-e4b-ollama :model/id]
                :fork-tales/not-the-registry-key)
        issues (dsl/lint broken)]
    (is (some #(= :registry/id-mismatch (:issue/code %)) issues))))

(deftest lint-rejects-missing-references
  (let [broken (assoc-in
                program
                [:classifiers :fork-tales/random-ten-theme-discovery-v1
                 :classifier/model]
                :fork-tales/missing-model)
        issues (dsl/lint broken)]
    (is (some #(and (= :reference/missing (:issue/code %))
                    (= :fork-tales/missing-model
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
