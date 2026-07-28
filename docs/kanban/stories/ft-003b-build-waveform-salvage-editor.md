---
id: "24607f2f-2d5b-4c59-aa2b-ac902b581547"
title: "FT-003B: Build waveform salvage editor"
status: incoming
type: story
priority: P0
phase: 3
epic: "dfad6caa-f439-4d41-95c4-18ed8807be6d"
owner: unassigned
points: 5
labels: [waveform, clips, editor, ui]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["2cc53668-22a0-4701-91b9-001f2deb9ca6", "22d92aa9-61d2-45e6-bb2a-a9c6c6518fc3"]
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