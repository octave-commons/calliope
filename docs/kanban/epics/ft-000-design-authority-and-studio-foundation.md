---
category: "epics"
labels: "media-workbench, architecture, law, decomposed"
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "epic"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785719665959-0.2tz6qdzefvr0evb3to9"
points: "13"
title: "FT-000: Establish media-workbench authority and durable studio foundation"
priority: "P0"
status: "in_progress"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
---

# FT-000: Establish media-workbench authority and durable studio foundation

## Outcome

Fork Tales has accepted architectural/product authority and versioned laws for
durable listening, curation, editing, release, and publication intent.

## Scope

- Review and accept or revise the governing research, ADR, design, and process.
- Define versioned media-workbench domain laws and durable event/projection laws.
- Decide the first native runtime, playback backend, read model, and application topology.
- Keep implementation responsibility in the bounded child stories below.

## Non-goals

- Player implementation.
- Waveform UI.
- Publication adapters.

## Decomposed into

- FT-000A — review ADR-001 and the v1 design.
- FT-000B — define studio-domain Malli laws.
- FT-000C — define append-only studio events and read projection.
- FT-000D — decide native runtime architecture through a real spike.

Implement the children, never this epic directly.

## Acceptance criteria

- The governing authority documents have explicit accepted/approved dispositions.
- FT-000A through FT-000D exist as resolvable child work in the board graph.
- Domain-law work precedes event/projection and adapter implementation.
- The native runtime decision requires real corpus audio evidence.
- No implementation is attributed directly to this decomposed epic.

## Verification

Read this epic and its four children through Rheos and confirm each resolves, that
every child's `epic` field is this epic's UUID, and that dependencies, ownership,
and status are as recorded:

```bash
eta-mu kanban find ft-000-establish-media-workbench-authority-and-durable-studio-foundation
eta-mu kanban find ft-000a-review-and-accept-or-revise-media-workbench-authority
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
eta-mu kanban find ft-000c-define-append-only-studio-events-and-read-projection
eta-mu kanban find ft-000d-decide-native-desktop-playback-read-model-and-application-topology
eta-mu kanban list
```

Child cards carry the executable tests and decision evidence for their bounded
scopes. Authority-path resolution is not checked by Rheos; verify those paths by
opening the referenced files.

---
Dependency analysis (2026-08-02): no dependencies — root epic. Blocks FT-001 (daily-driver player epic). Children: FT-000A, FT-000B, FT-000C, FT-000D. Moved breakdown -> ready: no unresolved blockers.

Gate 0 progress (updated 2026-08-25): FT-000A done (authority accepted). FT-000B done (studio domain laws, 30 tests / 193 assertions green). FT-000D done — ADR-002 accepted by Err on 2026-08-25 after spike evidence review; card moved review → document → done via Rheos. FT-000C ready (unblocked by the ADR-002 acceptance). Epic remains in_progress until children resolve. Earlier state (2026-08-02): FT-000D in review, FT-000C blocked on its acceptance.
---