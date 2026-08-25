---
uuid: "ft-ops-002-reconcile-adr-and-design-statuses-after-review"
title: "FT-OPS-002: Reconcile ADR and design statuses after review"
status: incoming
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
- FT-000A carries the acceptance basis. It was created `done` and has since been
  corrected to `incoming`, because a card cannot be created already accepted.
- FT-000B and FT-000D are `ready`.
- FT-000C and player work remain dependency-gated.
- Board prose resides outside `docs/kanban/`.

## Verification

```bash
eta-mu --version
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000a-review-and-accept-or-revise-media-workbench-authority
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
eta-mu kanban find ft-000c-define-append-only-studio-events-and-read-projection
eta-mu kanban find ft-000d-decide-native-desktop-playback-read-model-and-application-topology
clojure -M:test
```

Read the reconciled statuses and dependencies out of Rheos, read the `status:`
field of each authority document directly, and confirm Receipt River preserves the
review disposition.

## Verification result

Executed on 2026-07-28 against commit `e9e3770` of `design/media-workbench-v1`,
from the repository root with no `--tasks-dir` flag. `eta-mu --version` reported
`1.1.1`. Each acceptance criterion is checked below against a real read.

Authority-document dispositions, read from committed frontmatter:

```text
docs/adrs/adr-001-local-first-media-workbench.md
  status: accepted   accepted: "2026-07-28"   deciders: [Err]
docs/designs/media-workbench-v1.md
  status: approved   approved: "2026-07-28"
docs/process/product-design-and-delivery.md
  status: accepted   accepted: "2026-07-28"
```

That satisfies "ADR-001 is `accepted` with Err as decider", "Media Workbench v1 is
`approved`", and "the product-delivery process is `accepted`".

Reconciled card statuses, read from Rheos:

```text
$ eta-mu kanban count
Total tasks: 27
  Icebox: 6
  Incoming: 11
  Breakdown: 4
  Ready: 2
  Done: 4
```

```text
$ eta-mu kanban list        # FT-000 family rows only
incoming  P0  FT-000C: Define append-only studio events and read projection
ready     P0  FT-000B: Define media workbench domain laws
ready     P0  FT-000D: Decide native desktop, playback, read-model, and application topology
done      P0  FT-000A: Review and accept or revise Media Workbench authority
```

`eta-mu kanban find` resolved all four child UUIDs and exited zero. The transcripts
above and below are recorded as captured and are not edited. They show FT-000A as
`done`, which was the state at capture time; that status was created rather than
transitioned into, and has since been corrected to `incoming`. FT-000B and FT-000D
are `ready`, and FT-000C is still `incoming` behind its recorded dependencies:

```text
$ eta-mu kanban find ft-000c-define-append-only-studio-events-and-read-projection
  status:     incoming
  dependency: ["ft-000b-define-media-workbench-domain-laws",
               "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"]
```

Neither dependency is accepted, so FT-000C remains correctly gated. That satisfies
the FT-000A/FT-000B/FT-000D/FT-000C criteria.

The FT-001 player family (FT-001A-D) is verified separately, since none of them
are FT-000C or FT-000D themselves:

```text
$ eta-mu kanban find ft-001a-index-playable-media-metadata-and-waveform-jobs
  status:     incoming
  dependency: ["ft-000b-define-media-workbench-domain-laws",
               "ft-000c-define-append-only-studio-events-and-read-projection",
               "ft-000d-decide-native-desktop-playback-read-model-and-application-topology"]

$ eta-mu kanban find ft-001b-implement-playback-resolver-persistent-queue-and-resume
  status:     incoming
  dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology",
               "ft-001a-index-playable-media-metadata-and-waveform-jobs"]

$ eta-mu kanban find ft-001c-build-persistent-player-shell-and-library-browser
  status:     incoming
  dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology",
               "ft-001b-implement-playback-resolver-persistent-queue-and-resume"]

$ eta-mu kanban find ft-001d-add-dispositions-ratings-labels-sorting-and-playlists
  status:     incoming
  dependency: ["ft-000c-define-append-only-studio-events-and-read-projection",
               "ft-001c-build-persistent-player-shell-and-library-browser"]
```

All four sit at `incoming`, and each depends transitively on FT-000C and/or
FT-000D, neither of which is accepted yet. That satisfies "FT-000C and player
work remain dependency-gated" against direct evidence for the player family, not
just FT-000C.

Board prose location:

```text
$ ls docs/kanban
chores  epics  stories
$ ls docs/kanban-docs
AGENTS.md  BOARD-BREAKDOWN.md  README.md
```

`docs/kanban/` holds only the three card categories, and no Markdown sits at its
root, so no prose can be ingested as a phantom card. That satisfies "board prose
resides outside `docs/kanban/`".

Implementation tests on the same tree:

```text
$ clojure -M:test
Ran 23 tests containing 91 assertions.
0 failures, 0 errors.
```

All seven acceptance criteria are satisfied by the reads above. This section was
added after the fact: the card was previously marked `done` with a `## Verification`
plan but no recorded result, which the board contract does not permit. The
disposition is unchanged; only the missing evidence is now present.

Receipt River evidence: `receipts.edn` entries at `:ts "2026-07-28T21:19:42Z"` and
the validator-sweep entry appended for this correction.
