---
id: "34436b78-1a11-4722-8d0c-9e54197bacb8"
title: "FT-002A: Add smart lists and saved media workspaces"
status: incoming
type: story
priority: P1
phase: 2
epic: "1168af89-845d-4af8-9c46-9155ec6dc235"
owner: unassigned
points: 5
labels: [curation, smart-lists, workspaces]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["bcd785d4-0ad9-4b42-bfd8-bf989a42c70c"]
---

# FT-002A: Add smart lists and saved media workspaces

## Outcome

Err can save a live corpus query and restore a broader attention context including
queue, pinned objects, notes, comparison state, and active creative focus.

## Scope

- Closed query language for smart lists.
- Saved sort/filter/column state.
- Workspace pins, notes, queue reference, layout, and active object references.
- Restore and versioning behavior.

## Non-goals

- Team collaboration.
- Rheos development-board state inside a media workspace.
- Arbitrary executable query code.

## Acceptance criteria

- Smart-list membership is a current projection, not frozen hidden state.
- A workspace restores meaningful focus without copying canonical media data.
- Deleted/unavailable referenced objects remain explicit in restored workspaces.
- Query definitions use a closed non-Turing-complete vocabulary.
- Playlist and workspace laws remain distinct.

## Verification

Tests cover deterministic query evaluation, workspace restore, stale references,
and round-trip serialization.