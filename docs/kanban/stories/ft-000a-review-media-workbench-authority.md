---
uuid: "ft-000a-review-and-accept-or-revise-media-workbench-authority"
title: "FT-000A: Review and accept or revise Media Workbench authority"
status: done
type: story
priority: P0
phase: 0
epic: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
owner: unassigned
points: 2
labels: architecture, design-review, media-workbench
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
---

# FT-000A: Review and accept or revise Media Workbench authority

## Outcome

Err records an explicit disposition on ADR-001 and Media Workbench v1: approve,
request revisions, or reject.

## Scope

- Immutable source renders.
- First-class clips and arrangements.
- Ratings, playlists, smart lists, and workspaces.
- Local release before target publication.
- Local service and shared application boundary.
- Delivery gates and deferred publication work.

## Non-goals

- Implementing schemas or UI.
- Selecting every library or desktop-shell dependency.

## Acceptance criteria

- ADR-001 has an explicit status and review basis.
- The design is approved, revised, or rejected consistently with the ADR.
- Material open questions that block laws become separate research/decision cards.
- The review comment names any changed boundary rather than only saying “looks good.”

## Verification

Document/link review for internal consistency among research, ADR, design, process,
and board breakdown. No runtime test is required.
