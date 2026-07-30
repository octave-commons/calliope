---
uuid: "ft-004g-render-youtube-video-assets-from-an-accepted-release"
title: "FT-004G: Render YouTube video assets from an accepted release"
status: icebox
type: story
priority: P1
phase: 4
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
owner: unassigned
points: 3
labels: release, video, publishing
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-004a-define-release-manifest-and-publication-target-laws", "ft-004b-generate-target-ready-release-packages"]
---

# FT-004G: Render YouTube video assets from an accepted release

## Outcome

An accepted release can produce a valid video container suitable for YouTube
upload, without any other target's readiness depending on it.

## Scope

- Still-art or waveform video rendering from the release's accepted audio.
- Video container, encoding, and duration validation.
- Render settings, tool versions, and output hashes recorded for rebuild.
- A `needs-video` readiness state for targets that require video.

## Non-goals

- Uploading to a platform.
- Package manifests, audio encoding, artwork variants, or metadata bundles. Those
  are FT-004B.
- Blocking any non-video target's readiness.

## Acceptance criteria

- Output is a valid video container, not an audio file mislabeled as video.
- Video derives from one accepted release version.
- A missing or failing video render marks only video-requiring targets as
  `needs-video` and leaves other targets' readiness unchanged.
- Rebuild records tool/settings versions and output hashes.

## Verification

Media-probe tests cover container validity, duration, and output hashes. A
readiness test asserts that a failed video render leaves SoundCloud and
distributor targets unaffected while YouTube reports `needs-video`.

## Why this card exists

FT-004B previously bundled package generation with YouTube video rendering, and
FT-004C/FT-004D/FT-004E all depended on the whole card. That made SoundCloud
delivery wait on YouTube video generation, contradicting the approved design's
target matrix, which shows SoundCloud `ready` while YouTube is `needs-video`
(`docs/designs/media-workbench-v1.md`, "Release Builder"), and contradicting the
target-specific publication-state boundary in `AGENTS.md`. Splitting video out
decouples every non-video target. Raised in PR #3 review.
