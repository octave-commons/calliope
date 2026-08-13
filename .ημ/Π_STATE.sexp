;; Π_STATE.sexp — deterministic handoff snapshot
;; Branch: design/classifier-dsl-v1

(handoff
 (branch "design/classifier-dsl-v1")
 (parent-tag "Π-20260726T235322Z")
 (files-changed 9)
 (tests-passed true)
 (test-count 18)
 (assertion-count 79)
 (verification "clojure -M:test — 0 failures, 0 errors")

 (owned-paths
  "AGENTS.md"
  "docs/classifier-dsl.md"
  "resources/classifiers/theme-discovery-v1.edn"
  "src/calliope/classifier/main.clj"
  "src/calliope/classifier/runtime.clj"
  "src/calliope/law/classifier.cljc"
  "test/calliope/classifier/dsl_test.clj"
  "test/calliope/classifier/runtime_test.clj"
  "ledgers/classification.edn")

 (concurrent-dirt none)
 (blocked none)

 (summary
  "Classifier DSL v1: extended runtime with feature-extraction pipeline, theme-discovery schema updates, and classification ledger. All 18 tests pass."))
