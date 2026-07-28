---
id: "9351aa82-a474-4dbc-8fee-be4acc09e02a"
title: "FT-004: Prepare releases and publish through explicit target capabilities"
status: icebox
type: epic
priority: P1
phase: 4
points: 34
labels: [media-workbench, release, publishing, decomposed]
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["dfad6caa-f439-4d41-95c4-18ed8807be6d"]
---

# FT-004: Prepare releases and publish through explicit target capabilities

## Outcome

An accepted local release can generate complete target packages and independently
track direct uploads, resumable uploads, distributor handoffs, and manual actions.

## Non-goals

- Making a remote platform canonical.
- Storing OAuth credentials in Git.
- Pretending all destinations expose upload APIs.

## Decomposed into

- FT-004A — release and target-capability laws.
- FT-004B — export packages and YouTube video assets.
- FT-004C — SoundCloud adapter.
- FT-004D — YouTube adapter.
- FT-004E — distributor/manual handoffs.

Implement the children, never this epic directly.