---
id: "22d92aa9-61d2-45e6-bb2a-a9c6c6518fc3"
title: "FT-003A: Implement marker and clip commands over immutable renders"
status: incoming
type: story
priority: P0
phase: 3
epic: "dfad6caa-f439-4d41-95c4-18ed8807be6d"
owner: unassigned
points: 5
labels: [clips, markers, non-destructive-editing]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["9c66aedc-71c3-4d82-b222-d338daf3d99c", "d7e1dc7e-cf4d-4b77-8714-69ce092e1c2d", "63a5b66e-a80b-414b-9522-f3972cc6a4dd"]
---

# FT-003A: Implement marker and clip commands over immutable renders

## Outcome

A render can retain point/range annotations and accepted playable clips without
modifying or copying the source audio.

## Scope

- Marker create/revise/reject/accept commands.
- Clip create/revise/supersede commands.
- Source hash and millisecond range validation.
- Fade/gain metadata within bounded v1 transforms.
- Queue resolution for clips.

## Non-goals

- Waveform UI.
- Arrangement export.
- Destructive audio processing.

## Acceptance criteria

- Clip creation fails if the source hash changed or the range exceeds duration.
- A rejected render may still own accepted clips.
- Derived/model markers remain distinct from user markers.
- Revising a clip preserves prior versions and source lineage.
- The playback resolver can audition a clip directly.

## Verification

Tests cover boundary ranges, changed hashes, overlap, supersession, marker status,
and clip playback resolution.