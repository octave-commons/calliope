---
title: "Product Design and Delivery Process"
kind: process-policy
status: proposed
implements: [PROCESS.md]
operational-guide: docs/kanban/AGENTS.md
created: "2026-07-27"
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

The board is not the architecture and a merged implementation is not human
acceptance.

## Authority chain

Apply product guidance in this order:

1. `PROCESS.md` — evidence, epistemic tiers, receipts, verification, completion.
2. Accepted ADRs in `docs/adrs/` — durable architectural decisions.
3. Approved designs in `docs/designs/` — intended user experience and behavior.
4. Process policies in `docs/process/` — revisable delivery rules.
5. Rheos board cards in `docs/kanban/` — bounded work and current coordination.
6. Implementation details and task-local instructions.

A card may expose a design problem. It may not silently solve one by overriding an
ADR or design in code.

## Document kinds

### Research

Research belongs under `docs/research/` and records:

- the question and scope;
- source evidence and access dates where material;
- corpus observations;
- competing interpretations;
- constraints and unknowns;
- implications for a decision or design.

Research is derived evidence. It does not decide architecture by itself.

### ADR

An ADR belongs under `docs/adrs/` and records:

- context and forces;
- the decision;
- consequences and risks;
- rejected alternatives;
- acceptance conditions;
- links to grounding research and affected designs.

Status vocabulary:

```text
proposed -> accepted -> superseded
         -> rejected
```

Only an accepted ADR is architecturally authoritative. Rejection and
supersession preserve the original decision record.

### Design

A design belongs under `docs/designs/` and records:

- outcome and non-goals;
- user workflows and interaction model;
- domain concepts and state boundaries;
- command/query expectations;
- interface behavior and failure handling;
- accessibility;
- delivery slices;
- open questions and required ADRs.

Status vocabulary:

```text
open -> approved -> implemented
     -> rejected | superseded
```

A design cannot become `approved` while a required ADR remains proposed.
Implementation may discover new design evidence; material changes return the
design or ADR to review rather than being hidden in a card.

### Board card

A Rheos card coordinates one bounded outcome. It links to the governing ADR,
design, research, dependencies, acceptance criteria, and expected evidence.

Cards preserve implementation history through comments/progress records. They do
not copy entire research reports or designs into card bodies.

## Lifecycle

```text
capture desire
  -> research facts and corpus reality
  -> propose ADR where boundaries are architectural
  -> draft design
  -> human review / acceptance of ADR and design
  -> decompose into Rheos epics and stories
  -> implement contracts before adapters
  -> review evidence
  -> document durable outcomes
  -> explicit acceptance
```

Research, ADR, design, board setup, implementation, review, and documentation may
be separate cards when the work is material.

## Readiness gates

A product implementation story may enter `ready` only when:

- its outcome is bounded and at most 5 points;
- applicable ADRs are accepted;
- the governing design is approved or the card explicitly produces that design;
- dependencies are explicit;
- acceptance criteria are observable;
- expected tests, fixtures, or review evidence are named;
- open questions do not require the implementer to invent product policy.

Contract and schema stories precede runtime adapters. Runtime services precede UI
adapters. Publication adapters depend on release and credential boundaries, not
only on a button design.

## Design review questions

A reviewer should ask:

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

## Change handling

A material change discovered during implementation is recorded on the card and
classified:

- **implementation detail** — remains within accepted design and card scope;
- **design revision** — changes user-visible behavior or domain semantics;
- **ADR revision** — changes an architectural boundary or authority split;
- **new research need** — material fact is unknown or unstable;
- **follow-up** — valuable but outside the current acceptance scope.

Design/ADR revisions stop the affected implementation slice until reviewed.
Follow-ups become new cards; they are not smuggled into the current card.

## Completion

A card enters `review` with inspectable artifacts and actual evidence. A card
enters `document` when durable docs, migration notes, or acceptance records still
need completion. A card enters `done` only when the declared outcome is accepted
for its scope.

Completion records identify:

- changed artifacts;
- tests or verification actually run;
- limitations and unavailable checks;
- governing ADR/design versions;
- follow-up cards;
- Receipt River reference;
- accepting actor or authority.

## Current media-workbench design wave

The current authority set is:

- research: `docs/research/media-workbench-interface-and-publishing.md`;
- proposed ADR: `docs/adrs/adr-001-local-first-media-workbench.md`;
- open design: `docs/designs/media-workbench-v1.md`;
- delivery map: `docs/kanban/BOARD-BREAKDOWN.md`.

Until ADR-001 is accepted and the design approved, implementation stories remain
`incoming`, `accepted`, or `breakdown`; they are not ready merely because this
board exists.
