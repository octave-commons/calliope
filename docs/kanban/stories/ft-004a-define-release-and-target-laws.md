---
category: "stories"
labels: "release, law, publishing"
dependency: ["ft-000b-define-media-workbench-domain-laws", "ft-003d-preserve-render-to-release-derivation-graph"]
process: "docs/process/product-design-and-delivery.md"
phase: "4"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1788047454501-0.0u6vyt48kq7mvcb2841x"
points: "5"
title: "FT-004A: Define release manifest and publication target laws"
priority: "P1"
status: "icebox"
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-004a-define-release-manifest-and-publication-target-laws"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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

---
Recovery crosswalk (2026-08-30): GitHub issue #10 (https://github.com/octave-commons/calliope/issues/10) is the current-main release-admission/publication seam and proof umbrella. This card owns the pure law; FT-004F/C/D/E own assembly and target execution. Preserve the issue until its acceptance evidence is split and completed across those canonical cards.

Date correction: the preceding reconciliation comments say 2026-08-30, but Rheos recorded these operations on 2026-08-29 UTC. The substance is unchanged; this append-only correction preserves the original ledger history.
---