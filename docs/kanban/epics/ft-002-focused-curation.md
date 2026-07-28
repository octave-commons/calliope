---
uuid: "ft-002-make-corpus-curation-fast-and-context-preserving"
title: "FT-002: Make corpus curation fast and context-preserving"
status: breakdown
type: epic
priority: P1
phase: 2
owner: unassigned
points: 13
labels: media-workbench, curation, workspaces, decomposed
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-001-ship-a-daily-driver-library-and-player"]
---

# FT-002: Make corpus curation fast and context-preserving

## Outcome

Err can move through unreviewed renders quickly, save meaningful query contexts,
and return to a curation problem without reconstructing filters, queue, notes, or
active comparisons.

## Scope

- Add closed-query smart lists with deterministic current membership.
- Save and restore media-workspace context without copying canonical media data.
- Build keyboard-first triage over an unreviewed or smart-list-backed queue.
- Review classifier overlays as proposals requiring explicit accept/reject decisions.

## Non-goals

- Team collaboration or shared workspace conflict resolution.
- Arbitrary executable query code.
- Treating the Rheos development board as a media workspace.
- Automatic acceptance of model classifications.

## Decomposed into

- FT-002A — smart lists and saved workspaces.
- FT-002B — keyboard-first triage and classifier-overlay review.

Implement the children, never this epic directly.

## Acceptance criteria

- FT-002A and FT-002B resolve as bounded children of this epic.
- Saved workspaces restore attention context without duplicating source media.
- Smart-list membership remains a rebuildable projection.
- Primary triage actions are keyboard-accessible and visibly confirmed.
- Proposed classifier output remains distinct from accepted human decisions.

## Verification

Run `python3 scripts/validate_rheos_board.py`; child interaction and projection
tests verify query determinism, workspace restoration, keyboard safety, and
proposal adjudication.
