---
category: "epics"
labels: "media-workbench, curation, workspaces, decomposed"
dependency: ["ft-001-ship-a-daily-driver-library-and-player"]
process: "docs/process/product-design-and-delivery.md"
phase: "2"
type: "epic"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714741364-0.mtiaoi5fkrmyc5i00c5"
points: "13"
title: "FT-002: Make corpus curation fast and context-preserving"
priority: "P1"
status: "blocked"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-002-make-corpus-curation-fast-and-context-preserving"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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

Read this epic and its two children through Rheos and confirm each resolves with
this epic's UUID in its `epic` field:

```bash
eta-mu kanban find ft-002-make-corpus-curation-fast-and-context-preserving
eta-mu kanban find ft-002a-add-smart-lists-and-saved-media-workspaces
eta-mu kanban find ft-002b-build-keyboard-first-triage-and-classifier-overlay-review
eta-mu kanban list
```

Expected child dependency edges (verified against each `find` result's
`dependency` field, not just resolution and `epic` linkage):

- FT-002A depends on FT-001D.
- FT-002B depends on FT-001D, FT-002A, and FT-003A.

FT-002B's edge to FT-003A is deliberate and crosses the phase 2/3 boundary: the
triage surface records provisional markers and in/out points, and the marker and
clip commands it dispatches are owned by FT-003A. Gate 2 therefore cannot be
accepted before FT-003A lands. Do not "simplify" this edge away — removing it
reintroduces a second marker command path, which the ADR's command/query boundary
forbids.

Child interaction and projection tests verify query determinism, workspace
restoration, keyboard safety, and proposal adjudication.

---
Dependency analysis (2026-08-02): depends on FT-001 (player epic). Blocks nothing active. Moved breakdown -> blocked: waiting on FT-001.
---