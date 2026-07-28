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

Render playables can be queued and played continuously with recoverable session
state and clear failure isolation.

## Scope

- Versioned playable-resolution query.
- Queue append, play-next, remove, reorder, previous/next.
- Session resume and current position.
- Missing/unreadable item skip behavior.
- Loop and seek commands.

## Non-goals

- Waveform editor.
- System media keys before a desktop-shell adapter exists.
- Publication playback.

## Acceptance criteria

- Queue state survives application restart.
- One unreadable item yields an explicit error and does not destroy the queue.
- Playback progress does not append durable Git events on every tick.
- Transport state is independent of the current UI route.
- The resolver is prepared to accept clip/arrangement playables once their laws
  land without changing queue identity.

## Verification

Playback/queue tests use deterministic fake media sources and cover restart,
reordering, seeking, end-of-item advance, and unreadable-item isolation.
