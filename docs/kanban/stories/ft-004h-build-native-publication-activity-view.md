---
uuid: "ft-004h-build-native-publication-activity-and-retry-view"
title: "FT-004H: Build native Publication Activity and retry view"
status: icebox
type: story
priority: P1
phase: 4
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
owner: unassigned
points: 5
labels: publishing, native-ui, activity, retry
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-004g-render-youtube-video-assets-from-an-accepted-release", "ft-004c-implement-checkpointed-soundcloud-publication-adapter", "ft-004d-implement-resumable-youtube-publication-adapter", "ft-004e-implement-distributor-and-manual-publication-handoffs"]
---

# FT-004H: Build native Publication Activity and retry view

## Outcome

Err can use one native Publication Activity screen to inspect video-render jobs and
every target attempt, distinguish rendering, upload, processing, and publication,
and explicitly retry, resume, cancel, or complete manual actions without changing
another target's state.

## Scope

- A native Publication Activity view implemented through FT-000D's selected
  UI/runtime and application topology.
- A query/projection over FT-004G's durable video-render job events and the
  independent attempt states emitted by FT-004C, FT-004D, and FT-004E.
- Filtering by release, target, job/attempt state, and required user action.
- Job/attempt detail showing state history, checkpoints, timestamps, visibility,
  external IDs/URLs, output identity, and sanitized response summaries.
- Pointer and keyboard actions that dispatch FT-004G's or the owning adapter's
  retry, resume, cancel, or manual-confirmation commands.
- Explicit refusal and unavailable states when the owning service, adapter, or
  target capability does not support an action.
- Restart-safe reconstruction from durable render-job and publication-attempt
  records.

## Non-goals

- Implementing video rendering, upload, distributor, or manual-handoff behavior.
  Those remain FT-004G and FT-004C/D/E.
- A global publish, retry-all, or cancel-all action.
- Storing credentials or raw secret-bearing provider responses.
- Treating rendering, upload completion, processing completion, and publication as
  one state.
- Making remote platform state canonical over local job/attempt history.

## Acceptance criteria

- Planned, validating, rendering, ready, authenticating, uploading, processing,
  published, failed, unavailable, cancelled, and manual-action-required states are
  distinguishable only where the owning service or adapter can emit them.
- `rendering` and video-render recovery come from FT-004G job events and commands,
  not from a UI-only synthetic attempt state.
- Cancellation dispatches an owning FT-004G/FT-004C/FT-004D command and becomes
  `cancelled` only after that owner emits the outcome; unsupported or too-late
  cancellation remains unavailable with an inspectable reason.
- Failure, retry, cancellation, or completion for one job/target does not mutate any
  sibling target attempt or the accepted release identity.
- Retry creates a new linked job/attempt or resumes an explicit durable checkpoint;
  it never silently rewrites prior history.
- Pointer and keyboard paths dispatch the same owning commands, and the view does
  not maintain a second UI-only publication state machine.
- Unsupported actions are disabled with an inspectable reason rather than failing
  silently.
- External IDs, URLs, timestamps, visibility, output identity, and sanitized
  summaries remain inspectable after restart, while credentials and secret-bearing
  payloads do not.

## Verification

Native interaction tests cover filtering, job/attempt detail, keyboard/pointer
command parity, unsupported-action reasons, and refusal display. Integration tests
consume representative FT-004G render-job events and FT-004C/D/E attempt events and
verify rendering progress/recovery, owner-confirmed cancellation, independent target
state, linked retries, resumable checkpoints, manual-action completion, restart
reconstruction, external-reference display, and credential redaction.
