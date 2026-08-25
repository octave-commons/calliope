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
dependency: ["ft-004a-define-release-manifest-and-publication-target-laws", "ft-004f-build-release-builder-assembly-validation-and-local-acceptance"]
---

# FT-004G: Render YouTube video assets from an accepted release

## Outcome

An accepted release can produce a valid video container suitable for YouTube
upload, with durable render-job progress and recovery, without any other target's
readiness depending on it.

## Scope

- Still-art or waveform video rendering from the release's accepted audio.
- Video container, encoding, and duration validation.
- Render settings, tool versions, and output hashes recorded for rebuild.
- Durable render-job identity and planned, rendering, succeeded, failed, and
  cancelled events/projected states.
- Retry and cancel commands over an active or failed render job; retries create a
  linked job rather than rewriting prior history.
- A `needs-video` readiness state for targets that require video.

## Non-goals

- Uploading to a platform.
- Package manifests, audio encoding, artwork variants, or metadata bundles. Those
  are FT-004B.
- Blocking any non-video target's readiness.
- Mutating the accepted release when a render job fails, retries, or is cancelled.

## Acceptance criteria

- Output is a valid video container, not an audio file mislabeled as video.
- Video derives from one accepted release version produced by FT-004F.
- Render-job state and history reconstruct after restart and are queryable by the
  Publication Activity projection.
- A failed job can create a linked retry; an active job can be cancelled only at an
  explicit safe boundary, with refusal reasons when cancellation is unavailable.
- A missing, failed, or cancelled video render marks only video-requiring targets as
  `needs-video` and leaves other targets' readiness unchanged.
- Rebuild records tool/settings versions and output hashes.

## Verification

Media-probe tests cover container validity, duration, and output hashes. Job tests
cover planned-to-rendering-to-success, failure, linked retry, cancellation/refusal,
and restart reconstruction. A readiness test asserts that a failed or cancelled
video render leaves SoundCloud and distributor targets unaffected while YouTube
reports `needs-video`; an integration test exposes the same job events to FT-004H.

## Why this card exists

FT-004B previously bundled package generation with YouTube video rendering, and
FT-004C/FT-004D/FT-004E all depended on the whole card. That made SoundCloud
delivery wait on YouTube video generation, contradicting the approved design's
target matrix, which shows SoundCloud `ready` while YouTube is `needs-video`
(`docs/designs/media-workbench-v1.md`, "Release Builder"), and contradicting the
target-specific publication-state boundary in `AGENTS.md`. FT-004B and FT-004G now
branch independently from FT-004F's accepted release; only the YouTube adapter joins
both branches. Raised in PR #3 review.
