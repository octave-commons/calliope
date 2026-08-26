---
category: "stories"
labels: "architecture, native-ui, playback, read-model"
dependency: ["ft-000a-review-and-accept-or-revise-media-workbench-authority"]
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1787707463632-0.n63ogbqdw6egokhaf9r"
points: "5"
title: "FT-000D: Decide native desktop, playback, read-model, and application topology"
priority: "P0"
status: "done"
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
Dependency analysis (2026-08-02): depends on FT-000A. Blocks FT-000C, FT-001B, FT-001C, FT-003 (epic), FT-003B — the widest fan-out on the board; unblocking this card releases the whole playback and salvage spine. Moved breakdown -> blocked: waiting on FT-000A.

Unblocked 2026-08-02: FT-000A is done (acceptance recorded). Entering ready.

Spike evidence (2026-08-02, all three spikes PASS, exact commands in spike/ft-000d/README.md): (A) JavaFX Media played a real corpus MP3 (aquila-regina/211cce48.mp3, 187.8 s) — duration, play, seek to 30% (56,846 ms), pause with position held, resume, clean dispose. (B) cljfx/JavaFX 21.0.3 native window rendered on Linux; liked-clip listing and transport state flowed only through an in-process command/query boundary; no browser component instantiated, no HTTP server. (C) SQLite read model built from durable EDN (825 clips, 107 liked), deleted, rebuilt to identical results. Decision recorded as ADR-002 (docs/adrs/adr-002-native-runtime-architecture.md): cljfx/JavaFX UI, JavaFX Media playback, SQLite read model via next.jdbc, in-process function boundary. Rejected alternatives recorded (Swing, Skija, embedded browser, JLayer/mp3spi, FFmpeg transport, Datascript-as-read-model, XTDB). Implications recorded: jlink/jpackage packaging (GraalVM native-image NOT assumed), MPRIS media keys as future adapter, headless CI needs Monocle.

Accepted (2026-08-25): all agent-executable scope was complete as of 2026-08-02 (toolkit comparison, verified playback backend, rebuildable read model, in-process boundary, packaging/media-key/waveform/failure-isolation implications); Err accepted ADR-002 on 2026-08-25, completing this card and unblocking FT-000C and the FT-001/FT-003 spine. Not executed: GraalVM native-image evaluation (recorded as risk), MPRIS adapter (FT-001E scope), pro-audio decode adapter (deferred to FT-003 evidence).

ADR-002 accepted by Err on 2026-08-25 (frontmatter accepted date set). Disposition recorded in docs/adrs/adr-002-native-runtime-architecture.md; spike evidence reproducible per ADR acceptance section. Card moved review -> document -> done via Rheos.
---