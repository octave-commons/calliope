---
uuid: "cs-007-restart-demonstration"
title: "CS-007: Demonstrate the full native studio loop and restart recovery"
status: incoming
priority: P0
points: 3
labels: studio, foundation, native, recovery
dependency: ["cs-004-native-studio-loop", "cs-006-desktop-profile"]
research: docs/research/studio-foundation-map.md
adr: docs/adrs/adr-001-local-first-media-workbench.md
design: docs/designs/studio-session-boundary.md
---

# CS-007: Demonstrate the full native studio loop and restart recovery

## Context

The [studio boundary](../../designs/studio-session-boundary.md) specifies the
assignment's complete synthetic session and the accepted domain ownership.

## Outcome

Demonstrate the full native studio loop and restart recovery as one bounded part of the complete studio milestone.

## Scope

Produce a repeatable no-new-peripherals demonstration through the real native application boundary: open profile, explicitly arm, ingest deterministic audio/MIDI, mark, stop, replay/select/annotate, review a provisional suggestion, decide separately, terminate and restart. Preserve sources and unfinished state; record exact verification limits.

## Non-goals

A DAW replacement, new shared event laws, changed story/canon or simulation,
hardware procurement, publication, or a new persistent agent runtime.

## Acceptance criteria

- The scripted native journey performs every control/state transition in studio-session-boundary.md; actual JavaFX playback progresses over the captured fixture and selected phrase.
- Independent JVM restart retrieves the same phrase UUID, source and any materialized derivative hashes, event identities, selected range, annotations, proposal IDs and attributed accept/reject records. A non-materialized clip instead retains its render hash and range; never invent a derivative file hash.
- The reopened session is UNARMED and no input device opens. Pending local provider work is interrupted/unknown until explicitly retried.
- UI interaction evidence at 640x480 and 960x720 records keyboard reachability/activation, accessible names, visible capture/transport state and scroll-reachable overflow.
- Deliver a runnable guide, source/evidence identities, exact commands/results and remaining real-device, direct-monitor, latency, i3 and durable-agent integration work. Synthetic fixtures, real model inference and manually observed behavior are distinct.

## Verification

Write failing contract/behavior tests before production implementation, then
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Record
commands, exact identities, real versus simulated effects and limits in receipts.
Planning review precedes ready; current-head review/checks precede merge.

## Risks

Keep missing sources, failed writes, unavailable providers and partial effects
explicit. Device and durable-runtime evidence cannot be inferred from fixtures.

