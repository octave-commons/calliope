;; Π_STATE.sexp — deterministic handoff snapshot
;; Branch: design/media-workbench-v1

(handoff
 (branch "design/media-workbench-v1")
 (parent-tag "Π-20260728T012734Z")
 (files-changed 33)
 (tests-passed true)
 (test-count 30)
 (assertion-count 193)
 (verification "clojure -M:test — 30 tests, 193 assertions, 0 failures, 0 errors; eta-mu kanban count — 31 tasks, board parses")

 (owned-paths
  "AGENTS.md"
  "CLAUDE.md"
  "docs/adrs/adr-002-native-runtime-architecture.md"
  "docs/research/suno-metadata-index-and-search.md"
  "docs/kanban/.events/ledger.edn"
  "docs/kanban/chores/ft-ops-001-generate-rheos-board-snapshot.md"
  "docs/kanban/chores/ft-ops-002-review-authority-statuses.md"
  "docs/kanban/chores/ft-ops-003-document-first-review-disposition.md"
  "docs/kanban/epics/ft-000-design-authority-and-studio-foundation.md"
  "docs/kanban/epics/ft-001-daily-driver-player.md"
  "docs/kanban/epics/ft-002-focused-curation.md"
  "docs/kanban/epics/ft-003-salvage-and-arrangement.md"
  "docs/kanban/stories/*.md (16 modified + new ft-005a-renderer-metadata-search-and-classification.md)"
  "ledgers/ingest.edn"
  "ledgers/projections/suno-meta-v1.edn"
  "scripts/suno_meta.clj"
  "spike/ft-000d/{deps.edn,README.md,src/audio_spike.clj,src/window_spike.clj,src/read_model_spike.clj}"
  "src/fork_tales/law/studio.cljc"
  "test/fork_tales/law/studio_test.clj"
  "test/fork_tales/test_runner.clj"
  "receipts.edn")

 (concurrent-dirt none)

 (blocked
  "FT-000C remains blocked on Err's ADR-002 acceptance disposition (card in review)")

 (generated-untracked
  "spike/ft-000d/.clj-kondo/ and spike/ft-000d/.cpcache/ left untracked by intent")

 (summary
  "Gate 0 walk: FT-000A done on recorded human acceptance; FT-000B done with src/fork_tales/law/studio.cljc closed-map Malli contracts + negative tests; FT-000D in review with three passing spikes (JavaFX Media transport incl. seek/pause/resume over corpus MP3; cljfx native window through in-process boundary; SQLite read model deterministically rebuilt) and proposed ADR-002. Pass 4: scripts/suno_meta.clj ingests Suno renderer metadata idempotently into :suno/* ledger events and ledgers/projections/suno-meta-v1.edn (825 clips, 107 liked); research doc recommends embedded Lucene at the :anchor-neighbors seam; card FT-005A created incoming. AGENTS.md consolidated: Rheos board discipline moved to docs/kanban-docs/AGENTS.md, license to PROCESS.md (verified relocation, not loss)."))
