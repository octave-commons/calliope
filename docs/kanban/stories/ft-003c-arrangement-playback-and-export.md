---
id: "b539c602-c635-4735-aa9c-500c63f900d3"
title: "FT-003C: Implement arrangement playback and deterministic export"
status: incoming
type: story
priority: P0
phase: 3
epic: "dfad6caa-f439-4d41-95c4-18ed8807be6d"
owner: unassigned
points: 5
labels: [arrangement, export, playback]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["22d92aa9-61d2-45e6-bb2a-a9c6c6518fc3", "24607f2f-2d5b-4c59-aa2b-ac902b581547"]
---

# FT-003C: Implement arrangement playback and deterministic export

## Outcome

Accepted clips can be ordered into a playable arrangement and rendered into a
content-addressed derivative with complete settings and source identity.

## Scope

- Arrangement edit commands and ordered timeline.
- Gaps, bounded overlaps/crossfades, clip gain, and fades.
- Playback resolution without first exporting.
- Background deterministic audio export.
- Export job progress, failure, retry, and output hash.

## Non-goals

- Arbitrary effects/plugins.
- Remote publication.
- Automatic mastering claims.

## Acceptance criteria

- Arrangement playback and export consume the same versioned edit decision list.
- Re-running the same export inputs/settings produces the same declared identity.
- Source hash drift fails before rendering.
- Failed exports do not create successful export records.
- Export settings and tool versions remain inspectable.

## Verification

Golden-fixture tests cover timeline resolution, fades/gaps, deterministic job
identity, source drift, failed rendering, and successful output hashing.