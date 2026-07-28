---
id: "55c1a87e-122a-432a-9be2-4f26dbe8fcee"
title: "FT-004C: Implement checkpointed SoundCloud publication adapter"
status: icebox
type: story
priority: P1
phase: 4
epic: "9351aa82-a474-4dbc-8fee-be4acc09e02a"
owner: unassigned
points: 5
labels: [publishing, soundcloud, oauth]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["4b93e25c-f574-4ec3-ba5b-d7f5336dfb9e", "55a69664-5b24-4921-8761-c38a9234265b"]
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