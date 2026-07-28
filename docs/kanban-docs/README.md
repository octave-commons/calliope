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
prose lives here in `docs/kanban-docs/` so Rheos does not ingest it as phantom
work.

ADR-001 is accepted and Media Workbench v1 is approved. FT-000A, the review
reconciliation chores, and the FT-OPS-001 local board verification are done. The
first ready slices are:

- FT-000B — media-workbench domain laws;
- FT-000D — native UI, real playback, read-model, and application topology.

## Local verification

Run from the repository root so Rheos discovers `openhax.kanban.json`:

```bash
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
python3 scripts/validate_rheos_board.py
```

Use `--tasks-dir` only for an intentional override or configuration diagnostic.

Verified with eta-mu 1.1.1 on 2026-07-28: 27 cards, no phantom prose cards, and a
passing validator. See `../kanban/chores/ft-ops-001-generate-rheos-board-snapshot.md`
for the recorded output and the FSM/write-path drift found in that version.

The current Rheos `board snapshot` drops rich card fields and is not committed.
Markdown card files plus the repository validator are the inspectable board source.
The CLI is the authoritative reader; card Markdown in Git is the authoritative
writer, because `eta-mu kanban frontmatter`/`comment` rewrite frontmatter in a way
the repository validator rejects.
