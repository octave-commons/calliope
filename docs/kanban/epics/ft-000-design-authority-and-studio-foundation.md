---
uuid: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
title: "FT-000: Establish media-workbench authority and durable studio foundation"
status: breakdown
type: epic
priority: P0
phase: 0
owner: unassigned
points: 13
labels: media-workbench, architecture, law, decomposed
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
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

Run `python3 scripts/validate_rheos_board.py` and verify the four child UUIDs,
authority paths, dependencies, ownership, and status remain valid. Child cards carry
the executable tests and decision evidence for their bounded scopes.
