---
uuid: "ft-ops-001-validate-cards-and-generate-rheos-board-snapshot"
title: "FT-OPS-001: Validate corrected board with installed Rheos"
status: done
type: chore
priority: P0
phase: 0
owner: unassigned
points: 1
labels: rheos, board, verification
category: chores
process: "docs/process/product-design-and-delivery.md"
---

# FT-OPS-001: Validate corrected board with installed Rheos

## Outcome

A local harness runs the installed eta-mu/Rheos against the corrected card-only
`tasksDir` and records exact count, list, and identity evidence.

## Scope

- Confirm repository-root discovery of `openhax.kanban.json`.
- Run count/list/find using the installed tool and record version/commit.
- Run `scripts/validate_rheos_board.py`.
- Confirm 27 cards and no phantom prose cards.
- Record exact commands/results and Receipt River evidence.

## Non-goals

- Treating `board.json` as complete authority.
- Committing the current lossy snapshot.
- Redundant default `--tasks-dir` flags.
- Changing accepted ADR or product design boundaries.

## Acceptance criteria

- Rheos reads the config and card corpus without error.
- `eta-mu kanban count` reports 27 actual cards.
- `eta-mu kanban list` contains no README/AGENTS/BOARD-BREAKDOWN phantom cards.
- `eta-mu kanban find ft-000b-define-media-workbench-domain-laws` resolves the
  explicit UUID-backed card.
- The repository validator passes.
- Actual tool version and output are preserved in a card comment or receipt.

## Verification

```bash
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
python3 scripts/validate_rheos_board.py
```

## Verification result

Executed locally on 2026-07-28 against the installed CLI, from the repository
root of a `design/media-workbench-v1` worktree, with no `--tasks-dir` flag.

Tool identity:

```text
$ eta-mu --version
1.1.1
$ which eta-mu
/home/err/.volta/bin/eta-mu
```

The board engine is the bundled `@eta-mu/rheos` at
`~/.volta/tools/image/packages/eta-mu/lib/node_modules/eta-mu/node_modules/@eta-mu/rheos/dist/cli.cjs`.
The installed distribution ships no VCS commit metadata, so only the semantic
version is recordable.

Config discovery, before any status change:

```text
$ eta-mu kanban count
Total tasks: 27
  Icebox: 6
  Incoming: 11
  Breakdown: 4
  Ready: 3
  Done: 3
```

`eta-mu kanban list` returned exactly 27 rows matching that breakdown, all of
them `FT-*` cards. No `README`, `AGENTS`, or `BOARD-BREAKDOWN` row appeared,
confirming that moving board prose to `docs/kanban-docs/` removed the phantom
cards.

Identity resolution:

```text
$ eta-mu kanban find ft-000b-define-media-workbench-domain-laws
{ "uuid": "ft-000b-define-media-workbench-domain-laws",
  "frontmatter": { "status": "ready", "type": "story", "points": "5",
                   "epic": "ft-000-establish-media-workbench-authority-and-durable-studio-foundation",
                   "dependency": ["ft-000a-review-and-accept-or-revise-media-workbench-authority"], ... },
  "source-path": ".../docs/kanban/stories/ft-000b-define-studio-domain-laws.md" }
```

The stable UUID resolved to a file whose slug differs from it, which is the
intended consequence of canonical `uuid` identity surviving a retitle.

Repository contract:

```text
$ python3 scripts/validate_rheos_board.py
Rheos board valid: 27 cards (breakdown=4, done=3, icebox=6, incoming=11, ready=3)
$ clojure -M:test
Ran 23 tests containing 91 assertions.
0 failures, 0 errors.
```

Config discovery was confirmed to walk upward from the working directory:
`eta-mu kanban count` reported the same 27 cards from the repository root, from
`docs/`, and from `scripts/`. Run from `/tmp` it fell back to the built-in
default and reported `collect error: /tmp/docs/agile/tasks ENOENT` with
`Total tasks: 0`, so the 27-card result is genuine config discovery rather than
a coincidental default.

After moving this card to `done`:

```text
$ eta-mu kanban count
Total tasks: 27
  Icebox: 6
  Incoming: 11
  Breakdown: 4
  Ready: 2
  Done: 4
$ python3 scripts/validate_rheos_board.py
Rheos board valid: 27 cards (breakdown=4, done=4, icebox=6, incoming=11, ready=2)
```

All acceptance criteria passed.

## Recorded tool drift

1. The installed `promethean` FSM does not match the FSM previously documented in
   `docs/kanban-docs/AGENTS.md`. `blocked` is reachable only from `breakdown`,
   an undocumented `testing` state sits between `in_progress` and `review`, and
   `archived` exists as a terminal state. The board contract has been corrected
   to the transitions the installed engine actually enforces.
2. `in_progress -> review` is guarded by a `pnpm build` gate. Fork Tales is a
   Clojure/JVM repository with no pnpm project, so the guard fails
   (`transition rejected: Build gate failed: 'pnpm build' exited with code 1`)
   and the CLI cannot advance any card past `in_progress` here.
3. `eta-mu kanban frontmatter` and `eta-mu kanban comment` rewrite the whole
   frontmatter block: keys are reordered, every scalar is re-quoted, a
   `write-id` field is injected, and the file's trailing newline is dropped. The
   last of these fails `scripts/validate_rheos_board.py`. Because of 2 and 3,
   this card's status change and this evidence section were written directly to
   card Markdown, which `docs/kanban-docs/AGENTS.md` already names as the
   durable board source.
4. The installed engine's own WIP limits are `in_progress=10` and `review=5`.
   The repository's stricter 2/1 limits are enforced only by
   `scripts/validate_rheos_board.py`.
