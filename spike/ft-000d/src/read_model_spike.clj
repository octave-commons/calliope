(ns read-model-spike
  "FT-000D spike C: rebuildable SQLite read model over durable EDN sources.

  Builds target/studio/spike-index.db from ledgers/projections/suno-meta-v1.edn,
  answers representative player queries, then DELETES the database and
  rebuilds it to identical results — the projection is disposable; the EDN
  ledger/projection remains truth (ADR-001 section 3).
  Run: clojure -M -m read-model-spike"
  (:require [clojure.edn :as edn]
            [next.jdbc :as jdbc]))

(def db-path "../../target/studio/spike-index.db")

(defn build! []
  (let [projection (edn/read-string
                    (slurp "../../ledgers/projections/suno-meta-v1.edn"))
        ds (jdbc/get-datasource {:dbtype "sqlite" :dbname db-path})]
    (jdbc/execute! ds ["DROP TABLE IF EXISTS clips"])
    (jdbc/execute! ds ["
      CREATE TABLE clips (id TEXT PRIMARY KEY, title TEXT, liked INTEGER,
                          duration_s REAL, model TEXT, tags TEXT, folder TEXT)"])
    (jdbc/execute! ds ["CREATE INDEX idx_clips_liked ON clips(liked)"])
    (doseq [[id c] (:clips projection)]
      (jdbc/execute! ds
                     ["INSERT INTO clips (id, title, liked, duration_s, model, tags, folder)
                       VALUES (?, ?, ?, ?, ?, ?, ?)"
                      id
                      (:clip/title c)
                      (if (:clip/liked c) 1 0)
                      (:clip/duration c)
                      (:clip/model c)
                      (:clip/tags-raw c)
                      (:clip/folder c)]))
    ds))

(defn query! [ds]
  {:clip-count (:count (jdbc/execute-one! ds ["SELECT COUNT(*) AS count FROM clips"]))
   :liked-count (:count (jdbc/execute-one! ds ["SELECT COUNT(*) AS count FROM clips WHERE liked = 1"]))
   :by-model (jdbc/execute! ds ["SELECT model, COUNT(*) AS count FROM clips
                                 WHERE model IS NOT NULL GROUP BY model ORDER BY count DESC"])
   :liked-sample (mapv :clips/title
                       (jdbc/execute! ds ["SELECT title FROM clips WHERE liked = 1 LIMIT 5"]))})

(defn -main [& _]
  (.mkdirs (java.io.File. "../../target/studio"))
  (let [first-results (query! (build!))]
    (println "first build:" first-results)
    (.delete (java.io.File. db-path))
    (println "deleted" db-path "— rebuilding from durable EDN ...")
    (let [second-results (query! (build!))]
      (println "rebuild:" second-results)
      (if (= first-results second-results)
        (println "READ-MODEL SPIKE: PASS (rebuild is deterministic)")
        (do (println "READ-MODEL SPIKE: FAIL (rebuild diverged)")
            (System/exit 1)))))
  (.delete (java.io.File. db-path))
  (println "spike db removed; durable sources untouched")
  (shutdown-agents)
  (System/exit 0))
