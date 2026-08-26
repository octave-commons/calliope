---
category: "epics"
labels: "media-workbench, player, library, decomposed"
dependency: ["ft-000-establish-media-workbench-authority-and-durable-studio-foundation"]
process: "docs/process/product-design-and-delivery.md"
phase: "1"
type: "epic"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714739428-0.smtf7dvwnenuj4gvhw"
points: "21"
title: "FT-001: Ship a daily-driver library and player"
priority: "P0"
status: "blocked"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-001-ship-a-daily-driver-library-and-player"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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
- FT-001E — system media keys through the native shell adapter.

Implement the children, never this epic directly.

## Acceptance criteria

- FT-001A through FT-001E exist as resolvable child work with explicit dependencies.
- The first usable gate requires representative real corpus MP3 playback.
- Every behavior the approved design marks as required for daily-driver use is
  owned by a child card, including system media-key integration.
- Transport and queue persist across application views.
- One unreadable item produces a visible error without destroying the queue.
- No editor or publication scope is smuggled into the player milestone.

## Verification

Read this epic and its five children through Rheos and confirm each resolves with
this epic's UUID in its `epic` field, and that each child's `dependency` array in
the returned card matches the expected edge below:

```bash
eta-mu kanban find ft-001-ship-a-daily-driver-library-and-player
eta-mu kanban find ft-001a-index-playable-media-metadata-and-waveform-jobs
eta-mu kanban find ft-001b-implement-playback-resolver-persistent-queue-and-resume
eta-mu kanban find ft-001c-build-persistent-player-shell-and-library-browser
eta-mu kanban find ft-001d-add-dispositions-ratings-labels-sorting-and-playlists
eta-mu kanban find ft-001e-integrate-system-media-keys-through-the-native-shell-adapter
eta-mu kanban list
```

Expected child dependency edges (verified against each `find` result's
`dependency` field, not just resolution and `epic` linkage):

- FT-001A depends on FT-000B, FT-000C, FT-000D.
- FT-001B depends on FT-000D, FT-001A.
- FT-001C depends on FT-000D, FT-001B.
- FT-001D depends on FT-000C, FT-001C.
- FT-001E depends on FT-000D, FT-001C.

Child-card tests and the Gate 1 native playback evidence verify implementation.
This epic remains `breakdown` until its child work is accepted.

---
Dependency analysis (2026-08-02): depends on FT-000 (foundation epic). Blocks FT-002 and FT-003 epics. Children: FT-001A (accepted), FT-001B, FT-001C, FT-001D, FT-001E (icebox). Moved breakdown -> blocked: waiting on FT-000.
---