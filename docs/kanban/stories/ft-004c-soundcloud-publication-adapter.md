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
dependency: ["ft-004a-define-release-manifest-and-publication-target-laws", "ft-004b-generate-target-ready-release-packages"]
---

# FT-004C: Implement checkpointed SoundCloud publication adapter

## Outcome

A target-ready release can upload audio and metadata to SoundCloud while keeping
credentials external and recording target processing/publication state, including
explicit cancellation capability where the active operation can actually be
aborted.

## Scope

- OAuth authorization through a local credential store.
- Track upload, metadata/artwork update, and optional playlist placement.
- Progress, retry, processing state, external ID/URL, and visibility.
- An attempt-cancel command for active local/upload operations that can still be
  aborted, with a capability/refusal result when the provider state cannot be
  cancelled.
- Capability/version checks.

## Non-goals

- Storing tokens in Git.
- Treating SoundCloud transcoded audio as canonical.
- Uploading unaccepted releases.
- Pretending cancellation deletes or rolls back an already-created remote track.

## Acceptance criteria

- Adapter refuses release versions that are not locally accepted and target-ready.
- Credentials never appear in logs, ledgers, receipts, or packages.
- Upload and processing states are independently visible.
- Retry is idempotent or records a deliberate new attempt.
- Cancellation records an explicit attempt outcome, preserves prior history, and
  does not mutate the accepted release or sibling target attempts.
- Cancellation is unavailable with an inspectable reason once the operation cannot
  be aborted; the adapter never reports `cancelled` merely because the UI requested
  it.
- External resource ID and URL are preserved on success.

## Verification

Mock/API-contract tests cover auth absence, upload success, encoding delay, rate
limit/retry, duplicate request handling, cancellation before and after the provider's
abort boundary, unsupported-cancellation reasons, and redaction. Live verification
is separately recorded when credentials and an approved test target exist.
