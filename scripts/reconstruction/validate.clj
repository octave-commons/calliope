#!/usr/bin/env bb
(ns reconstruction.validate
  "CLI adapter for the μ1-μ6 handoff invariants.

  All validation logic lives in fork-tales.reconstruction.handoff, which is pure
  and JVM-testable. This namespace only does IO: read packets, call the core,
  write the report, append a :handoff/validated ledger event.

  Usage:
    bb scripts/reconstruction/validate.clj PACKET... [--catalog c.json]
                                                     [--out-json report.json]
                                                     [--case-id ID] [--no-ledger]

  Accepts JSON or EDN packets. Exits 1 on any error, matching the retired tool."
  (:require [babashka.cli :as cli]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [fork-tales.reconstruction.handoff :as handoff]
            [reconstruction.ledger :as ledger]))

(defn read-data
  "EDN when the path ends .edn, otherwise JSON."
  [path]
  (let [s (slurp (io/file path))]
    (if (str/ends-with? (str path) ".edn")
      (edn/read-string {:readers {}} s)
      (json/parse-string s))))

(defn- named-packets
  "Expand each file into [display-name packet] pairs; a top-level array becomes
  one entry per element, matching the Python tool's indexing."
  [paths]
  (mapcat (fn [path]
            (let [data (read-data path)
                  ps (if (sequential? data) data [data])
                  nm (.getName (io/file path))]
              (map-indexed (fn [i p] [(str nm "[" i "]") p]) ps)))
          paths))

(defn run [{:keys [packets catalog out-json case-id no-ledger]}]
  (let [schema (read-data (io/file (ledger/repo-root)
                                   "resources" "reconstruction" "handoff-schemas.json"))
        approved (handoff/approved-ids (when catalog (read-data catalog)))
        report (handoff/report (named-packets packets) schema approved)]
    (when out-json
      (io/make-parents out-json)
      (spit out-json (str (json/generate-string report {:pretty true}) "\n")))
    (when-not no-ledger
      (ledger/append-event!
       (merge (ledger/base-event {:type :handoff/validated
                                  :tier :derived
                                  :run-id (ledger/uuid)
                                  :case-id (or case-id "unscoped")})
              {:packets (mapv #(ledger/artifact-ref % :handoff-packet) packets)
               :checked-specs (:checked_specs report)
               :ok? (:ok report)
               :error-count (:error_count report)
               :warning-count (:warning_count report)
               :errors (:errors report)})))
    (println (json/generate-string report {:pretty true}))
    report))

(def cli-spec
  {:catalog   {:desc "Approved reference catalog (JSON or EDN)"}
   :out-json  {:desc "Write the report to this path"}
   :case-id   {:desc "Case identity recorded on the ledger event"}
   :no-ledger {:coerce :boolean :desc "Skip appending a ledger event"}})

(defn -main [& args]
  (let [{:keys [args opts]} (cli/parse-args args {:spec cli-spec})]
    (when (empty? args)
      (println "usage: validate.clj PACKET... [--catalog c] [--out-json r] [--case-id ID] [--no-ledger]")
      (System/exit 2))
    (System/exit (if (:ok (run (assoc opts :packets args))) 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
