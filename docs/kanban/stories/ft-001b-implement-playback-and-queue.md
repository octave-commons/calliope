---
category: "stories"
labels: "player, queue, playback"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-001a-index-playable-media-metadata-and-waveform-jobs"]
process: "docs/process/product-design-and-delivery.md"
phase: "1"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714739814-0.xfoujzf9g3refz8ghc"
points: "5"
title: "FT-001B: Implement playback resolver, persistent queue, and resume"
priority: "P0"
status: "blocked"
epic: "ft-001-ship-a-daily-driver-library-and-player"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-001b-implement-playback-resolver-persistent-queue-and-resume"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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

---
Dependency analysis (2026-08-02): depends on FT-000D (breakdown) and FT-001A (accepted — also unresolved). Blocks FT-001C. Moved breakdown -> blocked: waiting on FT-000D and FT-001A.
---