#!/usr/bin/env bb
(ns reconstruction.preflight
  "Check that the paths inside a preserved evidence artifact still resolve.

  Run this before any grader. A grader handed unreachable evidence does not fail
  — it scores those features null and reports lower coverage, so a broken input
  is indistinguishable from a weak candidate. This lane turns that into an
  explicit, non-zero exit and an :evidence/preflighted ledger event.

  Usage:
    bb scripts/reconstruction/preflight.clj EVIDENCE... [--case-id ID]
                                                        [--rewrite-to DIR]
                                                        [--no-ledger] [--quiet]

  --rewrite-to writes a path-translated copy into DIR for re-running a grader.
  Committed artifacts are never modified: they are append-only history, and
  correcting evidence after the fact would destroy the provenance it holds.

  Exit 0 when every path resolves (directly or by a declared rule), 1 otherwise."
  (:require [babashka.cli :as cli]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [fork-tales.reconstruction.paths :as paths]
            [reconstruction.ledger :as ledger]))

(def rules-path "resources/reconstruction/path-roots.edn")

(defn load-rules []
  (-> (io/file (ledger/repo-root) rules-path) slurp
      (as-> s (edn/read-string {:readers {}} s)) :rules))

(defn read-data [path]
  (let [s (slurp (io/file path))]
    (if (str/ends-with? (str path) ".edn")
      (edn/read-string {:readers {}} s)
      (json/parse-string s))))

(defn- fmt [{:keys [ok? total counts missing translated shadowed]} label]
  (str "  " label "\n"
       "    total=" total " " (pr-str counts) (when-not ok? "  <-- MISSING PATHS")
       (when (seq translated)
         (str "\n    translated (" (count translated) "):"
              (str/join (map (fn [[f t]] (str "\n      " f "\n        -> " t)) (take 3 translated)))
              (when (> (count translated) 3) (str "\n      … " (- (count translated) 3) " more"))))
       (when (seq shadowed)
         (str "\n    SHADOWED — resolves as written but a rule also matches; may read a stale copy ("
              (count shadowed) ")"))
       (when (seq missing)
         (str "\n    missing (" (count missing) "):"
              (str/join (map #(str "\n      " %) (take 5 missing)))
              (when (> (count missing) 5) (str "\n      … " (- (count missing) 5) " more"))))))

(defn run [{:keys [evidence case-id rewrite-to no-ledger quiet]}]
  (let [rules (load-rules)
        exists? #(.exists (io/file %))
        results
        (mapv
         (fn [path]
           (let [data (read-data path)
                 rep (paths/report {:rules rules :exists? exists? :data data})]
             (when rewrite-to
               (let [out (io/file rewrite-to (.getName (io/file path)))]
                 (io/make-parents out)
                 (spit out (json/generate-string (paths/rewrite rules data) {:pretty true}))))
             (when-not quiet (println (fmt rep (.getName (io/file path)))))
             (when-not no-ledger
               (ledger/append-event!
                (merge (ledger/base-event {:type :evidence/preflighted
                                           :tier :observed
                                           :run-id (ledger/uuid)
                                           :case-id (or case-id "unscoped")})
                       {:artifact (ledger/artifact-ref path :evidence)
                        :ok? (:ok? rep)
                        :total (:total rep)
                        :counts (:counts rep)
                        :missing (:missing rep)
                        :translated (:translated rep)
                        :shadowed (:shadowed rep)
                        :rules-source rules-path})))
             rep))
         evidence)
        ok (every? :ok? results)]
    (when-not quiet
      (println (str "\n  " (if ok "OK — every referenced path resolves"
                              "FAIL — unresolvable references; do not treat grader output as data"))))
    {:ok ok :results results}))

(def cli-spec
  {:case-id    {:desc "Case identity recorded on the ledger event"}
   :rewrite-to {:desc "Write path-translated scratch copies into this directory"}
   :no-ledger  {:coerce :boolean :desc "Skip appending ledger events"}
   :quiet      {:coerce :boolean :desc "Suppress the human-readable summary"}})

(defn -main [& args]
  (let [{:keys [args opts]} (cli/parse-args args {:spec cli-spec})]
    (when (empty? args)
      (println "usage: preflight.clj EVIDENCE... [--case-id ID] [--rewrite-to DIR] [--no-ledger] [--quiet]")
      (System/exit 2))
    (System/exit (if (:ok (run (assoc opts :evidence args))) 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
