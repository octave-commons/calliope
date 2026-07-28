# Fork Tales Rheos Board — Agent Contract

Markdown-backed board managed by `eta-mu kanban` / Rheos. Every card is one
Markdown file with YAML frontmatter.

`PROCESS.md` governs evidence, receipts, and completion. Accepted ADRs in
`docs/adrs/` are architecturally authoritative. Approved designs in
`docs/designs/` govern product behavior. Cards coordinate bounded work and never
silently override those authorities.

## Layout

```text
docs/kanban/
  stories/            implementable product and engineering slices
  epics/              outcome groups; not implemented directly
  chores/             maintenance and operational work
  BOARD-BREAKDOWN.md  gates, dependencies, critical path
  board.json          generated snapshot; never hand-edit
  AGENTS.md           this contract
```

The high-churn Rheos event ledger under `docs/kanban/.events/` is operational and
ignored by Git. Card files and append-oriented comments/progress remain durable.

## CLI

Run from the repository root, where `openhax.kanban.json` lives:

```bash
eta-mu kanban count --tasks-dir docs/kanban
eta-mu kanban list --tasks-dir docs/kanban
eta-mu kanban find <slug> --tasks-dir docs/kanban
eta-mu kanban frontmatter <slug> status in_progress --tasks-dir docs/kanban
eta-mu kanban comment <slug> "Progress note" --tasks-dir docs/kanban
eta-mu kanban board snapshot --tasks-dir docs/kanban --out docs/kanban/board.json
```

A remote harness that cannot run Rheos may create or update card Markdown through
the repository API, preserving the same schema and append-oriented history. It
must explicitly state that the generated snapshot and Rheos validation were not
run.

## Frontmatter contract

```yml
id: "uuid"
title: "FT-001A: Imperative bounded outcome"
status: incoming
type: story
priority: P0
phase: 1
epic: "epic uuid"
parent: "optional parent uuid"
research: "docs/research/...md"
adr: "docs/adrs/...md"
design: "docs/designs/...md"
process: "docs/process/...md"
points: 3
labels: [player, phase-1]
category: stories
dependency: ["card uuid"]
```

Required before leaving `incoming`:

- stable ID, title, type, priority, phase, category, and honest points;
- bounded outcome, scope, non-goals, and acceptance criteria;
- dependencies and governing research/ADR/design/process links;
- expected verification evidence;
- current owner or explicit unassigned state.

## Status FSM

```text
icebox -> incoming -> accepted -> breakdown -> ready -> todo
       -> in_progress -> review -> document -> done
```

`rejected` is reachable from any state.

- **icebox** — real work intentionally deferred.
- **incoming** — captured but not yet fully triaged.
- **accepted** — worth planning; authority and rough dependencies are known.
- **breakdown** — being decomposed, or a terminal decomposed parent.
- **ready** — another qualified actor can start without inventing material scope.
- **todo** — selected for the execution queue.
- **in_progress** — one actor owns a bounded active commitment.
- **review** — result and evidence await evaluation.
- **document** — durable documentation/acceptance evidence is being completed.
- **done** — accepted for the card's declared scope.
- **rejected** — deliberately not pursued in the stated form.

States describe process position, not truth. A merged PR or green check does not
make a card done without the required acceptance record.

## Hard rules

1. **No card over 5 points may be `ready`.** Split it. Children name `parent`; the
   decomposed parent remains `breakdown` with label `decomposed`.
2. **Points are honest estimates.** Scale: 1 = an hour or two, 2–3 = roughly half
   a day to a day, 5 = a couple of days, 8/13 = must be decomposed.
3. **Contracts precede adapters.** Law/schema cards precede services; services
   precede UI adapters; release contracts precede publication adapters.
4. **ADRs and designs gate implementation.** A story that implements a proposed
   ADR or open design cannot become `ready` unless its outcome is to produce or
   review that authority document.
5. **Append, do not rewrite history.** Record progress, review dispositions,
   scope changes, and failures in comments/progress sections.
6. **`board.json` is generated.** Never edit it manually.
7. **WIP limits:** at most 2 cards `in_progress` and 1 card `review`, unless an
   explicit process experiment records another limit.
8. **Partial-value media is first-class.** No card may collapse work, render,
   clip, arrangement, and release identity for implementation convenience.
9. **Publication claims are target-specific.** Export, manual handoff, upload,
   processing, and published states remain distinct.
10. **Every substantive board wave receives one Receipt River record** under
    `PROCESS.md`.

## How an agent picks work

1. List the board and filter `ready`.
2. Discard cards with unmet dependencies.
3. Order by priority, phase, then smallest points.
4. Confirm the governing ADR/design status.
5. Move the card through `todo` and `in_progress` before acting.
6. Record material observations and evidence as they occur.
7. If scope expands or authority is unclear, return to `breakdown` rather than
   inventing policy.
8. Move through `review` and `document`; only accepted work reaches `done`.

## Phase map

| Phase | Theme | State |
|---|---|---|
| 0 | Design authority and board foundation | active |
| 1 | Daily-driver library and playback | blocked by ADR/design acceptance |
| 2 | Curation, smart lists, and workspaces | incoming |
| 3 | Markers, clips, arrangements, and export | incoming |
| 4 | Release and publication adapters | icebox until local release works |

The current gates and critical path live in `BOARD-BREAKDOWN.md`.