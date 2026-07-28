---
uuid: "ft-004d-implement-resumable-youtube-publication-adapter"
title: "FT-004D: Implement resumable YouTube publication adapter"
status: icebox
type: story
priority: P1
phase: 4
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
owner: unassigned
points: 5
labels: publishing, youtube, resumable-upload
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-004a-define-release-manifest-and-publication-target-laws", "ft-004b-generate-target-ready-release-packages-and-youtube-video-assets"]
---

# FT-004D: Implement resumable YouTube publication adapter

## Outcome

A generated release video can be uploaded with resumable checkpoints and explicit
privacy, processing, audit, and publication state.

## Scope

- OAuth authorization and YouTube upload scope.
- Resumable upload session/checkpoint state.
- Video metadata, tags, playlist placement, visibility, and external IDs.
- Private-only/audit restriction handling.
- Processing-status polling.

## Non-goals

- Uploading raw audio as a normal channel video.
- Assuming an API project can publish publicly without required audit.
- Storing credentials in Git.

## Acceptance criteria

- Adapter requires a validated video asset.
- Interrupted uploads can resume from durable non-secret checkpoint state.
- Privacy requested, privacy achieved, and audit restriction are distinguishable.
- Upload completion and YouTube processing completion are separate states.
- External video/playlist IDs are preserved.

## Verification

Mock/API-contract tests cover resumable interruption, expired sessions, private
restriction, processing failure, duplicate requests, and credential redaction.
Live verification is separately recorded when an approved channel/project exists.
