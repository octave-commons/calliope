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
dependency: ["ft-001d-add-dispositions-ratings-labels-sorting-and-playlists", "ft-003a-implement-marker-and-clip-commands-over-immutable-renders", "ft-003b-build-waveform-salvage-editor"]
---

# FT-003C: Implement arrangement playback and deterministic export

## Outcome

Accepted clips can be dragged or keyboard-added into a native arrangement lane,
ordered into a playable arrangement, and rendered into a content-addressed export
that remains a resolvable playable for queues and playlists.

## Scope

- Arrangement edit commands and ordered timeline.
- A native arrangement-lane view for adding, removing, and reordering accepted
  clips with pointer and keyboard interaction.
- Arrangement-marker interactions that dispatch FT-003A marker commands against
  the current versioned arrangement subject.
- Gaps, bounded overlaps/crossfades, clip gain, and fades.
- Arrangement playback resolution without first exporting.
- Background deterministic audio export.
- Successful-export registration in the media index and playable resolver as an
  `:export` reference backed by its output hash and declared path.
- Queue insertion and playlist references through the same playable identity used
  by renders, clips, and arrangements.
- Export job progress, failure, retry, output hash, and missing-output behavior.

## Non-goals

- Arbitrary effects/plugins.
- Remote publication.
- Automatic mastering claims.

## Acceptance criteria

- Accepted clips can be dragged from the salvage workflow into the arrangement
  lane and added, removed, or reordered without mutating source media.
- Pointer and keyboard arrangement interactions dispatch the same application
  commands; the view does not maintain a second UI-only arrangement state.
- Point/range markers created from the arrangement view remain attached to the
  referenced arrangement version through FT-003A's marker commands.
- Arrangement playback and export consume the same versioned edit decision list.
- Re-running the same export inputs/settings produces the same declared identity.
- A successful export is indexed and resolves by its `:export` playable reference
  to the content-addressed output; it can enter a queue and be saved in a playlist
  without copying media bytes or changing identity.
- A missing or unreadable exported file produces an explicit resolver error and
  follows the queue's existing failure-isolation behavior.
- Source hash drift fails before rendering.
- Failed exports do not create successful export or playable-index records.
- Export settings and tool versions remain inspectable.

## Verification

Interaction tests cover pointer and keyboard add/remove/reorder and arrangement-
marker behavior, asserting the emitted arrangement and FT-003A marker commands.
Golden-fixture and integration tests cover timeline resolution, fades/gaps,
deterministic job identity, source drift, failed rendering, successful output
hashing and index registration, `:export` resolver lookup, queue insertion,
playlist reference stability, and missing/unreadable exported files.
