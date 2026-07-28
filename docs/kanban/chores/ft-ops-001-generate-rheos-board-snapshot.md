---
uuid: "ft-ops-001-validate-cards-and-generate-rheos-board-snapshot"
title: "FT-OPS-001: Validate corrected board with installed Rheos"
status: ready
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
