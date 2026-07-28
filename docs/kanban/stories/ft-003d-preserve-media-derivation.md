---
uuid: "ft-003d-preserve-render-to-release-derivation-graph"
title: "FT-003D: Preserve render-to-release derivation graph"
status: incoming
type: story
priority: P1
phase: 3
epic: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
owner: unassigned
points: 3
labels: provenance, derivation, export
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-003c-implement-arrangement-playback-and-deterministic-export"]
---

# FT-003D: Preserve render-to-release derivation graph

## Outcome

Every clip, arrangement, and export can display and export its complete derivation
chain back to immutable source renders and works.

## Scope

- Typed derivation relations.
- Query from derivative to source and source to derivatives.
- Source hashes, versions, ranges, transforms, and export settings.
- Inspector and machine-readable provenance packet.

## Non-goals

- Inferring semantic identity from similarity.
- Blockchain or public provenance registry.

## Acceptance criteria

- Derivation edges are typed and evidence-backed.
- A derivative never becomes a source render in the model.
- Missing source media remains visible as unavailable provenance.
- The complete chain can be exported without embedding credentials/private tokens.

## Verification

Graph/query tests cover multi-render arrangements, superseded clips, unavailable
sources, and provenance packet round trips.
