(ns calliope.test-runner
  (:require [clojure.test :as test]
            [calliope.classifier.adapters-test]
            [calliope.classifier.dsl-test]
            [calliope.classifier.main-test]
            [calliope.classifier.runtime-test]))

(defn -main
  [& _]
  (let [{:keys [fail error]}
        (test/run-tests 'calliope.classifier.adapters-test
                        'calliope.classifier.dsl-test
                        'calliope.classifier.main-test
                        'calliope.classifier.runtime-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
