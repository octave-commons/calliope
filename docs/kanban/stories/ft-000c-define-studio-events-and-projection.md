---
category: "stories"
labels: "event-ledger, projection, media-workbench"
dependency: ["ft-000b-define-media-workbench-domain-laws", "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"]
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1787707467398-0.q5z826iv75paw20vg30"
points: "5"
title: "FT-000C: Define append-only studio events and read projection"
priority: "P0"
status: "ready"
epic: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-000c-define-append-only-studio-events-and-read-projection"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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

---
Dependency analysis (2026-08-02): depends on FT-000B and FT-000D. Blocks FT-001D (ratings/labels/playlists write studio events). Moved breakdown -> blocked: waiting on FT-000B and FT-000D.

Unblocked: ADR-002 accepted by Err on 2026-08-25, so the read-model choice this card's projection law depends on (SQLite rebuildable projection) is now governing. Moved blocked -> ready via Rheos.
---