---
uuid: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
title: "FT-003: Recover valuable spans and arrange them non-destructively"
status: breakdown
type: epic
priority: P0
phase: 3
points: 21
labels: media-workbench, clips, waveform, arrangement, decomposed
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-001-ship-a-daily-driver-library-and-player"]
---

# FT-003: Recover valuable spans and arrange them non-destructively

## Outcome

A globally poor render can retain accepted markers and clips, and those clips can
be auditioned, arranged, exported, and traced back to immutable source audio.

## Non-goals

- General-purpose DAW replacement.
- Destructive source editing.
- Plugin hosting, MIDI, or recording.

## Decomposed into

- FT-003A — marker and clip commands.
- FT-003B — waveform salvage editor.
- FT-003C — arrangement playback and deterministic export.
- FT-003D — complete derivation graph.

Implement the children, never this epic directly.
