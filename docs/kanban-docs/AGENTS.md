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
discovery. Config discovery walks upward from the working directory, so read
commands also work from `docs/` or `scripts/`. Run from a path outside the
repository and the tool silently falls back to its built-in default and reports
zero tasks.

The current `board snapshot` is useful for display diagnostics but discards rich
frontmatter fields. It is not committed and is not dependency or acceptance
authority.

### Write-path caution

Verified against eta-mu 1.1.1: `eta-mu kanban frontmatter` and
`eta-mu kanban comment` rewrite the entire frontmatter block. They reorder keys,
re-quote every scalar, inject a `write-id` field, and drop the file's trailing
newline, which fails `scripts/validate_rheos_board.py`. Until that is fixed
upstream, edit card Markdown directly for status changes and durable notes, then
re-run `eta-mu kanban count` and the validator to confirm the board still reads.
Treat the CLI as the authoritative *reader* and Git as the authoritative
*writer*.

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

`openhax.kanban.json` selects the `promethean` FSM. These are the transitions the
installed engine actually enforces, read from eta-mu 1.1.1 and confirmed by
rejected probe transitions:

```text
icebox      -> incoming
incoming    -> icebox | accepted
accepted    -> breakdown | incoming
breakdown   -> ready | accepted | blocked
blocked     -> breakdown | ready
ready       -> todo | breakdown
todo        -> in_progress            (WIP check)
in_progress -> testing | todo | breakdown
in_progress -> review                 (build gate; the ONLY gated edge)
testing     -> review | in_progress | todo
review      -> document | in_progress | todo
document    -> done | review
done        -> icebox | review
```

`rejected` is reachable from `accepted`, `breakdown`, `blocked`, `ready`, `todo`,
`in_progress`, `review`, and `document`. `archived` is reachable from every other
state.

- **icebox** — real work intentionally deferred.
- **incoming** — captured but not fully triaged.
- **accepted** — worth planning; authority and rough dependencies are known.
- **breakdown** — being decomposed or a decomposed parent.
- **blocked** — planned work that cannot proceed on named evidence. The engine
  only allows entering it from `breakdown`, so a stalled `in_progress` card
  returns to `breakdown` first.
- **ready** — another qualified actor can begin without inventing scope.
- **todo** — selected for execution.
- **in_progress** — one actor owns an active bounded commitment.
- **testing** — engine-supported verification hop between `in_progress` and
  `review`. Fork Tales does not currently use it, but it is the ungated route
  around the `in_progress -> review` build gate.
- **review** — result and evidence await evaluation.
- **document** — durable documentation or acceptance evidence is being completed.
- **done** — accepted for the card's declared scope.
- **rejected** — deliberately not pursued in the stated form.
- **archived** — removed from active board consideration.

States describe process position, not truth. A merged PR or green check does not
make a card done without acceptance evidence.

The engine's build gate sits on exactly one edge: the direct
`in_progress -> review` transition. Its check spec runs `pnpm build`,
`pnpm lint`, and `pnpm test`; Fork Tales is a Clojure/JVM repository with no
pnpm project, so the first command fails and the CLI rejects that edge with
``transition rejected: Build gate failed: `pnpm build` exited with code 1``.

That gate does **not** prevent the CLI from reaching `done`. `in_progress ->
testing` and `testing -> review` are both `always_allow`, so the gate is
bypassable by routing through `testing`. Verified on a throwaway board copy with
`eta-mu kanban frontmatter <uuid> status <state>`: `ready -> todo ->
in_progress -> testing -> review -> document -> done` succeeded at every step.
Do not read the gate as a hard stop; it only makes the shortcut edge unusable.

Fork Tales still moves cards by editing card Markdown, because the CLI write path
corrupts the card contract (see the write-path caution below) — not because the
build gate blocks it. After any status edit, re-run `eta-mu kanban count` plus
`scripts/validate_rheos_board.py`. The governing evidence gate for this
repository is `clojure -M:test` and the Repository Contracts workflow, not
`pnpm build`.

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
   experiment changes it. This is a repository rule enforced by
   `scripts/validate_rheos_board.py`; the installed engine's own limits are far
   looser and must not be treated as permission. The `promethean` FSM this
   repository selects sets `in_progress=50`, `review=40`, `todo=75`,
   `ready=100`, `blocked=15`, `breakdown=50`, `testing=40`, `accepted=40`,
   `document=40`, `done=500`, and `9999` for `icebox`/`incoming`/`rejected`/
   `archived`. (The commonly quoted `in_progress=10`/`review=5` pair belongs to
   the engine's `default_fsm`, which this repository does not use.)
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
7. Return to `breakdown` when scope or evidence is insufficient, and to `blocked`
   from there when the obstruction is named and external.
8. Move through `review` and `document`; only accepted work reaches `done`.
9. Confirm every status change with `eta-mu kanban count` and
   `scripts/validate_rheos_board.py`.

## Current phase map

| Phase | Theme | State |
|---|---|---|
| 0 | Accepted authority, laws, and runtime decisions | active |
| 1 | Native daily-driver library and playback | dependency-blocked |
| 2 | Curation, smart lists, and workspaces | incoming |
| 3 | Markers, clips, arrangements, and export | incoming |
| 4 | Release and publication adapters | icebox until local release works |

The current gates and critical path live in `BOARD-BREAKDOWN.md`.
