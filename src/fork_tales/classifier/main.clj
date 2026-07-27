(ns fork-tales.classifier.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [fork-tales.classifier.runtime :as runtime]))

(def defaults
  {:program "classifiers/theme-discovery-v1.edn"
   :classifier :fork-tales/random-ten-theme-discovery-v1
   :base-dir "."
   :run-seed 0
   :dry-run? false})

(defn- usage
  []
  (str/join
   "\n"
   ["Fork Tales classifier interpreter"
    ""
    "Usage:"
    "  clojure -M:classify -- [options]"
    ""
    "Options:"
    "  --program PATH       EDN classifier program or classpath resource"
    "  --classifier ID      classifier keyword"
    "  --seed N             reproducible selection seed"
    "  --base-dir PATH      repository root (default .)"
    "  --dry-run            build selection and prompts without model calls"
    "  --help               show this message"]))

(defn- parse-long!
  [value option]
  (try
    (Long/parseLong value)
    (catch NumberFormatException _
      (throw (ex-info "Option requires an integer."
                      {:option option :value value})))))

(defn parse-args
  [args]
  (loop [remaining args
         options defaults]
    (if-let [arg (first remaining)]
      (case arg
        "--program"
        (recur (nnext remaining)
               (assoc options :program (second remaining)))

        "--classifier"
        (recur (nnext remaining)
               (assoc options :classifier (keyword (second remaining))))

        "--seed"
        (recur (nnext remaining)
               (assoc options :run-seed (parse-long! (second remaining) arg)))

        "--base-dir"
        (recur (nnext remaining)
               (assoc options :base-dir (second remaining)))

        "--dry-run"
        (recur (next remaining) (assoc options :dry-run? true))

        "--help"
        (assoc options :help? true)

        (throw (ex-info "Unknown command-line option."
                        {:option arg})))
      options)))

(defn load-program
  [base-dir path]
  (let [file (io/file base-dir path)
        source (cond
                 (.exists file) file
                 (io/resource path) (io/resource path)
                 :else nil)]
    (when-not source
      (throw (ex-info "Classifier program was not found."
                      {:program path :base-dir base-dir})))
    (-> source slurp edn/read-string)))

(defn -main
  [& args]
  (try
    (let [{:keys [help? program classifier base-dir] :as options}
          (parse-args args)]
      (if help?
        (println (usage))
        (let [definition (load-program base-dir program)
              result (runtime/run-classifier!
                      (runtime/default-runtime base-dir)
                      definition
                      classifier
                      options)]
          (prn result))))
    (catch Exception error
      (binding [*out* *err*]
        (println "Classifier run failed:" (.getMessage error))
        (when-let [data (ex-data error)]
          (prn data)))
      (System/exit 1))))
