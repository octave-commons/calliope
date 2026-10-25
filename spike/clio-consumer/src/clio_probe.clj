(ns clio-probe
  "Two-process consumer discovery through Clio's published JVM boundary."
  (:require [clio.extern.jvm.fs :as fs]
            [clio.infra.runtime :as runtime]
            [clio.infra.ledger :as ledger]
            [clio.law.schema :as schema]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def source-hash
  "211cce48219584744f7f9b23768e55f7ca7dace4d7d52fd28b477049b86eac0e")

(def catalog
  {:studio.discovery/source-observed
   (schema/event-schema
    :studio.discovery/source-observed
    [:map {:closed true} [:source/sha256 [:string {:min 64 :max 64}]]])})

(defn ensure! [condition message]
  (when-not condition (throw (ex-info message {}))))

(defn -main [& [mode directory]]
  (ensure! (and (#{"append" "recover"} mode) (seq directory))
           "Usage: clio-probe append|recover /absolute/scratch/state")
  (let [root (.getCanonicalPath (io/file directory))
        ledger-path (str root "/events.edn")
        candidate-path (str root "/candidate.edn")
        rt (runtime/open (str root "/schemas") catalog)]
    (case mode
      "append"
      (do
        (ledger/create-ledger! ledger-path)
        (let [result
              (runtime/append!
               rt ledger-path :studio.discovery/source-observed
               {:event/stream "studio-discovery" :event/seq 1
                :event/actor "local:discovery" :event/subject "source:discovery"
                :event/data {:source/sha256 source-hash}})]
          (fs/write-text! candidate-path
                          (pr-str {:ledger/path ledger-path :event (:event result)}))
          (ensure! (= :appended (:append/result result)) "Append was not durable")
          (println "APPEND JVM: durable Clio event" (get-in result [:event :event/id]))))

      "recover"
      (let [candidate (edn/read-string (fs/read-text candidate-path))
            before (runtime/canonicalize-files rt [ledger-path])
            retry-result (runtime/retry-append! rt candidate)
            _ (runtime/ensure-durable! rt ledger-path)
            after (runtime/canonicalize-files rt [ledger-path])]
        (ensure! (= :already-present (:append/result retry-result))
                 "Exact retry did not deduplicate")
        (ensure! (= before after) "Retry changed canonical history")
        (ensure! (= [(:event candidate)] (:canonical/events after))
                 "Second JVM did not recover the exact first-JVM event")
        (ensure! (= source-hash
                    (get-in after [:canonical/events 0 :event/data :source/sha256]))
                 "Source-hash metadata changed")
        (println "RECOVERY JVM: same event identity/source hash; durable retry deduped"
                 (:canonical/event-ids after)))))
  (shutdown-agents))
