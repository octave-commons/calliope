---
uuid: "ft-002b-build-keyboard-first-triage-and-classifier-overlay-review"
title: "FT-002B: Build keyboard-first triage and classifier-overlay review"
status: incoming
type: story
priority: P0
phase: 2
epic: "ft-002-make-corpus-curation-fast-and-context-preserving"
owner: unassigned
points: 5
labels: triage, keyboard, classifier-review
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-001d-add-dispositions-ratings-labels-sorting-and-playlists", "ft-002a-add-smart-lists-and-saved-media-workspaces"]
---

# FT-002B: Build keyboard-first triage and classifier-overlay review

## Outcome

Err can listen through an unreviewed queue and record dispositions, ratings,
markers, and clip in/out proposals without leaving the playback flow.

## Scope

- Configurable shortcut map and help overlay.
- Unreviewed/smart-list queue source.
- Quick rating/disposition/label actions.
- In/out points and provisional marker creation.
- Derived classifier overlays with accept/reject actions.

## Non-goals

- Full waveform editing.
- Automatic acceptance of classifier labels or section boundaries.

## Acceptance criteria

- All primary triage actions are keyboard-accessible and visibly confirmed.
- Shortcut collisions are configurable and documented.
- Classifier suggestions show status, evidence, and extractor identity.
- Accept/reject creates durable decisions; merely viewing a proposal does not.
- Queue advance never hides a failed command.

## Verification

Interaction tests cover shortcuts, command failure, queue advance, proposal
accept/reject, and input-focus safety.
