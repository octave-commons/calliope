# Board Breakdown: Fork Tales Media Workbench

The board is organized as usable gates. Each gate must leave the application more
valuable as a daily driver; later publishing work may not delay local listening
and salvage.

ADR-001 is proposed and the design is open. Except for authority-producing cards,
implementation cards remain `incoming` or `accepted` until the ADR is accepted
and the design approved.

## Gate 0 — Authority and durable media decisions

| Card | Outcome | Pts | Depends on | Initial state |
|---|---|---:|---|---|
| FT-000A | Review and accept/reject ADR-001 and Media Workbench v1 design | 2 | — | review |
| FT-000B | Define Malli laws for playable refs, ratings, labels, clips, arrangements, workspaces, and releases | 5 | 000A | incoming |
| FT-000C | Define append-only studio events and rebuildable read projection | 5 | 000B | incoming |

Gate outcome: the application can represent durable user intent without treating
source audio, projections, or model suggestions as mutable truth.

## Gate 1 — Daily-driver player

| Card | Outcome | Pts | Depends on | Initial state |
|---|---|---:|---|---|
| FT-001A | Index playable media metadata and waveform-peak jobs | 5 | 000B, 000C | incoming |
| FT-001B | Implement playback resolver, persistent queue, resume, and failure isolation | 5 | 001A | incoming |
| FT-001C | Build persistent app shell and virtualized library browser | 5 | 001B | incoming |
| FT-001D | Add quick disposition, multidimensional ratings, labels, sorting, and explicit playlists | 5 | 000C, 001C | incoming |

Gate outcome: Fork Tales is useful as the everyday player for the corpus.

## Gate 2 — Focused curation

| Card | Outcome | Pts | Depends on | Initial state |
|---|---|---:|---|---|
| FT-002A | Add query-backed smart lists and saved workspaces | 5 | 001D | incoming |
| FT-002B | Add keyboard-first triage and classifier-overlay review | 5 | 001D, 002A | incoming |

Gate outcome: the corpus can be reviewed quickly without losing richer context.

## Gate 3 — Salvage and arrangement

| Card | Outcome | Pts | Depends on | Initial state |
|---|---|---:|---|---|
| FT-003A | Implement marker and clip commands with immutable source ranges | 5 | 000B, 000C, 001B | incoming |
| FT-003B | Build waveform salvage editor with loop, in/out, fades, and clip audition | 5 | 001C, 003A | incoming |
| FT-003C | Implement arrangement playback and deterministic audio export | 5 | 003A, 003B | incoming |
| FT-003D | Preserve derivation graph from render through clip, arrangement, and export | 3 | 003C | incoming |

Gate outcome: valuable spans can be recovered from globally poor renders and
assembled without destructive editing.

## Gate 4 — Release and publication

| Card | Outcome | Pts | Depends on | Initial state |
|---|---|---:|---|---|
| FT-004A | Define release manifest, rights/provenance checklist, and target capability law | 5 | 000B, 003D | icebox |
| FT-004B | Generate target-ready audio/artwork/metadata packages and YouTube video assets | 5 | 003C, 004A | icebox |
| FT-004C | Implement checkpointed SoundCloud direct-upload adapter | 5 | 004A, 004B | icebox |
| FT-004D | Implement resumable YouTube upload adapter with privacy/audit state | 5 | 004A, 004B | icebox |
| FT-004E | Implement distributor/manual handoff packages for Spotify, Bandcamp, and similar targets | 5 | 004A, 004B | icebox |

Gate outcome: an accepted local release can be published or handed off without
confusing export, upload, processing, and published states.

## Critical path

```text
FT-000A -> FT-000B -> FT-000C
                    -> FT-001A -> FT-001B -> FT-001C -> FT-001D
                                                    -> FT-002A -> FT-002B
                               -> FT-003A -> FT-003B -> FT-003C -> FT-003D
                                                                  -> FT-004A
                                                                  -> FT-004B
                                                                  -> target adapters
```

After FT-000C, playback indexing and clip-law work may proceed in parallel. The
waveform editor depends on a working player so editing is auditionable from the
first implementation.

## WIP and acceptance

- WIP limit: 2 `in_progress`, 1 `review`.
- No card above 5 points becomes `ready`.
- Contracts precede adapters.
- A target adapter cannot become ready before release/export semantics work
  locally.
- `done` means accepted for the card's declared scope, not merely merged or green.

## Current first move

Review FT-000A. Acceptance may approve ADR-001 as written, request revisions, or
reject it. Only after that disposition should FT-000B move toward `ready`.
