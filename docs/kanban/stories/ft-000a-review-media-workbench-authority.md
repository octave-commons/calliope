---
category: "stories"
labels: "architecture, design-review, media-workbench"
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792917202753-0.ukwenm9eptgo1ujqqfd"
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

**Explicitly not claimed.** At the time this result was recorded, PR #3 carried no
review with GitHub state `APPROVED`; the available reviews were `COMMENTED`. The
human comment above is design-disposition evidence, not proof that this card passed
the Rheos lifecycle. No acceptance of unimplemented player, audio, read-model, or
publication work is recorded or implied here.

This result section was added after a branch snapshot had already created the new
card with `status: done`. That status was invalid: a new card enters `incoming`,
and `done` must be reached through a Rheos lifecycle transition. The current
frontmatter is therefore `incoming`; the recorded disposition and evidence remain
inputs to a future lifecycle acceptance rather than a claim that it already
occurred.

---
2026-10-25 current-source reconciliation for the studio goal: native GitHub readback of human disposition comment 5099692071 and independent review 4793817603 succeeded. The comment records approval after config-discovery correction, with later authority/status reconciliation explicitly separate. Current ADR-001 remains accepted, design remains approved, process accepted; current cards use canonical UUIDs and prose remains outside tasksDir. This is evidence for the existing card lifecycle, not new approval or acceptance of player/capture implementations. References: PR #22, docs/research/studio-foundation-map.md; no historical ledger was imported.

2026-10-25 lifecycle reconciliation completed through Rheos: incoming -> accepted -> breakdown -> ready -> todo -> in_progress, original pnpm gate refusal respected, then review only after consuming the upstream Rheos JVM gate extension and executing 112 tests/699 assertions plus zero-warning lint. document -> done records the already-existing named human acceptance, not approval of new studio code. Source approval comments 5099692071 / 4793817603 were freshly read; prior events remain immutable.

---