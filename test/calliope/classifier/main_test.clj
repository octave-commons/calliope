(ns calliope.classifier.main-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [calliope.classifier.main :as main]))

(deftest parse-args-applies-every-option-over-the-defaults
  (is (= main/defaults (main/parse-args [])))
  (let [options (main/parse-args ["--" "--program" "programs/x.edn"
                                  "--classifier" "calliope/x-v1"
                                  "--seed" "42"
                                  "--base-dir" "/tmp/corpus"
                                  "--dry-run"
                                  "--output-contract" "tool-call"])]
    (is (= "programs/x.edn" (:program options)))
    (is (= :calliope/x-v1 (:classifier options)))
    (is (= 42 (:run-seed options)))
    (is (= "/tmp/corpus" (:base-dir options)))
    (is (true? (:dry-run? options)))
    (is (= :tool-call (:output-contract options))))
  (testing "--help short-circuits into the help branch"
    (is (:help? (main/parse-args ["--seed" "1" "--help"]))))
  (testing "unknown options and non-integer seeds fail loudly"
    (is (thrown? clojure.lang.ExceptionInfo
                 (main/parse-args ["--teleport"])))
    (let [error (try
                  (main/parse-args ["--seed" "tomorrow"])
                  nil
                  (catch clojure.lang.ExceptionInfo error error))]
      (is (= "--seed" (:option (ex-data error))))
      (is (= "tomorrow" (:value (ex-data error)))))))

(deftest override-output-contract-rewrites-every-prompt
  (let [program {:prompts {:a {:prompt/output-contract :inline-schema}
                           :b {:prompt/output-contract :provider-native}}}
        overridden (main/override-output-contract program :tool-call)]
    (is (= :tool-call (get-in overridden [:prompts :a :prompt/output-contract])))
    (is (= :tool-call (get-in overridden [:prompts :b :prompt/output-contract])))))

(deftest load-program-reads-files-classpath-resources-and-fails-loudly
  (testing "classpath resources load when no relative file exists"
    (let [program (main/load-program "." "classifiers/theme-discovery-v1.edn")]
      (is (map? program))
      (is (contains? program :classifiers))))
  (testing "a missing program explains where it looked"
    (let [error (try
                  (main/load-program "." "classifiers/absent-v99.edn")
                  nil
                  (catch clojure.lang.ExceptionInfo error error))]
      (is (some? error))
      (is (= "classifiers/absent-v99.edn" (:program (ex-data error)))))))

(deftest usage-documents-the-interface
  (is (str/includes? (#'main/usage) "Usage"))
  (is (str/includes? (#'main/usage) "--dry-run")))

(deftest main-help-prints-usage-and-returns
  (is (str/includes? (with-out-str (main/-main "--help")) "Usage")))

(deftest main-dry-run-executes-the-pipeline-without-model-calls
  (let [output (with-out-str
                 (main/-main "--" "--seed" "3721599729" "--dry-run"))]
    (is (str/includes? output ":dry-run? true"))
    (is (str/includes? output ":classifier/id :calliope/random-ten-theme-discovery-v1"))))
