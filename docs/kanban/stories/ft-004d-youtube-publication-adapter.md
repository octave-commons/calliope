---
id: "cf03f612-9a4f-45b8-9516-11e8d5fc6e29"
title: "FT-004D: Implement resumable YouTube publication adapter"
status: icebox
type: story
priority: P1
phase: 4
epic: "9351aa82-a474-4dbc-8fee-be4acc09e02a"
owner: unassigned
points: 5
labels: [publishing, youtube, resumable-upload]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["4b93e25c-f574-4ec3-ba5b-d7f5336dfb9e", "55a69664-5b24-4921-8761-c38a9234265b"]
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