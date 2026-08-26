---
category: "stories"
labels: "curation, ratings, labels, playlists"
dependency: ["ft-000c-define-append-only-studio-events-and-read-projection", "ft-001c-build-persistent-player-shell-and-library-browser"]
process: "docs/process/product-design-and-delivery.md"
phase: "1"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714740603-0.qo9laf8ik2l5yu8dtax"
points: "5"
title: "FT-001D: Add dispositions, ratings, labels, sorting, and playlists"
priority: "P0"
status: "blocked"
epic: "ft-001-ship-a-daily-driver-library-and-player"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-001d-add-dispositions-ratings-labels-sorting-and-playlists"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
---

# FT-001D: Add dispositions, ratings, labels, sorting, and playlists

## Outcome

Listening can immediately produce durable, scoped curation without leaving the
player.

## Scope

- Keeper/salvage/reject/unreviewed dispositions.
- Enjoyment, publishability, weirdness, salvageability, and technical-quality
  ratings.
- User labels and accepted classifier proposals.
- Library sort/filter columns.
- Explicit ordered playlists containing declared playable refs.
- A save-queue-as-playlist command plus its player affordance, capturing the
  current queue order as a new explicit playlist.

## Non-goals

- Smart/query-backed lists.
- Workspaces.
- Automatically aggregating clip ratings to renders or works.

## Acceptance criteria

- Quick actions append idempotent durable events.
- Rating subject and dimension are visible and inspectable.
- Rejecting a render does not reject clips or delete media.
- Labels distinguish user decisions from classifier proposals.
- Playlist reorder preserves an auditable edit sequence and current projection.
- Library sorting can choose one rating dimension without an unexplained aggregate.
- Saving the queue as a playlist preserves the queue's current order, and later
  queue mutations do not retroactively alter the saved playlist.

## Verification

Command/projection tests cover repeated requests, independent rating dimensions,
render/clip scope separation, label provenance, and playlist ordering.

---
Dependency analysis (2026-08-02): depends on FT-000C and FT-001C. Blocks FT-002A. Moved breakdown -> blocked: waiting on FT-000C and FT-001C.
---