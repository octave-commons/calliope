---
uuid: "ft-ops-001-validate-cards-and-generate-rheos-board-snapshot"
title: "FT-OPS-001: Validate corrected board with installed Rheos"
status: incoming
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
- Read the board through Rheos alone, with no repository-local board tool.
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
- Every board read used in evidence comes from Rheos, and each Rheos invocation
  exits zero without a collect, parse, or identity error.
- `clojure -M:test` passes on the same tree.
- Actual tool version and output are preserved in a card comment or receipt.

## Verification

```bash
eta-mu --version
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
clojure -M:test
```

## Verification result

Executed locally on 2026-07-28 against the installed CLI, from the repository
root of a `design/media-workbench-v1` worktree, with no `--tasks-dir` flag.

Tool identity:

```text
$ eta-mu --version
1.1.1
$ which eta-mu
<user-toolchain>/bin/eta-mu
```

The board engine is the bundled `@eta-mu/rheos` at
`<user-toolchain>/packages/eta-mu/node_modules/@eta-mu/rheos/dist/cli.cjs`.
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

Repository contract, as executed on 2026-07-28:

```text
$ python3 scripts/validate_rheos_board.py
Rheos board valid: 27 cards (breakdown=4, done=3, icebox=6, incoming=11, ready=3)
$ clojure -M:test
Ran 23 tests containing 91 assertions.
0 failures, 0 errors.
```

> **Annotation (retained history, not a current command).** The
> `validate_rheos_board.py` transcript above is preserved because it is what
> actually ran on 2026-07-28. That script was a repository-local second board
> implementation and was deleted in commit `ee10c65` ("revert: remove parallel
> Rheos validator") under the ruling in `AGENTS.md` that Rheos is the sole
> implementation and operational authority for the board. Do not run, restore, or
> reimplement it. The current, reproducible verification surface for this card is
> the `eta-mu` block under **Verification** above; `clojure -M:test` is unchanged
> and still passes.

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
```

The `Ready: 2 / Done: 4` counts above remain reproducible today through
`eta-mu kanban count`. The second `validate_rheos_board.py` line that originally
followed this block has been dropped rather than annotated, because it restated
the same card totals that Rheos already reports.

All acceptance criteria as originally written passed on 2026-07-28. The criterion
"the repository validator passes" has since been replaced, because the tool it
named no longer exists and may not be recreated; the replacement criteria above
are stated in terms of what Rheos itself verifies.

Receipt River evidence:

- local execution record: `receipts.edn` event at `:ts "2026-07-28T20:33:40Z"`;
- corrected FSM/WIP adjudication: `receipts.edn` event at `:ts "2026-07-28T21:19:42Z"`.

## Recorded tool drift

1. The installed `promethean` FSM does not match the FSM previously documented in
   `docs/kanban-docs/AGENTS.md`. `blocked` is reachable only from `breakdown`,
   an undocumented `testing` state sits between `in_progress` and `review`, and
   `rejected` and `archived` exist as further states — `rejected` reachable from
   `accepted`, `breakdown`, `blocked`, `ready`, `todo`, `in_progress`, `review`,
   and `document`; `archived` reachable from every other state. The FSM declares
   15 transitions in total. This observation stands as recorded history, but the
   resolution originally claimed here does not: the board contract was **not**
   corrected to restate those transitions. `docs/kanban-docs/AGENTS.md` was
   subsequently reduced by 259 lines (commit `f204d95`, "docs: remove duplicated
   Rheos mechanics") and now deliberately documents no transition table at all,
   delegating the status machine to the installed Rheos version instead. Copying
   a transition table into this repository is now forbidden, so the correct way
   to learn the current transitions is to query Rheos, not to read this card.
2. The build gate sits on exactly one edge, the direct `in_progress -> review`
   transition, and its check spec runs three commands: `pnpm build`, `pnpm lint`,
   `pnpm test`. Fork Tales is a Clojure/JVM repository with no pnpm project, so
   the first command fails and that edge is rejected with
   ``transition rejected: Build gate failed: `pnpm build` exited with code 1``.
   The gate does **not** stop the CLI from reaching `done`: `in_progress ->
   testing` and `testing -> review` are both `always_allow`, so routing through
   `testing` bypasses it. Verified on a throwaway board copy — `ready -> todo ->
   in_progress -> testing -> review -> document -> done` via
   `eta-mu kanban frontmatter <uuid> status <state>` succeeded at every step,
   ending at `eta-mu kanban count` -> `Total tasks: 1 / Done: 1`.
3. `eta-mu kanban frontmatter` and `eta-mu kanban comment` rewrite the whole
   frontmatter block: keys are reordered, every scalar is re-quoted, a
   `write-id` field is injected, and the file's trailing newline is dropped. The
   last of these was rejected by the repository validator deleted in `ee10c65`;
   with that validator gone, a dropped trailing newline is now accepted silently
   and nothing in this repository detects it. Because of 3 — item 2
   is *not* a reason, since the gate is bypassable — this card's status change
   and this evidence section were written directly to card Markdown, which
   `docs/kanban-docs/AGENTS.md` already names as the durable board source.
4. The installed engine's own WIP limits are looser than the repository rule, but
   the relevant numbers are the `promethean` FSM's, not `default_fsm`'s.
   `openhax.kanban.json` selects `promethean`, whose limits are
   `in_progress=50`, `review=40`, `todo=75`, `ready=100`, `blocked=15`,
   `breakdown=50`, `testing=40`, `accepted=40`, `document=40`, `done=500`, and
   `9999` for `icebox`/`incoming`/`rejected`/`archived`. The `in_progress=10` /
   `review=5` pair belongs to `default_fsm` and does not apply here. Confirmed
   empirically: 12 sandbox cards moved to `in_progress` with zero rejections,
   which a limit of 10 would have refused.
5. **Open gap: the stricter 2/1 WIP rule is currently enforced by nothing.** The
   repository's "at most 2 `in_progress`, at most 1 `review`" rule was enforced
   only by the validator deleted in `ee10c65`, and the rule text itself was
   removed from `docs/kanban-docs/AGENTS.md` in `f204d95`. The installed
   `promethean` FSM permits `in_progress=50` / `review=40`, so today nothing —
   not Rheos, not CI, not this repository — stops the board from exceeding 2/1.
   This is not a claim that the limit holds; it is a statement that the limit is
   unenforced. Restoring it is an upstream change to Rheos/FSM configuration, not
   permission to add a local WIP checker. No upstream `@eta-mu/rheos` issue has
   been filed for it yet.
