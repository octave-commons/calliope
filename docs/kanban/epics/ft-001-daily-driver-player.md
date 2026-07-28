---
id: "033b5cda-ed7e-429b-95ac-a56c1ab7156d"
title: "FT-001: Ship a daily-driver library and player"
status: accepted
type: epic
priority: P0
phase: 1
points: 21
labels: [media-workbench, player, library, decomposed]
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["c75d00eb-7cc8-4938-a088-7f2acefb0f4f"]
---

# FT-001: Ship a daily-driver library and player

## Outcome

Fork Tales is pleasant and reliable enough to remain open as the primary player
for the corpus, with persistent transport, queue, library browsing, ratings,
labels, sorting, and playlists.

## Non-goals

- Waveform editing.
- Arrangement export.
- Remote publication.

## Decomposed into

- FT-001A — media metadata and waveform-peak indexing.
- FT-001B — playback resolver and queue.
- FT-001C — application shell and library.
- FT-001D — dispositions, ratings, labels, and playlists.

Implement the children, never this epic directly.