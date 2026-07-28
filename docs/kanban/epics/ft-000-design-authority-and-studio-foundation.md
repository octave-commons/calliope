---
id: "c75d00eb-7cc8-4938-a088-7f2acefb0f4f"
title: "FT-000: Establish media-workbench authority and durable studio foundation"
status: accepted
type: epic
priority: P0
phase: 0
points: 13
labels: [media-workbench, architecture, law, decomposed]
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: []
---

# FT-000: Establish media-workbench authority and durable studio foundation

## Outcome

Fork Tales has accepted architectural/product authority and versioned laws for
durable listening, curation, editing, release, and publication intent.

## Non-goals

- Player implementation.
- Waveform UI.
- Publication adapters.

## Decomposed into

- FT-000A — review ADR-001 and the v1 design.
- FT-000B — define studio-domain Malli laws.
- FT-000C — define append-only studio events and read projection.

Implement the children, never this epic directly.