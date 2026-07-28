---
uuid: "ft-003a-implement-marker-and-clip-commands-over-immutable-renders"
title: "FT-003A: Implement marker and clip commands over immutable renders"
status: incoming
type: story
priority: P0
phase: 3
epic: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
owner: unassigned
points: 5
labels: clips, markers, non-destructive-editing
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000b-define-media-workbench-domain-laws", "ft-000c-define-append-only-studio-events-and-read-projection", "ft-001b-implement-playback-resolver-persistent-queue-and-resume"]
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
