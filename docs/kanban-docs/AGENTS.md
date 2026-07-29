# Fork Tales Rheos Board — Agent Contract

The Fork Tales development board is managed by eta-mu/Rheos. Rheos owns board
collection, parsing, identity, transitions, WIP enforcement, comments, events,
writeback, and drift detection. This repository must not implement those mechanics
a second time.

`PROCESS.md` governs evidence, receipts, and completion. Accepted ADRs and approved
designs govern architecture and product behavior. Rheos cards coordinate bounded
work and never silently override those authorities.

## Layout

```text
openhax.kanban.json          Rheos discovery and FSM configuration

docs/kanban/                configured Rheos tasksDir
  stories/
  epics/
  chores/

docs/kanban-docs/           product/process prose outside tasksDir
  README.md
  AGENTS.md
  BOARD-BREAKDOWN.md
```

The operational event ledger and generated board projections are Rheos artifacts.
Generated snapshots are diagnostic and are not hand-edited or promoted over Rheos.

## Sole-tool rule

- **Use Rheos for every board operation.**
- Do not create repository-local board parsers, validators, migration scripts,
  writeback helpers, shadow state machines, WIP checkers, or alternate APIs.
- Do not copy Rheos transition tables or limits into repository code and treat the
  copy as authoritative.
- CI invokes eta-mu/Rheos directly.
- Agents use Rheos CLI, API, MCP, or UI operations for reads, writes, comments,
  subtasks, and status transitions.
- A harness without Rheos may inspect files but must not mutate board state or
  claim that the board is valid.
- When Rheos lacks a required capability or has a defect, the work is blocked here
  until the capability is fixed upstream in `open-hax/eta-mu` / `@eta-mu/rheos`.
  A local substitute is not an acceptable workaround.

This rule exists specifically to prevent the board implementation from forking and
drifting across repositories.

## CLI

Run from the repository root so eta-mu/Rheos discovers `openhax.kanban.json`.

```bash
eta-mu --version
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
```

Use `--tasks-dir` only for an intentional alternate board or a configuration
diagnostic. Normal operations rely on repository config discovery.

The exact command surface, status machine, parser behavior, WIP limits, and event
semantics come from the installed Rheos version. Query Rheos rather than copying
those values into this repository.

## Board content

Fork Tales cards should remain bounded and link to governing authority where
applicable. Product expectations include:

- stable task identity;
- a bounded outcome and explicit non-goals;
- dependencies expressed in Rheos’s identity model;
- observable acceptance and verification evidence;
- explicit ownership when work is active;
- preserved progress, review, and failure history.

These are product/process expectations. Their operational representation and
enforcement belong in Rheos. When the current Rheos schema cannot represent one,
change Rheos first rather than adding an out-of-band field checker here.

## Known eta-mu 1.1.1 findings

A local verification on 2026-07-28 found differences between assumptions in this
repository and eta-mu/Rheos 1.1.1, including writeback formatting and transition
behavior. Those observations remain historical evidence in FT-OPS-001 and
`receipts.edn`; they are not a license to establish Git edits or a repository
script as a second board engine.

Until an upstream defect is fixed, affected Rheos mutations are unavailable or
blocked. Do not bypass the tool and then claim Rheos-managed state.

## Product sequencing rules

These rules describe Fork Tales delivery intent, not a replacement FSM:

1. Contracts precede adapters.
2. Application services precede native views.
3. Release contracts precede publication adapters.
4. Native UI, playback, read-model, and topology choices are produced by FT-000D,
   not invented downstream.
5. A target adapter cannot advance before local release/export semantics work.
6. `done` requires accepted evidence for the declared scope; a merge or green CI
   result alone is insufficient.
7. Work, render, clip, arrangement, export, release, and publication identities
   remain distinct.
8. Every substantive board wave receives a Receipt River record.
9. The first UI is native Clojure/JVM without an embedded browser.
10. The daily-driver milestone requires real corpus audio evidence.

## How an agent picks work

1. Use Rheos to read the board and its current actionable view.
2. Confirm governing ADR/design status and local capability.
3. Use Rheos to claim and transition the selected card before acting.
4. Record material evidence as it occurs through the supported Rheos surface.
5. Use Rheos to return insufficiently specified work to the appropriate planning or
   blocked state.
6. Only accepted work reaches `done`.

## Current phase map

| Phase | Theme | State |
|---|---|---|
| 0 | Accepted authority, laws, and runtime decisions | active |
| 1 | Native daily-driver library and playback | dependency-blocked |
| 2 | Curation, smart lists, and workspaces | incoming |
| 3 | Markers, clips, arrangements, and export | incoming |
| 4 | Release and publication adapters | icebox until local release works |

The current product gates and critical path live in `BOARD-BREAKDOWN.md`; live
board state comes from Rheos.
