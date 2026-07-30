---
uuid: "ft-003c-implement-arrangement-playback-and-deterministic-export"
title: "FT-003C: Implement arrangement playback and deterministic export"
status: incoming
type: story
priority: P0
phase: 3
epic: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
owner: unassigned
points: 5
labels: arrangement, export, playback, native-ui
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-003a-implement-marker-and-clip-commands-over-immutable-renders", "ft-003b-build-waveform-salvage-editor"]
---

# FT-003C: Implement arrangement playback and deterministic export

## Outcome

Accepted clips can be dragged or keyboard-added into a native arrangement lane,
ordered into a playable arrangement, and rendered into a content-addressed
derivative with complete settings and source identity.

## Scope

- Arrangement edit commands and ordered timeline.
- A native arrangement-lane view for adding, removing, and reordering accepted
  clips with pointer and keyboard interaction.
- Gaps, bounded overlaps/crossfades, clip gain, and fades.
- Playback resolution without first exporting.
- Background deterministic audio export.
- Export job progress, failure, retry, and output hash.

## Non-goals

- Arbitrary effects/plugins.
- Remote publication.
- Automatic mastering claims.

## Acceptance criteria

- Accepted clips can be dragged from the salvage workflow into the arrangement
  lane and added, removed, or reordered without mutating source media.
- Pointer and keyboard arrangement interactions dispatch the same application
  commands; the view does not maintain a second UI-only arrangement state.
- Arrangement playback and export consume the same versioned edit decision list.
- Re-running the same export inputs/settings produces the same declared identity.
- Source hash drift fails before rendering.
- Failed exports do not create successful export records.
- Export settings and tool versions remain inspectable.

## Verification

Interaction tests cover pointer and keyboard add/remove/reorder behavior in the
native arrangement lane and assert the emitted arrangement commands. Golden-fixture
tests cover timeline resolution, fades/gaps, deterministic job identity, source
drift, failed rendering, and successful output hashing.
