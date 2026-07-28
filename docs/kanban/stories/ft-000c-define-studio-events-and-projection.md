---
id: "d7e1dc7e-cf4d-4b77-8714-69ce092e1c2d"
title: "FT-000C: Define append-only studio events and read projection"
status: incoming
type: story
priority: P0
phase: 0
epic: "c75d00eb-7cc8-4938-a088-7f2acefb0f4f"
owner: unassigned
points: 5
labels: [event-ledger, projection, media-workbench]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["9c66aedc-71c3-4d82-b222-d338daf3d99c"]
---

# FT-000C: Define append-only studio events and read projection

## Outcome

Durable listening/creative decisions can be appended idempotently and projected
into a responsive local read model without making the projection canonical.

## Scope

- Command/event envelopes and idempotency.
- Rating, label, disposition, playlist, workspace, clip, arrangement, release,
  and publication event families.
- Deterministic projection fold.
- Rebuild and replay contract.
- Separation of durable intent from high-frequency playback telemetry.

## Non-goals

- Choosing the final embedded database adapter.
- Building the GUI.
- Capturing every seek/progress tick in Git.

## Acceptance criteria

- Event envelopes preserve actor, time, request ID, subject, schema version, and
  provenance where applicable.
- Replay produces the same projection from the same ordered events.
- Duplicate request IDs do not duplicate durable intent.
- Raw playback telemetry is excluded or explicitly marked operational.
- Projection loss is recoverable from ledgers.

## Verification

Property/examples for deterministic replay, idempotency, append-only behavior,
and invalid event rejection pass under `clojure -M:test`.