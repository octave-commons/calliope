---
uuid: "ft-001a-index-playable-media-metadata-and-waveform-jobs"
title: "FT-001A: Index playable media metadata and waveform jobs"
status: incoming
type: story
priority: P0
phase: 1
epic: "ft-001-ship-a-daily-driver-library-and-player"
owner: unassigned
points: 5
labels: player, media-index, waveform
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000b-define-media-workbench-domain-laws", "ft-000c-define-append-only-studio-events-and-read-projection", "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"]
---

# FT-001A: Index playable media metadata and waveform jobs

## Outcome

The application resolves corpus-linked renders into normalized local playback
metadata using the read model selected by FT-000D and schedules rebuildable
waveform-peak generation.

## Scope

- Render path/hash/duration/codec/channel/sample-rate observations.
- Missing, unreadable, and changed-file states.
- Content-addressed waveform peak job identity.
- Read-model rows for render playables.
- Deterministic rebuild from durable corpus and studio events.

## Non-goals

- Choosing a different read-model architecture inside this card.
- Audio transport UI.
- Clip waveforms before clip laws exist.
- Destructive transcoding.

## Acceptance criteria

- The implementation uses the FT-000D-selected read-model adapter.
- Media metadata remains tied to the observed source hash.
- Changed or missing files become explicit states, not stale successful rows.
- Peak files are rebuildable and keyed by source plus algorithm/version.
- The projection enumerates playable renders without scanning the filesystem for
  every library query.
- Deleting the derived index and replaying its inputs recreates equivalent rows.

## Verification

Fixture-backed indexing tests cover valid media, missing media, changed hashes,
deterministic peak-job identity, and read-model rebuild equivalence.
