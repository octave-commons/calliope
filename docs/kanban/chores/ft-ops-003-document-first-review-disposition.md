---
uuid: "ft-ops-003-record-first-independent-design-review-disposition"
title: "FT-OPS-003: Record first independent design review disposition"
status: done
type: chore
priority: P1
phase: 0
owner: claude-local
points: 2
labels: review, media-workbench, governance
category: chores
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
---

# FT-OPS-003: Record first independent design review disposition

## Outcome

The first independent review disposition is preserved with design approval,
board-mechanics objections, accepted findings, and their resolution.

## Scope

- Record the independent review identity and explicit disposition.
- Separate approval of design authority from requested board-mechanics changes.
- Preserve each accepted finding and the repository change that resolved it.
- Keep review evidence distinct from later implementation acceptance.

## Disposition

**Design authority: APPROVE. Board mechanics: REQUEST CHANGES.**

Review: `https://github.com/octave-commons/fork_tales_v2/pull/3#pullrequestreview-4793817603`

## Non-goals

- Claiming the independent review approved unimplemented player or publication work.
- Dismissing data-integrity findings as stylistic.
- Replacing the durable review record with a generated board snapshot.

## Accepted findings

- Rheos recursively ingests every Markdown file below `tasksDir`; prose created
  phantom cards.
- `id:` was inert; explicit `uuid:` is operational identity.
- Dependency and epic references must use that same UUID namespace.
- Current snapshots are lossy and cannot be acceptance authority.
- Decomposed epic statuses, blocked-state documentation, empty dependencies,
  ignored event paths, and trailing newlines required correction.
- Native UI, real playback, read-model, and application-boundary decisions lacked
  explicit ownership.

## Acceptance criteria

- Prose is outside `docs/kanban/` and the board contains only actual cards.
- All cards use explicit UUID identity and resolvable relationships.
- Snapshot output is ignored and documented as diagnostic.
- The repository validator enforces rich card invariants.
- FT-000D owns native UI, real audio backend, read model, and application topology.
- Gate 1 requires real corpus MP3 evidence.
- FT-003B limits v1 sibling comparison to manual independent playheads.

## Verification

Review `4793817603`, the corrected card corpus, and Repository Contracts provide
the durable evidence. Run `python3 scripts/validate_rheos_board.py` and
`clojure -M:test`; confirm the accepted findings remain represented in the board
contract, authority documents, and Receipt River.
