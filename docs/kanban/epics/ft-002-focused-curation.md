---
id: "1168af89-845d-4af8-9c46-9155ec6dc235"
title: "FT-002: Make corpus curation fast and context-preserving"
status: incoming
type: epic
priority: P1
phase: 2
points: 13
labels: [media-workbench, curation, workspaces, decomposed]
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["033b5cda-ed7e-429b-95ac-a56c1ab7156d"]
---

# FT-002: Make corpus curation fast and context-preserving

## Outcome

Err can move through unreviewed renders quickly, save meaningful query contexts,
and return to a curation problem without reconstructing filters, queue, notes, or
active comparisons.

## Decomposed into

- FT-002A — smart lists and saved workspaces.
- FT-002B — keyboard-first triage and classifier-overlay review.

Implement the children, never this epic directly.