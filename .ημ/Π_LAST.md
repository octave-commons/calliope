# Π Handoff Snapshot

- **Branch**: `design/media-workbench-v1`
- **Parent**: `Π-20260728T012734Z`
- **Date**: 2026-08-25
- **Tests**: 30 tests, 193 assertions, 0 failures
- **Board**: 31 tasks parse via Rheos (icebox 12, accepted 5, blocked 10, in_progress 1, review 1, done 2)

## Changes

| Area | Summary |
|------|---------|
| `src/fork_tales/law/studio.cljc` | FT-000B: closed-map versioned Malli contracts — work/render/clip/arrangement/export distinct, scoped ratings, distinct playlists/smart-lists/workspaces, per-target publication state |
| `test/fork_tales/law/studio_test.clj` + `test_runner.clj` | 7 new deftests with negative fixtures for cross-scope promotion and malformed ranges |
| `docs/adrs/adr-002-native-runtime-architecture.md` | FT-000D proposal: cljfx/JavaFX 21 UI, JavaFX Media playback, SQLite read model, in-process boundary; deciders [Err], acceptance pending |
| `spike/ft-000d/` | Three reproducible spikes, all PASS: audio transport (187.8s corpus MP3, seek held while paused), native window through boundary, SQLite rebuild-from-EDN |
| `scripts/suno_meta.clj` | Pass 4: Suno metadata → `:suno/*` events in `ledgers/ingest.edn`, idempotent |
| `ledgers/projections/suno-meta-v1.edn` | Projection: 825 clips, 107 liked, 909 tag tokens |
| `docs/research/suno-metadata-index-and-search.md` | Recommends embedded Lucene feeding the reserved `:anchor-neighbors` seam |
| Kanban cards + `.events/ledger.edn` | Gate 0 walked through Rheos: FT-000A/FT-000B done, FT-000D review, FT-000C blocked on ADR-002 disposition |
| `CLAUDE.md` | Claude Code harness onboarding doc |
| `AGENTS.md` | Consolidation: board discipline → `docs/kanban-docs/AGENTS.md`, ledger/license → `PROCESS.md` |

## Verification

```
$ clojure -M:test
Ran 30 tests containing 193 assertions.
0 failures, 0 errors.

$ eta-mu kanban count
Total tasks: 31
```

## Blockers / Concurrent Dirt

- **Blocked**: FT-000C waits on Err's ADR-002 acceptance decision.
- **Concurrent dirt**: none observed.
- **Excluded**: `spike/ft-000d/.clj-kondo/`, `spike/ft-000d/.cpcache/` (generated, left untracked).
