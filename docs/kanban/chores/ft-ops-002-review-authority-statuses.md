---
uuid: "ft-ops-002-reconcile-adr-and-design-statuses-after-review"
title: "FT-OPS-002: Reconcile ADR and design statuses after review"
status: done
type: chore
priority: P0
phase: 0
owner: gpt-5.6-thinking
points: 1
labels: adr, design-review, documentation
category: chores
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000a-review-and-accept-or-revise-media-workbench-authority"]
---

# FT-OPS-002: Reconcile ADR and design statuses after review

## Outcome

The authority documents and board now reflect the same explicit disposition.

## Scope

- Reconcile ADR-001, Media Workbench v1, and the delivery process statuses.
- Align FT-000A, FT-000B, FT-000C, and FT-000D with the accepted authority state.
- Keep board prose outside `tasksDir` and preserve explicit UUID relationships.
- Record the reconciliation through Receipt River.

## Non-goals

- Accepting implementation work that has not been executed or reviewed.
- Advancing dependency-gated cards without their prerequisites.
- Treating generated board snapshots as durable authority.

## Acceptance criteria

- ADR-001 is `accepted` with Err as decider.
- Media Workbench v1 is `approved`.
- The product-delivery process is `accepted`.
- FT-000A is `done` with the acceptance basis.
- FT-000B and FT-000D are `ready`.
- FT-000C and player work remain dependency-gated.
- Board prose resides outside `docs/kanban/`.

## Verification

Run `python3 scripts/validate_rheos_board.py` and `clojure -M:test`. Confirm the
accepted authority files resolve, the reconciled card statuses match their
recorded dependencies, and Receipt River preserves the review disposition.
