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
reconciliation chores, and the FT-OPS-001 local board verification are done. The
first ready slices are:

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

Generated board snapshots remain diagnostic Rheos projections. Live board state,
legal transitions, writes, and drift detection come from Rheos.
