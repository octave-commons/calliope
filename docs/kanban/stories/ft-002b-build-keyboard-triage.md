---
category: "stories"
labels: "triage, keyboard, classifier-review"
dependency: ["ft-001d-add-dispositions-ratings-labels-sorting-and-playlists", "ft-002a-add-smart-lists-and-saved-media-workspaces", "ft-003a-implement-marker-and-clip-commands-over-immutable-renders"]
process: "docs/process/product-design-and-delivery.md"
phase: "2"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785713983291-0.apoqmzq7k40lbf3o5i"
points: "5"
title: "FT-002B: Build keyboard-first triage and classifier-overlay review"
priority: "P1"
status: "icebox"
epic: "ft-002-make-corpus-curation-fast-and-context-preserving"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-002b-build-keyboard-first-triage-and-classifier-overlay-review"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
---

# FT-002B: Build keyboard-first triage and classifier-overlay review

## Outcome

Err can listen through an unreviewed queue and record dispositions, ratings,
markers, and clip in/out proposals without leaving the playback flow.

## Scope

- Configurable shortcut map and help overlay.
- Unreviewed/smart-list queue source.
- Quick rating/disposition/label actions.
- In/out points and provisional marker creation, dispatched to FT-003A's marker
  and clip commands rather than a triage-local command path.
- Derived classifier overlays with accept/reject actions.

## Non-goals

- Full waveform editing.
- Automatic acceptance of classifier labels or section boundaries.
- Implementing marker or clip commands. Triage consumes FT-003A's commands; a
  second command path for the same domain operations is prohibited.

## Acceptance criteria

- All primary triage actions are keyboard-accessible and visibly confirmed.
- Shortcut collisions are configurable and documented.
- Classifier suggestions show status, evidence, and extractor identity.
- Accept/reject creates durable decisions; merely viewing a proposal does not.
- Queue advance never hides a failed command.
- Marker and in/out actions dispatch FT-003A commands; no marker or clip write
  path originates in this card's code.

## Verification

Interaction tests cover shortcuts, command failure, queue advance, proposal
accept/reject, and input-focus safety.