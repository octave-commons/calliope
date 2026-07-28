---
id: "55a69664-5b24-4921-8761-c38a9234265b"
title: "FT-004B: Generate target-ready release packages and YouTube video assets"
status: icebox
type: story
priority: P1
phase: 4
epic: "9351aa82-a474-4dbc-8fee-be4acc09e02a"
owner: unassigned
points: 5
labels: [release, export-package, video]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["b539c602-c635-4735-aa9c-500c63f900d3", "4b93e25c-f574-4ec3-ba5b-d7f5336dfb9e"]
---

# FT-004B: Generate target-ready release packages and YouTube video assets

## Outcome

An accepted release can produce reproducible audio, artwork, metadata, lyrics,
credits, provenance, and checklist bundles, plus a video container suitable for
YouTube upload.

## Scope

- Target package manifests and directory layout.
- Audio encoding/loudness report.
- Artwork variants and metadata text/JSON/EDN.
- Still-art or waveform video rendering for YouTube.
- Package validation and output hashes.

## Non-goals

- Uploading to a platform.
- Claiming automated mastering or legal clearance.

## Acceptance criteria

- Package contents are derived from one accepted release version.
- YouTube output is a valid video container, not an audio file mislabeled as video.
- Missing required target metadata blocks package readiness.
- Rebuild records tool/settings versions and output hashes.
- Manual targets receive a human-readable checklist.

## Verification

Fixture-based package and media-probe tests cover deterministic manifests, missing
metadata, video generation, and output hashes.