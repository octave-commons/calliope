---
category: "stories"
labels: "architecture, design-review, media-workbench"
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785718816040-0.i501qm765yp1z1x4b6a"
points: "2"
title: "FT-000A: Review and accept or revise Media Workbench authority"
priority: "P0"
status: "done"
epic: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-000a-review-and-accept-or-revise-media-workbench-authority"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "Err"
---

# FT-000A: Review and accept or revise Media Workbench authority

## Outcome

Err explicitly accepted ADR-001 and approved Media Workbench v1 after requiring
normal Rheos config discovery instead of repeated `--tasks-dir` defaults.

## Scope

- Review the research, ADR, design, and delivery process as one governing set.
- Record explicit human disposition and independent review findings.
- Correct board mechanics without silently changing accepted product boundaries.
- Establish which implementation cards may advance after acceptance.

## Accepted boundaries

- Immutable source renders.
- First-class clips and arrangements.
- Scoped ratings, playlists, smart lists, and workspaces.
- Local accepted releases before target publication.
- Per-target publication state.
- Shared command/query application boundary.
- Native Clojure/JVM first client with no embedded browser.
- Daily-driver playback before editing and publication expansion.

## Non-goals

- Implementing the native player, audio backend, or read model.
- Accepting future implementation results without their own evidence.
- Treating PR merge state as architectural acceptance by itself.

## Acceptance criteria

- ADR-001 has an explicit accepted disposition and named decider.
- Media Workbench v1 has an explicit approved disposition.
- Independent review findings are recorded and board-mechanics defects are corrected.
- FT-000B and FT-000D may advance only through their declared dependencies.
- Future implementation cards remain separately reviewable and evidence-gated.

## Verification

- Human disposition in the conversation and PR #3 comment `5099692071`.
- Independent local Claude review `4793817603`, which approved the design authority
  and requested Rheos mechanics corrections.
- Board mechanics corrected in commit `09be7d22f414a753f1a3a5067fb14f8e8fff6da3`.
- FT-000D created to own the native UI, playback backend, read model, and
  application-topology decisions found missing by the independent review.
- Repository Contracts reads the board through eta-mu/Rheos and runs
  `clojure -M:test`. It does not validate the card graph or authority paths; no
  check in this repository does, and none may be added locally.

## Verification result

This is a human acceptance card owned by `Err`. Its disposition is recorded, not
inferred. The evidence that actually exists on 2026-07-28, at commit `e9e3770`:

**Human disposition — PR #3 issue comment `5099692071`**, author `riatzukiza`,
`2026-07-28T03:46:00Z`, titled "Human review disposition", verbatim verdict:

```text
**APPROVE AFTER CHANGE**

Err approved the media-workbench direction with one requested correction: normal
Rheos commands must rely on repository-root discovery of `openhax.kanban.json`
rather than redundantly defaulting to `--tasks-dir docs/kanban`.
```

Retrieved with
`gh api repos/octave-commons/fork_tales_v2/issues/comments/5099692071`.
That comment is the acceptance basis for this card, and the requested correction
was applied on this branch.

**Independent review — PR #3 review `4793817603`**, author `riatzukiza`,
`2026-07-28T04:51:14Z`, GitHub state `COMMENTED`. Its body records
"REQUEST-CHANGES on board mechanics; APPROVE the design authority".

**Board-mechanics correction** — commit `09be7d2` ("fix: align media workbench
board with Rheos mechanics"), confirmed present in this branch's history.

**Explicitly not claimed.** PR #3 carries no review with GitHub state `APPROVED`;
all 13 reviews on it are `COMMENTED`
(`gh api repos/octave-commons/fork_tales_v2/pulls/3/reviews`). This card's `done`
status therefore rests on the human comment above and not on a GitHub review
approval, and not on PR merge state, which the non-goals already exclude. No
acceptance of unimplemented player, audio, read-model, or publication work is
recorded or implied here.

This section was added after the fact: the card was already `done` with a
`## Verification` plan but no recorded result, which the board contract does not
permit. The status and disposition are unchanged; only the missing evidence is now
present.

---
Dependency analysis (2026-08-02): no dependencies — root story. Blocks FT-000B (domain laws) and FT-000D (native runtime decision). Note: verification evidence for the human disposition is already recorded on this card; candidate for review/done at your call. Moved breakdown -> ready: no unresolved blockers.

Walk-through (2026-08-02): no new work produced — this card's declared outcome (review and accept or revise the media-workbench authority set) was executed 2026-07-28 with evidence already on-card: human disposition APPROVE AFTER CHANGE (PR #3 comment 5099692071), independent review 4793812071-adjacent (review 4793817603), and the requested board-mechanics correction applied in commit 09be7d2. Verification re-run this session: eta-mu kanban find resolves the card; clojure -M:test green (23 tests, 91 assertions). Acceptance: recorded, not pending. Not executed: nothing further was required by the card scope.

Review evidence (2026-08-02): artifacts on-card — Verification result section with verbatim human disposition, independent review record, and the explicitly-not-claimed list. Routed in_progress -> testing -> review per the adjudicated repo path (receipts.edn row 18): the installed promethean build gate on direct in_progress -> review runs pnpm against a pnpm-less Clojure repo; that is the recorded upstream Rheos gap, not a local check.

Done (2026-08-02): declared outcome accepted by Err 2026-07-28 (PR #3 comment 5099692071); correction applied and verified. Completion record: changed artifacts — none this session (board state only); evidence — on-card verification section plus this walk's comments; not executed — none outstanding; receipt — receipts.edn 2026-08-02 gate-0 walk entry; acceptance — recorded, not pending.
---