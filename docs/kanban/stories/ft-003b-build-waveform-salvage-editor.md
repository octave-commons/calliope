---
uuid: "ft-003b-build-waveform-salvage-editor"
title: "FT-003B: Build waveform salvage editor"
status: incoming
type: story
priority: P0
phase: 3
epic: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
owner: unassigned
points: 5
labels: waveform, clips, editor, native-ui
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-001c-build-persistent-player-shell-and-library-browser", "ft-003a-implement-marker-and-clip-commands-over-immutable-renders", "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"]
---

# FT-003B: Build waveform salvage editor

## Outcome

Err can visually find, loop, mark, trim, fade, name, and audition useful spans of
a render in the native application without modifying source media.

## Scope

- Zoomable waveform and overview.
- Playhead, loop, in/out selection, and marker lanes.
- Clip creation and revision.
- Fade/gain controls within the v1 transform law.
- Keyboard and textual time editing.
- Manual sibling-render comparison with independently controlled playheads.
- Boundary audition with short pre/post roll.

## Non-goals

- Full multitrack DAW.
- Plugins, MIDI, recording, or spectral repair.
- Automatic cross-render alignment.
- Claiming semantic section synchronization from title or lyric similarity.

## Acceptance criteria

- Pointer and keyboard workflows produce equivalent application commands.
- Source, range, and status remain visible while editing.
- User, deterministic-analysis, and model marker lanes are distinguishable.
- Boundary audition supports loop and pre/post roll.
- Time ranges can be edited without precision pointer gestures.
- Sibling renders may be opened side by side and auditioned independently.
- The UI does not display an automatic alignment offset unless a future accepted
  model provides evidence and uncertainty.

## Verification

Interaction tests cover zoom-independent range identity, in/out commands, clip
audition, marker lanes, keyboard operation, invalid ranges, and independent
sibling playheads. A native smoke test verifies waveform interaction while the
selected audio backend is active.
