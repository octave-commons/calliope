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
labels: waveform, clips, editor, ui
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-001c-build-persistent-player-shell-and-library-browser", "ft-003a-implement-marker-and-clip-commands-over-immutable-renders"]
---

# FT-003B: Build waveform salvage editor

## Outcome

Err can visually find, loop, mark, trim, fade, name, and audition useful spans of
a render without leaving the application or modifying source media.

## Scope

- Zoomable waveform and overview.
- Playhead, loop, in/out selection, marker lanes.
- Clip creation and revision.
- Fade/gain controls within the v1 transform law.
- Keyboard and text-time editing.
- Sibling-render comparison entry point.

## Non-goals

- Full multitrack DAW.
- Plugins, MIDI, recording, or spectral repair.
- Automatic cross-render alignment.

## Acceptance criteria

- Pointer and keyboard workflows produce the same commands.
- Source, range, and status remain visible while editing.
- User, deterministic-analysis, and model marker lanes are distinguishable.
- Boundary audition supports loop and short pre/post roll.
- Time ranges can be edited without precision pointer gestures.

## Verification

Interaction tests cover zoom-independent range identity, in/out commands, clip
audition, marker lanes, keyboard operation, and invalid range feedback.
