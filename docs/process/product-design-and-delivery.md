---
title: "Product Design and Delivery Process"
kind: process-policy
status: accepted
implements: [PROCESS.md]
operational-guide: docs/kanban-docs/AGENTS.md
created: "2026-07-27"
accepted: "2026-07-28"
---

# Product Design and Delivery Process

## Purpose

This policy defines how Fork Tales turns a broad product desire into researched,
architecturally governed, reviewable implementation work.

It follows the Epiphany separation of responsibilities:

- research preserves evidence and unresolved facts;
- ADRs decide architectural boundaries;
- designs describe intended product behavior within those boundaries;
- Rheos cards coordinate bounded delivery;
- implementation and tests provide review evidence;
- explicit acceptance closes the declared scope.

The board is not the architecture, and a merged implementation is not human
acceptance.

## Authority chain

Apply product guidance in this order:

1. `PROCESS.md` — evidence, epistemic tiers, receipts, verification, completion.
2. Accepted ADRs in `docs/adrs/` — durable architectural decisions.
3. Approved designs in `docs/designs/` — intended user experience and behavior.
4. Process policies in `docs/process/` — revisable delivery rules.
5. Rheos cards in `docs/kanban/` — bounded work and current coordination.
6. Implementation details and task-local instructions.

Board prose and delivery maps live under `docs/kanban-docs/`, outside Rheos's
card-scanning `tasksDir`.

A card may expose a design problem. It may not silently solve one by overriding an
ADR or design in code.

## Document kinds

### Research

Research belongs under `docs/research/` and records the question, scope, sources,
corpus observations, competing interpretations, constraints, unknowns, and
implications. Research is derived evidence; it does not decide architecture by
itself.

### ADR

An ADR belongs under `docs/adrs/` and records context, the decision, consequences,
risks, rejected alternatives, acceptance conditions, and links to research and
affected designs.

```text
proposed -> accepted -> superseded
         -> rejected
```

Only an accepted ADR is architecturally authoritative. Rejection and
supersession preserve the original record.

### Design

A design belongs under `docs/designs/` and records outcome, non-goals, workflows,
domain concepts, command/query expectations, interface behavior, failure
handling, accessibility, delivery slices, open questions, and required ADRs.

```text
open -> approved -> implemented
     -> rejected | superseded
```

A design cannot become approved while a required ADR remains proposed. Material
implementation discoveries return the design or ADR to review rather than being
hidden in a card.

### Board card

A Rheos card coordinates one bounded outcome. It links to governing authority,
dependencies, acceptance criteria, and expected evidence. Cards preserve progress
and review history but do not duplicate entire research reports or designs.

Only actual card Markdown belongs under `docs/kanban/{stories,epics,chores}`.
Rheos owns card collection, identity, parsing, transitions, comments, events,
writeback, WIP enforcement, and drift detection. Repository documents may describe
product expectations but may not establish a second operational board protocol.

## Lifecycle

```text
capture desire
  -> research facts and corpus reality
  -> propose ADR where boundaries are architectural
  -> draft design
  -> human review / acceptance
  -> decompose into Rheos epics and stories
  -> implement contracts before adapters
  -> review evidence
  -> document durable outcomes
  -> explicit acceptance
```

Research, ADR, design, board setup, implementation, review, and documentation may
be separate cards when material.

## Readiness gates

A product implementation story may enter `ready` only when:

- its outcome is bounded and at most 5 points;
- applicable ADRs are accepted;
- the governing design is approved or the card produces that design;
- dependencies are represented through Rheos;
- acceptance criteria are observable;
- expected tests, fixtures, or review evidence are named;
- open questions do not require the implementer to invent product policy.

Contracts and schemas precede adapters. Application services precede UI adapters.
Publication adapters depend on release, export, and credential boundaries.

The native-player work has an additional gate: FT-000D must choose and verify the
first Clojure/JVM UI, real audio playback backend, rebuildable read model, and
application topology before downstream cards can assume those choices.

## Rheos mechanics

- Rheos is the sole board implementation and validation authority.
- Repository-root config discovery through `openhax.kanban.json` is normal.
- `--tasks-dir` is only an explicit alternate-board override or diagnostic.
- Board reads, writes, comments, subtasks, and transitions use Rheos CLI, API, MCP,
  or UI operations.
- CI invokes eta-mu/Rheos directly.
- Do not add repository-local parsers, validators, migration scripts, writeback
  helpers, shadow FSMs, WIP checkers, or alternate board APIs.
- Do not copy current Rheos transition tables, parser rules, or WIP limits into
  repository code and treat the copy as authority.
- A harness without Rheos may inspect board files but must not mutate board state
  or claim validation.
- Missing or defective behavior is fixed upstream in `open-hax/eta-mu` /
  `@eta-mu/rheos`; it is not reimplemented in Fork Tales.
- `board.json` remains a generated diagnostic projection and is ignored by Git.

## Design review questions

A reviewer asks:

1. Does the design model the user's actual unit of value?
2. Does it preserve source material and provenance?
3. Are observed, derived, provisional, and accepted states distinguishable?
4. Can the smallest useful slice stand alone?
5. Are failure, unavailability, retry, and partial success represented?
6. Does every durable UI action have a command/application-boundary equivalent?
7. Are credentials and private operational data kept out of Git?
8. Can derived indexes and media artifacts be rebuilt?
9. Does the interface remain useful without future model automation?
10. Has external platform capability been verified rather than assumed?
11. Does the daily-driver gate include real audio evidence, not only fakes?
12. Are native UI and application topology choices owned by an explicit decision?

## Change handling

A material change discovered during implementation is classified as:

- **implementation detail** — within accepted design and card scope;
- **design revision** — changes user-visible behavior or domain semantics;
- **ADR revision** — changes architecture or authority;
- **new research need** — a material fact is unknown or unstable;
- **follow-up** — valuable but outside current acceptance scope.

Design or ADR revisions stop the affected slice until reviewed. Follow-ups become
new cards rather than hidden scope.

## Completion

A card enters `review` with inspectable artifacts and actual evidence. It enters
`document` when durable docs or acceptance evidence remain. It enters `done` only
when the declared outcome is accepted.

Completion records identify changed artifacts, verification actually run,
limitations, governing authority versions, follow-ups, Receipt River reference,
and accepting actor.

## Current media-workbench wave

The current authority set is:

- research: `docs/research/media-workbench-interface-and-publishing.md`;
- accepted ADR: `docs/adrs/adr-001-local-first-media-workbench.md`;
- approved design: `docs/designs/media-workbench-v1.md`;
- delivery map: `docs/kanban-docs/BOARD-BREAKDOWN.md`;
- board contract: `docs/kanban-docs/AGENTS.md`.

FT-000A, FT-OPS-002, and FT-OPS-003 are currently `incoming`. Their documents
preserve prior review, disposition, and verification evidence, but no Rheos
lifecycle has accepted those cards as `done`. FT-000B and FT-000D are currently
`breakdown`, not selectable `ready` work, because their declared FT-000A
dependency is still `incoming`. They may return to `ready` only after that
dependency is satisfied or explicitly revised through canonical authority.
Query Rheos for live state before acting. FT-000C and player cards remain
dependency-gated rather than blocked by unresolved product acceptance.
