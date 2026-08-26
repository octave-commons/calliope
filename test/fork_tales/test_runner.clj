(ns fork-tales.test-runner
  (:require [clojure.test :as test]
            [fork-tales.classifier.dsl-test]
            [fork-tales.classifier.runtime-test]
            [fork-tales.law.studio-test]))

(defn -main
  [& _]
  (let [{:keys [fail error]}
        (test/run-tests 'fork-tales.classifier.dsl-test
                        'fork-tales.classifier.runtime-test
                        'fork-tales.law.studio-test)]
    (when (pos? (+ fail error))
      (System/exit 1))))
