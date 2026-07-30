---
uuid: "ft-004b-generate-target-ready-release-packages"
title: "FT-004B: Generate target-ready release packages"
status: icebox
type: story
priority: P1
phase: 4
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
owner: unassigned
points: 5
labels: release, export-package, packaging
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-003c-implement-arrangement-playback-and-deterministic-export", "ft-004a-define-release-manifest-and-publication-target-laws", "ft-004f-build-release-builder-assembly-validation-and-local-acceptance"]
---

# FT-004B: Generate target-ready release packages

## Outcome

An accepted release can produce reproducible audio, artwork, metadata, lyrics,
credits, provenance, and checklist bundles for every target.

## Scope

- Target package manifests and directory layout.
- Audio encoding/loudness report.
- Artwork variants and metadata text/JSON/EDN.
- Package validation and output hashes.

## Non-goals

- Uploading to a platform.
- Claiming automated mastering or legal clearance.
- Video rendering. That is FT-004G, so that a target needing no video is never
  blocked on video generation.

## Acceptance criteria

- Package contents are derived from one accepted release version.
- Missing required target metadata blocks package readiness.
- Rebuild records tool/settings versions and output hashes.
- Manual targets receive a human-readable checklist.

## Verification

Fixture-based package tests cover deterministic manifests, missing metadata, and
output hashes. Confirm through Rheos that no publication adapter depends on this
card for video assets.
