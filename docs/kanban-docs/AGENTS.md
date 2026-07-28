# Fork Tales Rheos Board — Agent Contract

Markdown-backed development board managed by eta-mu/Rheos. Every Markdown file
under the configured `tasksDir` is interpreted as a card, so prose is kept outside
that directory.

`PROCESS.md` governs evidence, receipts, and completion. Accepted ADRs and approved
designs govern architecture and product behavior. Cards coordinate bounded work
and never silently override those authorities.

## Layout

```text
openhax.kanban.json          board discovery and FSM configuration

docs/kanban/                Rheos tasksDir: cards only
  stories/
  epics/
  chores/

docs/kanban-docs/           prose outside the card scanner
  README.md
  AGENTS.md
  BOARD-BREAKDOWN.md
```

The operational `.events/` directory and lossy `board.json` snapshot are ignored
by Git. Durable task changes and comments are written to card Markdown.

## CLI

Run from the repository root. Rheos discovers `openhax.kanban.json`; its
`tasksDir` is the normal board-location authority.

```bash
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
python3 scripts/validate_rheos_board.py
```

Use `--tasks-dir` only to operate a different board or diagnose configuration
discovery.

The current `board snapshot` is useful for display diagnostics but discards rich
frontmatter fields. It is not committed and is not dependency or acceptance
authority.

## Frontmatter contract

```yml
uuid: "ft-001a-index-playable-media-metadata-and-waveform-jobs"
title: "FT-001A: Imperative bounded outcome"
status: incoming
type: story
priority: P0
phase: 1
epic: "ft-001-ship-a-daily-driver-library-and-player"
parent: "optional-parent-uuid"
research: "docs/research/...md"
adr: "docs/adrs/...md"
design: "docs/designs/...md"
process: "docs/process/...md"
points: 3
labels: player, phase-1
category: stories
dependency: ["another-card-uuid"]
```

Rules:

- `uuid` is explicit, stable, and canonical. Retitling a card does not change it.
- `epic`, `parent`, and `dependency` reference the same UUID namespace.
- `epic` names outcome-group membership. `parent` is reserved for direct task
  decomposition.
- Omit `dependency` when there are no dependencies; do not write `dependency: []`.
- Labels use a comma-separated scalar because the current Rheos snapshot parser
  does not preserve YAML flow-list brackets correctly.
- Rich fields remain durable in Markdown even when the generated snapshot omits
  them.

Required before leaving `incoming`:

- stable UUID, title, type, priority, phase, category, and honest points;
- bounded outcome, scope, non-goals, and acceptance criteria;
- resolvable dependencies and governing authority links;
- expected verification evidence;
- current owner or explicit `unassigned`.

## Status FSM

```text
icebox -> incoming -> accepted -> breakdown -> ready -> todo
       -> in_progress -> blocked -> review -> document -> done
```

`rejected` is reachable from any state.

- **icebox** — real work intentionally deferred.
- **incoming** — captured but not fully triaged.
- **accepted** — worth planning; authority and rough dependencies are known.
- **breakdown** — being decomposed or a decomposed parent.
- **ready** — another qualified actor can begin without inventing scope.
- **todo** — selected for execution.
- **in_progress** — one actor owns an active bounded commitment.
- **blocked** — started or selected but unable to proceed on named evidence.
- **review** — result and evidence await evaluation.
- **document** — durable documentation or acceptance evidence is being completed.
- **done** — accepted for the card's declared scope.
- **rejected** — deliberately not pursued in the stated form.

States describe process position, not truth. A merged PR or green check does not
make a card done without acceptance evidence.

## Hard rules

1. No card over 5 points may be `ready`.
2. Active decomposed parents remain `breakdown`; deliberately deferred decomposed
   epics may remain `icebox`.
3. Contracts precede adapters. Application services precede native views. Release
   contracts precede publication adapters.
4. Proposed ADRs and open designs gate implementation. Accepted ADR-001 and the
   approved v1 design now permit their explicitly dependent cards to advance.
5. Append progress, review dispositions, scope changes, and failures; do not erase
   history.
6. `board.json` is lossy diagnostic output, never hand-edited or authoritative.
7. WIP limit: at most 2 `in_progress` and 1 `review` unless an explicit process
   experiment changes it.
8. Work, render, clip, arrangement, export, and release identities may not be
   collapsed for implementation convenience.
9. Export, handoff, upload, processing, and published states remain distinct.
10. Every substantive board wave receives one Receipt River record.
11. The first UI is native Clojure/JVM without an embedded browser.
12. A daily-driver milestone requires real corpus audio evidence, not only fake
    media fixtures.

## How an agent picks work

1. Run Rheos list and `scripts/validate_rheos_board.py`.
2. Filter `ready` cards and discard cards with unmet dependencies.
3. Order by priority, phase, then smallest points.
4. Confirm governing ADR/design status and local capability.
5. Move the card through `todo` and `in_progress` before acting.
6. Record material evidence as it occurs.
7. Return to `breakdown` or `blocked` when scope or evidence is insufficient.
8. Move through `review` and `document`; only accepted work reaches `done`.

## Current phase map

| Phase | Theme | State |
|---|---|---|
| 0 | Accepted authority, laws, and runtime decisions | active |
| 1 | Native daily-driver library and playback | dependency-blocked |
| 2 | Curation, smart lists, and workspaces | incoming |
| 3 | Markers, clips, arrangements, and export | incoming |
| 4 | Release and publication adapters | icebox until local release works |

The current gates and critical path live in `BOARD-BREAKDOWN.md`.
