---
uuid: "ft-001-ship-a-daily-driver-library-and-player"
title: "FT-001: Ship a daily-driver library and player"
status: breakdown
type: epic
priority: P0
phase: 1
owner: unassigned
points: 21
labels: media-workbench, player, library, decomposed
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000-establish-media-workbench-authority-and-durable-studio-foundation"]
---

# FT-001: Ship a daily-driver library and player

## Outcome

Fork Tales is pleasant and reliable enough to remain open as the primary player
for the corpus, with persistent transport, queue, library browsing, ratings,
labels, sorting, and playlists.

## Scope

- Index playable media metadata and rebuildable waveform jobs.
- Implement playback resolution, queue behavior, and failure recovery.
- Build the persistent native player shell and library browser.
- Add dispositions, ratings, labels, sorting, and playlists.

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

## Acceptance criteria

- FT-001A through FT-001D exist as resolvable child work with explicit dependencies.
- The first usable gate requires representative real corpus MP3 playback.
- Transport and queue persist across application views.
- One unreadable item produces a visible error without destroying the queue.
- No editor or publication scope is smuggled into the player milestone.

## Verification

Run `python3 scripts/validate_rheos_board.py`; child-card tests and the Gate 1
native playback evidence verify implementation. This epic remains `breakdown`
until its child work is accepted.
