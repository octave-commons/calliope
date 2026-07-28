# OpenCode global harness instructions

This document is source material for machine-global OpenCode/eta-mu
instructions. It is not evidence that another harness has these capabilities.

## Expected environment

On Err's configured development machine, OpenCode may provide:

- local repository and filesystem access;
- shell and process execution;
- local Ollama and llama.cpp endpoints;
- eta-mu extensions, including `receipt_river`;
- Muse tools such as `edn_ledger` when installed and published;
- repository-scoped skills under `.opencode/skills/`.

Verify each capability at session start. A stale global configuration, missing
build, different working directory, or unavailable service is an observable
failure—not permission to silently substitute another path.

## Global behavior

1. Resolve the current Git root before writing.
2. Read the root `PROCESS.md`, root `AGENTS.md`, and applicable nested
   `AGENTS.md` or skill.
3. Tail recent `receipts.edn` records before consequential continuation.
4. Use `receipt_river` for substantive work in every touched repository.
5. Use `edn_ledger` for append-only EDN ledgers when available; do not use
   ad-hoc shell appends for governed ledgers.
6. Record real commands, models, versions, and results. Do not convert an
   unavailable service into a successful empty result.
7. Keep local-machine paths and service assumptions in global or OpenCode-local
   instructions, not in harness-neutral repository policy.

## Receipt defaults

When calling `receipt_river`:

- `:origin` should be `opencode`;
- `:host` should be `local` only when executing on the local machine;
- `:repo` should resolve to the actual Git root;
- `:refs` should name changed paths, commit or PR references, and reports;
- `:tests` should list commands actually executed;
- `:drift` should record contract violations or unavailable assumptions.

Never edit old receipt lines and never log credentials, tokens, or secrets.

## Fork Tales local assumptions

Historical corpus sources may exist below `~/Downloads`, `~/Music`, and
`~/devel`. Treat those paths as available only after checking them. The Git
repository remains the authority for committed projections, schemas, programs,
and process documents.

Local classifier runs may use:

```bash
clojure -M:classify -- --seed 3721599729 --dry-run
clojure -M:classify -- --seed 3721599729
```

A live run requires the configured model endpoint and is not established merely
because the command is documented.
