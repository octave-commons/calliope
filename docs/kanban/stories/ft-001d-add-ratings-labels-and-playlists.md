---
id: "bcd785d4-0ad9-4b42-bfd8-bf989a42c70c"
title: "FT-001D: Add dispositions, ratings, labels, sorting, and playlists"
status: incoming
type: story
priority: P0
phase: 1
epic: "033b5cda-ed7e-429b-95ac-a56c1ab7156d"
owner: unassigned
points: 5
labels: [curation, ratings, labels, playlists]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["d7e1dc7e-cf4d-4b77-8714-69ce092e1c2d", "2cc53668-22a0-4701-91b9-001f2deb9ca6"]
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

## Verification

Command/projection tests cover repeated requests, independent rating dimensions,
render/clip scope separation, label provenance, and playlist ordering.