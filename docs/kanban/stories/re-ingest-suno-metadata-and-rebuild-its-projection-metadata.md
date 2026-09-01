---
uuid: "calliope-issue-11-re-ingest-suno-metadata"
title: "Re-ingest Suno metadata and rebuild its projection"
status: "incoming"
type: "task"
priority: "P1"
points: "5"
labels: "recovery, ingest, projection, suno"
category: "stories"
write-id: "1788047388220-0.ocny14fyeaf13cb7tkv"
created_at: "2026-08-29T23:49:48.220Z"
---

# Re-ingest Suno metadata and rebuild its projection

GitHub issue: https://github.com/octave-commons/calliope/issues/11

## Outcome

Calliope has a current-main, append-only Suno metadata ingestion lane and a
deterministically rebuilt search/read projection. Suno remains an evidence and
media source, never ontology, review, or publication authority.

## Source evidence to preserve

Conflicted PR #7 head `194512b4ce7e9e706f555a4f933449c632bb8fb5`
contains the prior `scripts/suno_meta.clj`, 835 appended ingest events, a large
`suno-meta-v1.edn` projection, research notes, and an FT-005A board-card attempt.
Replay useful behavior through current authority; do not merge the 51 MB ledger
or copy its projection as truth.

## Scope

- Preserve distinct work/song, renderer clip, metadata observation/source hash,
  local asset, user-authored liked/tag state, and derived search fields.
- Define fixture-backed accepted input, malformed input, identity, and dedupe.
- Make repeated observations idempotent while changed source evidence appends.
- Rebuild the projection solely from canonical events and available source assets.
- Make missing/deleted sources and unavailable LFS objects explicit evidence.

## Acceptance criteria

- Re-ingesting the same source observation appends nothing.
- Changed source content or version produces a new event without rewriting history.
- Existing `ledgers/ingest.edn` bytes and order remain unchanged before appends.
- Two clean projection rebuilds have the same hash.
- Events and projections contain no credentials, secrets, local absolute paths,
  or unnecessary personal data.
- Derived search/classification fields cannot promote song identity or acceptance.
- Current `calliope` namespaces and paths are used.
- Unit and integration tests cover normalization, identity, dedupe, malformed
  metadata, epistemic status, double ingestion, and double projection rebuild.
- Governed EDN validation, `clojure -M:test`, zero-warning lint, projection drift,
  and privacy/secret checks pass in hosted CI.
- The historical 825 clips / 107 liked counts are reconciled against accessible
  sources as observations, not forced targets.
- A Receipt River entry pins sources, commands, counts, hashes, unavailable LFS
  evidence, and non-goals.

## Non-goals

Studio editing law, release/publication, provider generation, a finished-song
claim, live Suno API calls, and generated audio commits are out of scope.
