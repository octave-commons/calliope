---
category: "stories"
labels: "curation, smart-lists, workspaces"
dependency: ["ft-001d-add-dispositions-ratings-labels-sorting-and-playlists"]
process: "docs/process/product-design-and-delivery.md"
phase: "2"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714740974-0.3xzowldlde2oy4wd577"
points: "5"
title: "FT-002A: Add smart lists and saved media workspaces"
priority: "P0"
status: "blocked"
epic: "ft-002-make-corpus-curation-fast-and-context-preserving"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-002a-add-smart-lists-and-saved-media-workspaces"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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

---
Dependency analysis (2026-08-02): depends on FT-001D. Blocks nothing in the active columns (FT-002B is icebox). Moved breakdown -> blocked: waiting on FT-001D.
---