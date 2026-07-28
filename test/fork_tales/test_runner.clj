(ns fork-tales.test-runner
  (:require [clojure.test :as test]
            [fork-tales.classifier.dsl-test]
            [fork-tales.classifier.runtime-test]))

(defn -main
  [& _]
  (let [{:keys [fail error]}
        (test/run-tests 'fork-tales.classifier.dsl-test
                        'fork-tales.classifier.runtime-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
