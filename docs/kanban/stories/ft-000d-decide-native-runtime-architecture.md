---
uuid: "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"
title: "FT-000D: Decide native desktop, playback, read-model, and application topology"
status: ready
type: story
priority: P0
phase: 0
epic: "c75d00eb-7cc8-4938-a088-7f2acefb0f4f"
owner: unassigned
points: 5
labels: architecture, native-ui, playback, read-model
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["32325ea0-6c36-4af7-9711-df9cace6cb36"]
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
