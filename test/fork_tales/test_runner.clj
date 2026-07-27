(ns fork-tales.test-runner
  (:require [clojure.test :as test]
            [fork-tales.classifier.dsl-test]))

(defn -main
  [& _]
  (let [{:keys [fail error]}
        (test/run-tests 'fork-tales.classifier.dsl-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
