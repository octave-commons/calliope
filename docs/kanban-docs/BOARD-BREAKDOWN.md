# Board Breakdown: Fork Tales Media Workbench

The board is organized as usable gates. Each gate must leave the application more
valuable as a daily driver; publication work may not delay local listening and
salvage.

ADR-001 is accepted and Media Workbench v1 is approved. Card readiness is now
controlled by explicit dependencies rather than unresolved product authority.

## Gate 0 — Authority and durable media decisions

| Card | Outcome | Pts | Depends on | State |
|---|---|---:|---|---|
| FT-000A | Review and accept/revise ADR-001 and Media Workbench v1 | 2 | — | incoming |
| FT-000B | Define Malli laws for playable refs, ratings, labels, clips, arrangements, workspaces, releases, and targets | 5 | 000A | ready |
| FT-000D | Decide native UI, real audio backend, read model, and in-process application topology | 5 | 000A | ready |
| FT-000C | Define append-only studio events and deterministic rebuildable projection | 5 | 000B, 000D | incoming |

Gate outcome: the application can represent durable user intent and has an
evidence-backed native implementation topology without treating projections,
source audio, or model suggestions as mutable truth.

FT-000B and FT-000D may run in parallel. FT-000C joins their decisions into the
application/event boundary.

## Gate 1 — Native daily-driver player

| Card | Outcome | Pts | Depends on | State |
|---|---|---:|---|---|
| FT-001A | Index playable media metadata and waveform-peak jobs using the selected read model | 5 | 000B, 000C, 000D | incoming |
| FT-001B | Implement real playback resolver, persistent queue, resume, and failure isolation | 5 | 000D, 001A | incoming |
| FT-001C | Build native Clojure/JVM shell and virtualized library browser | 5 | 000D, 001B | incoming |
| FT-001D | Add disposition, multidimensional ratings, labels, sorting, and playlists | 5 | 000C, 001C | incoming |
| FT-001E | Integrate system media keys through the native shell adapter | 3 | 000D, 001C | incoming |

Gate outcome: Fork Tales is demonstrably useful as the everyday player for the
corpus. Acceptance requires representative real MP3 playback, seek, pause/resume,
queue advance, restart recovery, and unreadable-item isolation; fake media tests
alone are insufficient.

## Gate 2 — Focused curation

| Card | Outcome | Pts | Depends on | State |
|---|---|---:|---|---|
| FT-002A | Add query-backed smart lists and saved workspaces | 5 | 001D | incoming |
| FT-002B | Add keyboard-first triage and classifier-overlay review | 5 | 001D, 002A, 003A | incoming |

Gate outcome: the corpus can be reviewed quickly without losing richer attention
context or classifier provenance.

FT-002B's edge to FT-003A crosses into Gate 3 on purpose: triage records
provisional markers and in/out points, and those commands are owned by FT-003A.
Gate 2 cannot be accepted before FT-003A lands.

## Gate 3 — Salvage and arrangement

| Card | Outcome | Pts | Depends on | State |
|---|---|---:|---|---|
| FT-003A | Implement marker and clip commands with immutable source ranges | 5 | 000B, 000C, 001B | incoming |
| FT-003B | Build waveform salvage editor with loop, in/out, fades, clip audition, and manual sibling comparison | 5 | 001C, 003A | incoming |
| FT-003C | Implement arrangement playback and deterministic audio export | 5 | 003A, 003B | incoming |
| FT-003D | Preserve derivation graph from render through clip, arrangement, and export | 3 | 003C | incoming |

Gate outcome: valuable spans can be recovered from globally poor renders and
assembled without destructive editing.

Automatic cross-render alignment is not part of FT-003B. V1 supports manual
comparison with independent playheads until a later research/decision card defines
an evidence-backed alignment model.

## Gate 4 — Release and publication

Rows are in dependency order; letters are stable labels, not sequence.

| Card | Outcome | Pts | Depends on | State |
|---|---|---:|---|---|
| FT-004A | Define release manifest, rights/provenance checklist, and target-capability law | 5 | 000B, 003D | icebox |
| FT-004F | Assemble, validate, and locally accept a release candidate | 5 | 004A | icebox |
| FT-004B | Generate target-ready export packages | 5 | 003C, 004A, 004F | icebox |
| FT-004G | Render YouTube video assets | 3 | 004A, 004B | icebox |
| FT-004C | Implement checkpointed SoundCloud direct-upload adapter | 5 | 004A, 004B | icebox |
| FT-004D | Implement resumable YouTube upload with privacy/audit state | 5 | 004A, 004B, 004G | icebox |
| FT-004E | Implement distributor/manual handoffs for Spotify, Bandcamp, and similar targets | 5 | 004A, 004B | icebox |

Gate outcome: an accepted local release can be published or handed off without
confusing export, upload, processing, and published states.

## Operational cards

| Card | Outcome | State |
|---|---|---|
| FT-OPS-001 | Run installed Rheos against the corrected card corpus and record count/list/find evidence | incoming |
| FT-OPS-002 | Reconcile ADR, design, process, and card statuses after review | incoming |
| FT-OPS-003 | Record independent Claude design/board review disposition | incoming |

These cards enter at `incoming` like any other. A card cannot be created already
`done`: `done` is reached by a Rheos transition, never asserted at creation.

`board.json` is intentionally not a committed acceptance artifact because the
current snapshot loses rich frontmatter. FT-OPS-001 recorded a local eta-mu/Rheos
1.1.1 run on 2026-07-28. Its observations are historical evidence, not a second
board implementation.

## Critical path

```text
FT-000A ─┬─> FT-000B ─┐
         └─> FT-000D ─┴─> FT-000C
                         ├─> FT-001A -> FT-001B -> FT-001C ─┬─> FT-001D -> FT-002A -> FT-002B
                         │                                  └─> FT-001E
                         └─> FT-003A -> FT-003B -> FT-003C -> FT-003D
                                                             -> FT-004A -> FT-004F -> FT-004B ─┬─> FT-004G -> FT-004D
                                                                                                 └─> FT-004C / FT-004E
```

Cross-gate edge not drawn above: **FT-002B also depends on FT-003A**, because
triage dispatches FT-003A's marker and clip commands.

This diagram shows principal chains only. Each card's `dependency` field is
authoritative, and several cards carry additional edges to the FT-000 foundation
cards that would make the drawing unreadable.

## Acceptance

- Contracts precede adapters.
- Native UI, playback, read-model, and topology choices are produced by FT-000D,
  not invented downstream.
- A target adapter cannot advance before local release/export semantics work.
- `done` means accepted for the card's scope, not merely merged or green.
- Live status, dependency resolution, transitions, WIP limits, and actionable work
  come from Rheos, not this static delivery map.
- Any missing board invariant must be implemented in Rheos upstream; Fork Tales
  does not create local validators or shadow board mechanics.

## Current first move

FT-OPS-001 records a local eta-mu/Rheos read of the corrected board. FT-000B and
FT-000D are the parallel product implementation fronts; query Rheos for live state
before acting.
