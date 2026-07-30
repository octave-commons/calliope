---
uuid: "ft-003a-implement-marker-and-clip-commands-over-immutable-renders"
title: "FT-003A: Implement marker subjects and clip commands"
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

# FT-003A: Implement marker subjects and clip commands

## Outcome

A render or arrangement can retain point/range annotations, and a render can retain
accepted playable clips, without modifying or copying source audio or arrangement
history.

## Scope

- Marker create/revise/reject/accept commands targeting a versioned render or
  arrangement subject.
- Clip create/revise/supersede commands over immutable render ranges.
- Subject identity, source hash, arrangement version, and millisecond range
  validation against the resolved subject duration.
- Fade/gain metadata within bounded v1 transforms.
- Queue resolution for clips.

## Non-goals

- Waveform or arrangement-lane UI.
- Arrangement export.
- Destructive audio processing.

## Acceptance criteria

- Clip creation fails if the source hash changed or the range exceeds duration.
- Marker creation fails when the render/arrangement subject or version is missing,
  or when its point/range exceeds the resolved subject duration.
- A marker on an arrangement remains attached to the referenced arrangement
  version; revising the arrangement does not silently retarget historical markers.
- A rejected render may still own accepted clips.
- Derived/model markers remain distinct from user markers on either subject type.
- Revising a clip preserves prior versions and source lineage.
- The playback resolver can audition a clip directly.

## Verification

Command and contract tests cover render and arrangement marker subjects, point and
range boundaries, missing/stale subject versions, changed render hashes, clip
overlap and supersession, marker status, and clip playback resolution.
