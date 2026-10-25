(ns calliope.studio.session-process
  "GPL-3.0-or-later. Actual independent-JVM session acceptance harness."
  (:require [calliope.studio.session :as session]
            [clojure.edn :as edn])
  (:import [java.nio.file Path Files LinkOption]
           [java.util UUID]))

(defn -main [& [mode directory actor-id output command-file barrier]]
  (let [root (Path/of directory (make-array String 0))
        actor {:actor/id actor-id :actor/kind :user}
        store (session/open! root actor)
        initial-view (session/view store)]
    (try
      (let [result (when (= mode "compete")
                     (let [deadline (+ (System/nanoTime) 15000000000)]
                       (spit (str output ".ready") "Opened initial revision\n")
                       (while (not (Files/exists (Path/of barrier (make-array String 0)) (make-array LinkOption 0)))
                         (when (> (System/nanoTime) deadline) (throw (ex-info "Writer barrier timeout" {})))
                         (Thread/sleep 10))
                       (session/ui-command! store actor (edn/read-string (slurp command-file)))))
            _ (session/reload! store)
            restart-ingest (when (= mode "recover")
                             (let [state (:session (session/view store))
                                   take (first (vals (:takes state)))]
                               (session/ui-command!
                                store actor
                                {:command/type :studio/ingest :command/version 1
                                 :request/id (str (UUID/randomUUID)) :session/id (:session/id state)
                                 :session/revision (:session/revision state)
                                 :audio {:filename "must-not-open.wav" :sha256 (get-in take [:take/audio :sha256])}
                                 :midi {:filename "must-not-open.mid" :sha256 (get-in take [:take/midi :sha256])}})))]
        (spit output (pr-str {:command-result result :restart-ingest restart-ingest
                             :opened-revision (get-in initial-view [:session :session/revision])
                             :view (session/view store)
                             :history (session/history store)
                             :tables (into {} (map (fn [q] [q (session/query-rows store q)])
                                                   [:sessions :events :sources :takes]))})))
      (finally (session/close! store) (shutdown-agents)))))
