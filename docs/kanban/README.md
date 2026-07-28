# Fork Tales Media Workbench Board

This is the Rheos/eta-mu coordination board for building the Fork Tales daily-driver
player, curation workbench, non-destructive salvage editor, release builder, and
publication adapters.

Read in this order:

1. `../../PROCESS.md`
2. `../process/product-design-and-delivery.md`
3. `../research/media-workbench-interface-and-publishing.md`
4. `../adrs/adr-001-local-first-media-workbench.md`
5. `../designs/media-workbench-v1.md`
6. `AGENTS.md`
7. `BOARD-BREAKDOWN.md`

The first active card is FT-000A, which requests an explicit human disposition on
the proposed ADR and open design. Implementation cards are intentionally not
`ready` yet.

When Rheos is available locally, run from the repository root so it discovers
`openhax.kanban.json`:

```bash
eta-mu kanban list
eta-mu kanban board snapshot --out docs/kanban/board.json
```

Use `--tasks-dir` only for an intentional board-location override or configuration
diagnostics.

`board.json` is generated and was not created by the remote ChatGPT/GitHub
connector design pass.
