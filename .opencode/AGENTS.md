# Calliope — OpenCode adapter

This file applies only when OpenCode is operating in this repository.
Harness-neutral obligations remain in root `PROCESS.md`; repository facts remain
in root `AGENTS.md`.

## Startup

1. Resolve the actual Git root and branch.
2. Read root `PROCESS.md`, root `AGENTS.md`, and the applicable skill under
   `.opencode/skills/`.
3. Tail the last three `receipts.edn` records before continuing consequential
   work.
4. Verify that expected global eta-mu/Muse extensions and local services are
   actually available.

Machine-global assumptions are documented in
`docs/harnesses/opencode-global.md`; they are not portable repository facts.

## Local tools

When installed:

- use `receipt_river` for every substantive touched repository;
- use `edn_ledger` for governed append-only EDN ledgers;
- use repository skills for corpus-specific operations;
- use local shell commands for tests and builds, recording exact results;
- use Ollama or llama.cpp only after checking endpoint availability and model
  identity.

If a global tool or service is missing, follow the fallback in `PROCESS.md` and
record the unavailability. Do not silently replace it or claim it ran.

## Corpus work

Use `.opencode/skills/calliope-corpus/SKILL.md` whenever changing ingestion,
`docs/lyrics/`, variants, or corpus ledgers. Projections are never canonical
sources.

## Completion

Before ending substantive work:

- run the applicable checks;
- append a Receipt River record with real paths, refs, tests, decisions, and
  drift;
- distinguish branch/PR state, CI state, merge state, and human acceptance.
