# Fork Tales Media Workbench Board

This is the Rheos/eta-mu coordination board for the Fork Tales native daily-driver
player, curation workbench, non-destructive salvage editor, release builder, and
publication adapters.

## Authority reading order

1. `../../PROCESS.md`
2. `../process/product-design-and-delivery.md`
3. `../research/media-workbench-interface-and-publishing.md`
4. `../adrs/adr-001-local-first-media-workbench.md`
5. `../designs/media-workbench-v1.md`
6. `AGENTS.md`
7. `BOARD-BREAKDOWN.md`

`docs/kanban/` is the configured Rheos `tasksDir` and contains cards only. Board
prose lives here in `docs/kanban-docs/` so Rheos does not ingest it as work.

ADR-001 is accepted and Media Workbench v1 is approved. FT-000A, the review
reconciliation chores, and FT-OPS-001 preserve historical disposition and
verification evidence, but their live card status is `incoming`; none is complete
until Rheos records the lifecycle acceptance. FT-000B and FT-000D were returned
from `ready` to `breakdown` through Rheos on 2026-08-29 because their declared
FT-000A dependency is still `incoming`:

- FT-000B — media-workbench domain laws;
- FT-000D — native UI, real playback, read-model, and application topology.

## Board operations

Run from the repository root so eta-mu/Rheos discovers `openhax.kanban.json`:

```bash
eta-mu --version
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
```

Use `--tasks-dir` only for an intentional alternate board or configuration
diagnostic.

Rheos is the sole board implementation and validation authority. This repository
does not ship a separate parser, validator, writeback helper, or shadow state
machine. CI invokes eta-mu/Rheos directly. Missing behavior is fixed upstream in
`open-hax/eta-mu` rather than reimplemented here.

Verified with eta-mu 1.1.1 on 2026-07-28: Rheos discovered 27 cards and no phantom
prose cards. See
`../kanban/chores/ft-ops-001-generate-rheos-board-snapshot.md` for the historical
command output and tool-version observations.

Current reconciliation evidence on 2026-08-29: Rheos discovered 35 cards
(`icebox=9`, `incoming=20`, `breakdown=6`, all other columns zero). The increase
from the latest 31-card receipt consists of the already-merged FT-004H card plus
three canonical recovery cards for GitHub issues #11, #13, and #14. Historical
count transcripts above and in receipts remain immutable evidence of their dated
trees; query Rheos for live counts.

Generated board snapshots remain diagnostic Rheos projections. Live board state,
legal transitions, writes, and drift detection come from Rheos.

## PR #3 review closeout

The final reviewed board has explicit owners for the product capabilities needed by
its dependent views and adapters:

- FT-001A projects explicit work-to-render and render-family identity; downstream
  views do not infer identity from titles or directories.
- FT-001B owns the shared stop transport command consumed by native media keys.
- FT-004B packaging and FT-004G video rendering are parallel branches from
  FT-004F's accepted release; FT-004G also owns durable render-job retry and
  cancellation activity.
- FT-004C and FT-004D own target-specific cancellation commands and refusal
  semantics; FT-004H only projects and dispatches owning command surfaces.

Capability ownership is distinct from assignment to a human. Icebox cards may
remain `owner: unassigned` until work becomes active. Cards introduced by this PR
enter at their proposed initial status; the review does not fabricate retroactive
Rheos transitions to represent discarded draft states.
