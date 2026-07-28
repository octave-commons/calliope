---
uuid: "ft-004c-implement-checkpointed-soundcloud-publication-adapter"
title: "FT-004C: Implement checkpointed SoundCloud publication adapter"
status: icebox
type: story
priority: P1
phase: 4
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
owner: unassigned
points: 5
labels: publishing, soundcloud, oauth
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-004a-define-release-manifest-and-publication-target-laws", "ft-004b-generate-target-ready-release-packages-and-youtube-video-assets"]
---

# FT-004C: Implement checkpointed SoundCloud publication adapter

## Outcome

A target-ready release can upload audio and metadata to SoundCloud while keeping
credentials external and recording target processing/publication state.

## Scope

- OAuth authorization through a local credential store.
- Track upload, metadata/artwork update, and optional playlist placement.
- Progress, retry, processing state, external ID/URL, and visibility.
- Capability/version checks.

## Non-goals

- Storing tokens in Git.
- Treating SoundCloud transcoded audio as canonical.
- Uploading unaccepted releases.

## Acceptance criteria

- Adapter refuses release versions that are not locally accepted/ready.
- Credentials never appear in logs, ledgers, receipts, or packages.
- Upload and processing states are independently visible.
- Retry is idempotent or records a deliberate new attempt.
- External resource ID and URL are preserved on success.

## Verification

Mock/API-contract tests cover auth absence, upload success, encoding delay, rate
limit/retry, duplicate request handling, and redaction. Live verification is
separately recorded when credentials and an approved test target exist.
