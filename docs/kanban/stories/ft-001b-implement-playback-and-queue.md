---
uuid: "ft-001b-implement-playback-resolver-persistent-queue-and-resume"
title: "FT-001B: Implement playback resolver, persistent queue, and resume"
status: incoming
type: story
priority: P0
phase: 1
epic: "ft-001-ship-a-daily-driver-library-and-player"
owner: unassigned
points: 5
labels: player, queue, playback
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-001a-index-playable-media-metadata-and-waveform-jobs"]
---

# FT-001B: Implement playback resolver, persistent queue, and resume

## Outcome

Render playables use the FT-000D-selected JVM audio backend to play continuously
with recoverable session state and explicit failure isolation.

## Scope

- Versioned playable-resolution query.
- Queue append, play-next, remove, reorder, previous/next.
- Session resume and current position.
- Missing/unreadable item skip behavior.
- Play, pause, seek, loop, end-of-item advance, and duration reporting.
- Adapter boundary prepared for clip and arrangement playables.

## Non-goals

- Selecting a different audio stack inside this card.
- Waveform editor.
- System media keys before the native-shell adapter exists.
- Publication playback.

## Acceptance criteria

- Queue state survives application restart.
- One unreadable item produces an explicit error and does not destroy the queue.
- Playback progress does not append durable Git events on every tick.
- Transport state is independent of the current view.
- The resolver can add clip/arrangement support without changing queue identity.
- At least one representative corpus MP3 plays, pauses, resumes, seeks, reports
  duration, reaches end-of-item advance, and survives a queue containing an
  unreadable item on the actual native backend.

## Verification

Deterministic fake-media tests cover queue logic, restart, reorder, and failures.
A separately recorded local integration test exercises a real corpus MP3 and the
selected JVM playback backend. The card cannot pass with fake sources alone.
