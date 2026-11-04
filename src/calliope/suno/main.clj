(ns calliope.suno.main
  (:require [calliope.companion.store :as store]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(defn -main [& args]
  (try
    (let [root (.getCanonicalPath (io/file "."))]
      (case (first args)
        "import" (do (when-not (= 2 (count args)) (throw (ex-info "Usage: import FILE_OR_DIRECTORY" {})))
                     (let [result (store/import! root (second args))]
                       (pprint/pprint result)
                       (when (seq (:errors result)) (System/exit 1))))
        "project" (do (when-not (= 1 (count args)) (throw (ex-info "Usage: project" {})))
                      (println "Projected clips:" (count (:clips (store/project! root)))))
        "stats" (let [clips (vals (:clips (store/catalog root)))]
                  (when-not (= 1 (count args)) (throw (ex-info "Usage: stats" {})))
                  (pprint/pprint {:clips (count clips)
                                  :with-lyrics (count (filter #(seq (:clip/lyrics %)) clips))
                                  :with-controls (count (filter #(seq (:clip/controls %)) clips))
                                  :sources (frequencies (map #(get-in % [:clip/source :source/format]) clips))}))
        "help" (println "clojure -M:suno-meta import FILE_OR_DIRECTORY | project | stats")
        (throw (ex-info "Usage: clojure -M:suno-meta import FILE_OR_DIRECTORY | project | stats | help" {}))))
    (catch Exception e (binding [*out* *err*] (println (.getMessage e))) (System/exit 1))))
