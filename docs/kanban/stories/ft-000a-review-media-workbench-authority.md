---
uuid: "ft-000a-review-and-accept-or-revise-media-workbench-authority"
title: "FT-000A: Review and accept or revise Media Workbench authority"
status: done
type: story
priority: P0
phase: 0
epic: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
owner: Err
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

Err explicitly accepted ADR-001 and approved Media Workbench v1 after requiring
normal Rheos config discovery instead of repeated `--tasks-dir` defaults.

## Accepted boundaries

- Immutable source renders.
- First-class clips and arrangements.
- Scoped ratings, playlists, smart lists, and workspaces.
- Local accepted releases before target publication.
- Per-target publication state.
- Shared command/query application boundary.
- Native Clojure/JVM first client with no embedded browser.
- Daily-driver playback before editing and publication expansion.

## Review evidence

- Human disposition in the conversation and PR #3 comment `5099692071`.
- Independent local Claude review `4793817603`, which approved the design authority
  and requested Rheos mechanics corrections.
- Board mechanics corrected in commit `09be7d22f414a753f1a3a5067fb14f8e8fff6da3`.
- FT-000D created to own the native UI, playback backend, read model, and
  application-topology decisions found missing by the independent review.

## Acceptance

ADR-001 is `accepted`, Media Workbench v1 is `approved`, and implementation may
advance only through the explicit dependencies in the corrected board.
