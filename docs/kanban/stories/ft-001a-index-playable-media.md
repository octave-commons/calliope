---
id: "1f07dfbc-f4f6-448d-937c-86d2c38d4e17"
title: "FT-001A: Index playable media metadata and waveform jobs"
status: incoming
type: story
priority: P0
phase: 1
epic: "033b5cda-ed7e-429b-95ac-a56c1ab7156d"
owner: unassigned
points: 5
labels: [player, media-index, waveform]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["9c66aedc-71c3-4d82-b222-d338daf3d99c", "d7e1dc7e-cf4d-4b77-8714-69ce092e1c2d"]
---

# FT-001A: Index playable media metadata and waveform jobs

## Outcome

The application can resolve corpus-linked renders into normalized local playback
metadata and schedule rebuildable waveform-peak generation.

## Scope

- Render path/hash/duration/codec/channel/sample-rate observations.
- Missing/unreadable/changed-file states.
- Content-addressed waveform peak job identity.
- Read-model rows for render playables.

## Non-goals

- Audio transport UI.
- Clip waveforms before clip laws exist.
- Destructive transcoding.

## Acceptance criteria

- Media metadata remains tied to the observed source hash.
- Changed or missing files become explicit states, not stale successful rows.
- Peak files are rebuildable and keyed by source plus algorithm/version.
- The projection can enumerate playable renders without scanning the filesystem on
  every library query.

## Verification

Fixture-backed indexing tests cover valid media metadata, missing media, changed
hashes, and deterministic peak-job identity.