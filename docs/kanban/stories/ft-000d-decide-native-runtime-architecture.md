---
category: "stories"
labels: "architecture, native-ui, playback, read-model"
dependency: ["ft-000a-review-and-accept-or-revise-media-workbench-authority"]
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1788047455111-0.6sme6jboey7o15dopd7"
points: "5"
title: "FT-000D: Decide native desktop, playback, read-model, and application topology"
priority: "P0"
status: "breakdown"
epic: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
---

# FT-000D: Decide native desktop, playback, read-model, and application topology

## Outcome

Fork Tales has an accepted implementation decision for the first native
Clojure/JVM client, audio backend, rebuildable local read model, and in-process
application-service boundary.

## Scope

- Compare viable Clojure/JVM native UI toolkits; no embedded browser.
- Select an audio decoding/playback backend against representative corpus MP3s.
- Select the first rebuildable query/read-model implementation.
- Define the in-process command/query application boundary used by UI and workers.
- Record packaging, media-key, waveform rendering, and failure-isolation implications.
- Amend or add an ADR with evidence and rejected alternatives.

## Non-goals

- Building the complete player shell.
- Implementing release/publication adapters.
- Selecting every future DSP or analysis library.

## Acceptance criteria

- The selected UI path is native Clojure/JVM and does not require a browser runtime.
- A representative real MP3 plays, seeks, pauses, resumes, and reports duration.
- The read-model choice can be rebuilt from durable sources.
- UI code cannot write ledgers or invoke FFmpeg directly.
- The application boundary works in-process and does not require a local HTTP server.
- Decision evidence names versions, platform limits, and rejected alternatives.

## Verification

A local spike records exact commands and demonstrates real audio playback plus a
minimal native window using the selected stack.

---
Recovery crosswalk (2026-08-30): GitHub issue #12 (https://github.com/octave-commons/calliope/issues/12) preserves the stale PR #7 ADR-002 and disposable-spike evidence for this existing canonical card. Historical acceptance must be reconciled against current authority and rerun on a compatible harness; it is not inferred from the stale head.

Readiness repair (2026-08-30): moved from ready back to breakdown through Rheos because declared dependency FT-000A is still incoming. Re-enter ready only after the dependency is satisfied or explicitly revised through canonical authority.

Date correction: the preceding reconciliation comments say 2026-08-30, but Rheos recorded these operations on 2026-08-29 UTC. The substance is unchanged; this append-only correction preserves the original ledger history.
---