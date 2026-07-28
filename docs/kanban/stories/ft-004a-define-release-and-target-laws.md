---
uuid: "ft-004a-define-release-manifest-and-publication-target-laws"
title: "FT-004A: Define release manifest and publication target laws"
status: icebox
type: story
priority: P1
phase: 4
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
owner: unassigned
points: 5
labels: release, law, publishing
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000b-define-media-workbench-domain-laws", "ft-003d-preserve-render-to-release-derivation-graph"]
---

# FT-004A: Define release manifest and publication target laws

## Outcome

An accepted local release and each target's capability/state can be represented
without conflating export, upload, processing, manual handoff, and publication.

## Scope

- Release metadata, assets, track order, credits, lyrics, provenance, and rights
  basis.
- Local acceptance event.
- Target capability declaration.
- Publication attempt/checkpoint/outcome states.
- Credential references that contain no secrets.

## Non-goals

- Legal determination of rights ownership.
- OAuth implementation.
- Actual upload adapters.

## Acceptance criteria

- Publication request requires an accepted local release.
- Per-target states allow partial success.
- Direct, resumable, export-package, manual, and distributor capabilities remain
  distinct.
- Tokens and secrets are structurally excluded from portable records.
- Target availability/version is explicit.

## Verification

Contract and state-transition tests cover invalid premature publication, partial
target outcomes, manual handoff, retries/checkpoints, and secret-field rejection.
