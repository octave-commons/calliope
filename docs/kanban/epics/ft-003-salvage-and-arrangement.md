---
uuid: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
title: "FT-003: Recover valuable spans and arrange them non-destructively"
status: breakdown
type: epic
priority: P0
phase: 3
owner: unassigned
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

## Scope

- Implement durable marker and clip commands over immutable renders.
- Build the native waveform salvage editor and boundary-audition workflow.
- Support arrangement playback and deterministic derivative export.
- Preserve complete render-to-release derivation relationships.

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

## Acceptance criteria

- FT-003A through FT-003D resolve as bounded children with explicit dependencies.
- Source renders remain immutable while clips and arrangements remain reversible.
- Every derivative retains typed provenance to its source render and ranges.
- Arrangement playback and export are deterministic for a fixed decision list.
- V1 remains bounded away from full DAW, recording, plugin, and spectral-repair scope.

## Verification

Read this epic and its four children through Rheos and confirm each resolves with
this epic's UUID in its `epic` field:

```bash
eta-mu kanban find ft-003-recover-valuable-spans-and-arrange-them-non-destructively
eta-mu kanban find ft-003a-implement-marker-and-clip-commands-over-immutable-renders
eta-mu kanban find ft-003b-build-waveform-salvage-editor
eta-mu kanban find ft-003c-implement-arrangement-playback-and-deterministic-export
eta-mu kanban find ft-003d-preserve-render-to-release-derivation-graph
eta-mu kanban list
```

Child contract, interaction, export, and derivation-graph tests verify the
executable work. Real native waveform and audio evidence is recorded by the owning
child cards.
