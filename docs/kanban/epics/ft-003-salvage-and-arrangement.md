---
category: "epics"
labels: "media-workbench, clips, waveform, arrangement, decomposed"
dependency: ["ft-001-ship-a-daily-driver-library-and-player", "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"]
process: "docs/process/product-design-and-delivery.md"
phase: "3"
type: "epic"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714741814-0.gm741bvoiqm68492d7n"
points: "21"
title: "FT-003: Recover valuable spans and arrange them non-destructively"
priority: "P0"
status: "blocked"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
---

# FT-003: Recover valuable spans and arrange them non-destructively

## Outcome

A globally poor render can retain accepted markers and clips, and those clips can
be auditioned, arranged, exported, and traced back to immutable source audio.

## Scope

- Implement durable marker and clip commands over immutable renders.
- Implement the salvage editor and boundary-audition workflow on the native
  UI/runtime and topology FT-000D selects; this epic implements that decision
  and does not choose a UI/runtime independently.
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
- The salvage editor implements FT-000D's accepted native UI/runtime and
  topology decision rather than a UI/runtime chosen within this epic.
- Source renders remain immutable while clips and arrangements remain reversible.
- Every derivative retains typed provenance to its source render and ranges.
- Arrangement playback and export are deterministic for a fixed decision list.
- V1 remains bounded away from full DAW, recording, plugin, and spectral-repair scope.

## Verification

Read this epic and its four children through Rheos and confirm each resolves with
this epic's UUID in its `epic` field, and that each child's `dependency` array in
the returned card matches the expected edge below:

```bash
eta-mu kanban find ft-003-recover-valuable-spans-and-arrange-them-non-destructively
eta-mu kanban find ft-003a-implement-marker-and-clip-commands-over-immutable-renders
eta-mu kanban find ft-003b-build-waveform-salvage-editor
eta-mu kanban find ft-003c-implement-arrangement-playback-and-deterministic-export
eta-mu kanban find ft-003d-preserve-render-to-release-derivation-graph
eta-mu kanban list
```

Expected child dependency edges (verified against each `find` result's
`dependency` field, not just resolution and `epic` linkage):

- FT-003A depends on FT-000B, FT-000C, FT-001B.
- FT-003B depends on FT-001C, FT-003A, FT-000D.
- FT-003C depends on FT-003A, FT-003B.
- FT-003D depends on FT-003C.

Child contract, interaction, export, and derivation-graph tests verify the
executable work. Real native waveform and audio evidence is recorded by the owning
child cards.

---
Dependency analysis (2026-08-02): depends on FT-001 (player epic) and FT-000D. Blocks nothing active. Children: FT-003A (icebox), FT-003B, FT-003C, FT-003D (icebox). Moved breakdown -> blocked: waiting on FT-001 and FT-000D.
---