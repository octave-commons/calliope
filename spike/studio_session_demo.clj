(ns studio-session-demo
  "GPL-3.0-or-later. Reproducible production-service probe, synthetic input only."
  (:require [calliope.studio.session :as session]
            [calliope.studio.synthetic :as synthetic]
            [clojure.edn :as edn])
  (:import [java.nio.file Path]
           [java.util UUID]))

(def actor {:actor/id "simulated-session-demo-operator" :actor/kind :user})
(def session-id "50000000-0000-4000-8000-000000000001")

(defn command [type revision]
  {:command/type type :command/version 1 :request/id (str (UUID/randomUUID))
   :session/id session-id :session/revision revision})

(defn run-command! [store command]
  (let [result (session/ui-command! store actor command)]
    (when-not (= :ok (:result/status result))
      (throw (ex-info "Demonstration command did not succeed" result)))
    result))

(defn snapshot [store]
  {:history (session/history store) :view (session/view store)
   :tables (into {} (map (fn [q] [q (session/query-rows store q)])
                        [:sessions :events :sources :takes]))})

(defn -main [mode root-name evidence-name]
  (let [root (Path/of root-name (make-array String 0))
        store (case mode "create" (session/create! root actor) "recover" (session/open! root actor))]
    (try
      (case mode
        "create"
        (do
          (run-command! store (assoc (command :studio/session-create 0)
                                    :session/title "Synthetic recovery demonstration"
                                    :work/id "studio-foundation-demo"
                                    :session/links ["urn:calliope:studio-foundation:synthetic-demo"]))
          (run-command! store (assoc (command :studio/arm 1)
                                    :capture/clock synthetic/clock
                                    :capture/origin (:origin (synthetic/fixture 42))))
          (run-command! store (merge (command :studio/ingest 2)
                                    (synthetic/write-fixture! (session/staging-root store) 42)))
          (run-command! store (command :studio/stop 3))
          (spit evidence-name (pr-str (snapshot store))))
        "recover"
        (let [before (edn/read-string (slurp evidence-name)) after (snapshot store)
              ingest (some #(let [c (get-in % [:event/data :record/command])]
                              (when (= :studio/ingest (:command/type c)) c)) (:history before))
              refused (session/ui-command! store actor
                                           (assoc ingest :request/id (str (UUID/randomUUID)) :session/revision 4))
              checks {:history-identical? (= (:history before) (:history after))
                      :session-identical? (= (get-in before [:view :session]) (get-in after [:view :session]))
                      :sqlite-rebuilt-identically? (= (:tables before) (:tables after))
                      :restart-unarmed? (= :unarmed (get-in after [:view :capture/state]))
                      :restart-ingest-refused? (= :capture/not-armed (:error/code refused))}]
          (when-not (every? true? (vals checks))
            (throw (ex-info "Recovered session differs" checks)))
          (spit (str evidence-name ".recovered.edn")
                (pr-str {:operator actor :input :synthetic :checks checks
                         :restart-ingest refused :recovered after
                         :limits ["No phrase/proposal/native UI/device or listening demonstration"]}))))
      (finally (session/close! store)))))
