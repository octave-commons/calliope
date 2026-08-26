# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

Fork Tales is a creative corpus (lyrics, prompts, renders, artwork, project
history) plus a Clojure/JVM system for recovering relationships among those
artifacts, plus a Rheos-managed board driving the Media Workbench product. Most
of the repository is EDN ledgers, Malli contracts, and governance prose; the
desktop application itself is not implemented yet (board Gate 0 is active).

## Authority order

Read `PROCESS.md` and `AGENTS.md` before substantive work. When guidance
conflicts, higher wins and lower must not silently override it:

1. `PROCESS.md` — evidence, receipts, harness portability, completion claims.
2. Accepted ADRs in `docs/adrs/` and approved designs in `docs/designs/`.
3. Data contracts in `src/fork_tales/law/`.
4. Root `AGENTS.md` — repository structure and engineering practice.
5. `docs/harnesses/` and `.opencode/AGENTS.md` — per-harness adapters.
6. Task/card instructions — bounded to their own task.

Declare real harness capability before acting. Never claim a command, model
call, or test ran unless it actually ran here; an unreachable service is
*unavailable*, never a successful empty result.

## Commands

```bash
# Clojure tests (23 tests / 91 assertions; also the CI gate)
clojure -M:test

# Single test var: the :test alias's :main-opts hijacks -M:test and -A:test, so
# appending -e re-runs the whole suite. Use an inline alias for the test path.
clojure -Sdeps '{:aliases {:t {:extra-paths ["test"]}}}' -M:t \
  -e "(require 'clojure.test 'fork-tales.classifier.dsl-test) \
      (prn (clojure.test/test-vars [#'fork-tales.classifier.dsl-test/example-program-is-runnable]))"

# Corpus pipeline (babashka)
bb scripts/corpus.clj ingest    # append :doc/discovered events to ledgers/ingest.edn
bb scripts/corpus.clj project   # rebuild docs/lyrics + ledgers/projections/songs-v1.edn
bb scripts/corpus.clj stats     # classification counts for the latest run
bb scripts/corpus.clj variants  # pass 2: same-title clusters -> :doc/variant-cluster
bb scripts/corpus.clj tracks    # pass 3: ingest audio/artwork into tracks/ (git LFS)

# Classifier interpreter (--dry-run builds selection + prompts, no model calls)
clojure -M:classify -- --seed 3721599729 --dry-run
clojure -M:classify -- --seed 3721599729          # needs a live endpoint + models
clojure -M:classify -- --help

# Board reads through Rheos, from the repository root so openhax.kanban.json is discovered
eta-mu kanban count
eta-mu kanban list
eta-mu kanban find ft-000b-define-media-workbench-domain-laws
```

`clojure -M:classify` ends its `:main-opts` with `-m`, so the `--` separator is
forwarded into `parse-args` rather than consumed by the CLI — keep it.

Adding a test namespace requires editing the explicit `require` and
`run-tests` list in `test/fork_tales/test_runner.clj`; there is no test
discovery.

CI (`.github/workflows/classifier-dsl.yml`) installs `eta-mu@1.1.1`, exercises
the Rheos board reads above, then runs `clojure -M:test`.

## Architecture

### Corpus pipeline — event-sourced, projection-derived

`scripts/corpus.clj` (babashka, single file) appends events to
`ledgers/ingest.edn`, the append-only source of truth. Everything under
`docs/lyrics/` and `ledgers/projections/` is a rebuildable projection over that
ledger. **Never hand-edit a projection to create truth** — change a source,
event, or the projection code and re-project. Dedup groups `:suno-lyric` and
`:hand-lyric` documents by `:body-sha256` (text minus the Suno `ID:` line,
whitespace-normalized) and copies one canonical source verbatim into
`docs/lyrics/`. Pass 2 emits graded Levenshtein similarity per same-title
cluster; similarity is a signal and never becomes identity. `tracks/` holds
corpus-linked audio and artwork through git LFS (`*.mp3`, `*.jpeg`).

See `.opencode/skills/fork-tales-corpus/SKILL.md` for event shapes,
classification rules, and pass-1/pass-2 corpus facts.

### Classifier DSL — programs are data, not code

```text
src/fork_tales/law/{classifier,feature}.cljc   Malli contracts (.cljc, portable)
src/fork_tales/classifier/dsl.cljc             registry, semantic lint, plan compilation
src/fork_tales/classifier/runtime.clj          JVM adapter: selection, context, model calls
src/fork_tales/classifier/main.clj             CLI entry
resources/classifiers/theme-discovery-v1.edn   the complete example program
docs/classifier-dsl.md                         the DSL reference — read before changing it
```

A program is EDN with named registries (`:sources :models :features :selectors
:contexts :prompts :outputs :extractors :classifiers`). Registry keys must equal
each definition's declared ID. `dsl/lint` checks cross-references and context
dataflow; Malli checks local shape; `dsl/compile-plan` and
`compile-extractor-plan` resolve a runnable plan.

Invariants that must survive any change:

- No executable functions and no arbitrary evaluation live in program data.
  Resolvers and context operations are *named* runtime capabilities
  (`runtime/built-in-resolvers`, the five `:step/op` verbs), and the selector
  filter language stays small and non-Turing-complete.
- Keep layers distinct: object -> feature observation -> classification. A
  section-level feature does not belong to its song, production *intent* is not
  audible *observation*, and a proposed relation is provisional until explicitly
  accepted for a declared scope.
- Feature cache keys are explicit and cover every dependency (content hash,
  extractor version, and for LLM extractors model digest, prompt version, and
  context-generator hash). A changed dependency yields a new observation instead
  of stale reuse. Feature events append to the ledger named by each extractor's
  `:extractor/emits :event/ledger` (currently `ledgers/classification.edn`).
- Output contracts are the single source of truth: `:provider-native` (default,
  JSON-Schema-constrained decoding) for bulk extraction, `:tool-call` only for
  small flat payloads, `:inline-schema` only for models confirmed to follow it
  (small local models echo the schema back). Keywords cross the JSON boundary
  through `runtime/json-safe`, and responses decode via
  `malli.transform/json-transformer`.

### Epistemic tiers

```text
observed -> derived -> provisional -> accepted
```

Never silently promote a tier. Repetition is not acceptance, model agreement is
not truth, and passing CI is not human acceptance.

## Rheos board discipline

`docs/kanban/` is a Rheos card corpus; `docs/kanban-docs/` holds the board
contract (`AGENTS.md`) and delivery map (`BOARD-BREAKDOWN.md`) outside
`tasksDir`. **Rheos is the sole implementation and operational authority for
board state.** Never add a repository-local parser, validator, migration script,
sidecar, WIP checker, or alternate command surface for board semantics — a
missing check is an upstream gap in `open-hax/eta-mu` / `@eta-mu/rheos` that
blocks work here, not permission to duplicate it.

- `openhax.kanban.json` sets `tasksDir` and `fsm`. The installed engine resolves
  `"fsm"` as either a known name or a complete FSM map used verbatim; a JSON map
  cannot express the keyword check ids gates dispatch on, so a map value freezes
  the board. `"fsm": "promethean"` is the only working value here.
- Cards are hand-authored Markdown and that is intended Rheos design, not a
  workaround. Outcome, scope, non-goals, acceptance, and verification are
  reviewed as diffs. Do not treat this as a second write protocol for board
  *state*: status, frontmatter edits, comments, and transitions go through Rheos.
- Explicit `uuid:` is canonical identity; `epic`, `parent`, and `dependency` use
  that namespace. Omit empty dependency fields. Labels are comma-separated
  scalars for the current parser.
- `docs/kanban/.events/ledger.edn` is tracked, one EDN event per line, union-merged
  (`.gitattributes`). Never rewrite or reorder it. `docs/kanban/board.json` is a
  lossy diagnostic and is gitignored.
- Never create a card already in `done`: `done` asserts accepted work that some
  lifecycle produced.
- A harness without Rheos may read board files but must not mutate board state
  or claim the board is valid.

## Receipt River

Append exactly one EDN map per line to `receipts.edn` after substantive work
(code, schemas, process docs, workflows, PRs, relied-upon test runs, durable
decisions, committed research). Required keys: `:ts :kind :repo :origin :owner
:dod :pi :host :manifest :refs`; `:note :tests :decisions :drift` are useful
optional keys. Recall the last three records before consequential work.

Never edit or delete a prior receipt line, even to fix an error — append a
correcting entry. Work done without a receipt is compensated with a `:drift`
record, not by rewriting history. The same append-only rule governs
`ledgers/*.edn`.

## Media Workbench product boundaries

The governing set is `docs/research/media-workbench-interface-and-publishing.md`,
`docs/adrs/adr-001-local-first-media-workbench.md` (accepted),
`docs/designs/media-workbench-v1.md` (approved),
`docs/process/product-design-and-delivery.md`, `docs/kanban-docs/AGENTS.md`, and
`docs/kanban-docs/BOARD-BREAKDOWN.md`.

Preserve these identities as distinct: a work is not a render; a render is
immutable source audio; a marker is an annotation, not an accepted edit; a clip
is a non-destructive span of one render; an arrangement is an edit decision list;
an export is a rebuildable derivative; a release is a locally accepted bundle;
publication state is per-target. Playlists, smart lists, workspaces, and the
Rheos board are separate concepts.

The first client is a native Clojure/JVM desktop application with no embedded
browser. FT-000D owns the native UI, audio backend, read model, and in-process
topology — downstream cards consume those decisions rather than inventing them.
The first milestone is a daily-driver player verified against real corpus audio;
publishing work may not delay playback, curation, or salvage.

## Conventions

- Several agents work this repository concurrently. Re-fetch before trusting any
  head, CI result, or mergeability claim.
- Prose in `docs/`, `AGENTS.md`, and `PROCESS.md` wraps at 80 columns.
- Contracts and the DSL are `.cljc` (portable); JVM-only adapters are `.clj`.
- Software and process docs are GPL-3.0-or-later; creative corpus and lore are
  CC-BY-SA-4.0 where applicable.
