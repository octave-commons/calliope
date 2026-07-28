# fork_tales_v2 — Repository Agent Guide

A muse/eta-mu/epiphany-participant workspace for the Fork Tales corpus: roughly
690 unique songs distilled from about 1,500 lyric files, now connected to Suno
track metadata, audio, artwork, classifier programs, and provisional concept
work.

Read `PROCESS.md` first. It governs evidence, receipts, harness portability,
completion claims, and acceptance. This file describes repository facts and
engineering practice; it does not imply access to Err's local machine.

## Ground truth

- `ledgers/ingest.edn` — append-only ingestion truth. Events preserve path,
  content hashes, classification, title, basis, and run provenance.
- `docs/lyrics/` — derived pass-1 projection. Never edit by hand to create new
  truth; change sources or projection logic and regenerate.
- `docs/lyrics/index.edn` and `ledgers/projections/songs-v1.edn` — song index
  projections.
- `ledgers/projections/variants-v1.edn` — pass-2 same-title similarity signals.
  Similarity never silently becomes identity.
- `tracks/` — corpus-linked audio, metadata, and artwork assets.
- `resources/classifiers/` — pure-data classifier and feature-extractor programs.
- `src/fork_tales/law/` — Malli contracts only.
- `src/fork_tales/classifier/` — DSL validation and JVM runtime adapters.
- `docs/lore/` — derived thematic, stylistic, and world-building synthesis.
- `docs/research/` — evidence and constraints; research does not decide architecture.
- `docs/adrs/` — accepted architectural authority for the declared scope.
- `docs/designs/` — product behavior grounded in research and accepted ADRs.
- `docs/process/` — revisable policies implementing the root charter.
- `docs/kanban/` plus `openhax.kanban.json` — Rheos coordination board. Cards do
  not override ADRs or designs; `board.json` is generated when present.
- `receipts.edn` — append-only Receipt River accountability ledger.

## Commands

These commands require a harness with a local repository, shell, dependencies,
and applicable services. Their presence here does not prove the current harness
can execute them.

```bash
bb scripts/corpus.clj ingest    # append :doc/discovered events
bb scripts/corpus.clj project   # rebuild lyrics and song projections
bb scripts/corpus.clj stats     # summarize the latest ingest run
bb scripts/corpus.clj variants  # produce same-title graded similarity signals

clojure -M:test
clojure -M:classify -- --seed 3721599729 --dry-run
clojure -M:classify -- --seed 3721599729

# Compare output contracts against a live endpoint.
clojure -M:classify -- --seed 3721599729 --output-contract tool-call

# Rheos board (when eta-mu is installed locally; run from the repository root so
# openhax.kanban.json supplies tasksDir).
eta-mu kanban list
eta-mu kanban board snapshot --out docs/kanban/board.json
```

The non-dry classifier run additionally requires its declared model endpoint
and its declared models pulled locally:

```bash
ollama pull gemma4:e2b   # feature extraction
ollama pull gemma4:e4b   # concept discovery
```

An unreachable selected service is unavailable, never a successful empty result.

## Classifier architecture

The classifier system is data-first:

```text
source objects
  -> seeded selection
  -> feature extraction and exact cache lookup
  -> bounded multimodal context
  -> model prompt and validated output
  -> derived feature or provisional concept events
```

Keep these layers distinct:

- source object versus projection;
- work versus section versus text or audio span;
- production intent versus audible observation;
- feature observation versus classification;
- provisional relationship versus accepted relation.

Schemas precede adapters. Classifier definitions must remain non-executable data;
runtime operations use closed vocabularies and explicit resolvers.

## Media workbench design

The proposed media workbench authority set is:

- `docs/research/media-workbench-interface-and-publishing.md`;
- `docs/adrs/adr-001-local-first-media-workbench.md`;
- `docs/designs/media-workbench-v1.md`;
- `docs/process/product-design-and-delivery.md`;
- `docs/kanban/BOARD-BREAKDOWN.md`.

Until ADR-001 is accepted and the design approved, implementation cards remain
planning records rather than ready work.

Preserve these boundaries:

- a work is not a render;
- a render is immutable source audio;
- a marker is an annotation, not automatically an accepted edit;
- a clip is a non-destructive span of one render;
- an arrangement is an edit decision list;
- an export is a rebuildable derivative;
- a release is a local accepted bundle before publication;
- publication state is target-specific;
- playlists, smart lists, user workspaces, and the Rheos development board have
  distinct semantics.

The first usable milestone is a daily-driver player. Publishing work must not
delay playback, curation, or salvage.

## Ledger discipline

1. Append-only: never rewrite historical events or receipts.
2. Preserve provenance: content hashes, object IDs, selection seeds, model and
   prompt identity, cache keys, and evidence remain inspectable.
3. Preserve epistemic tier: observed -> derived -> provisional -> accepted.
4. Never silently merge based on title, embedding, edit distance, repeated model
   agreement, or shared vocabulary.
5. Follow the Receipt River protocol in `PROCESS.md` after every substantive
   repository write. A receipt is required even when work is performed through a
   remote GitHub connector rather than OpenCode.

## Harness boundaries

Harness-specific instructions live outside this guide:

| Harness | Instructions |
|---|---|
| OpenCode local environment | `.opencode/AGENTS.md` and `docs/harnesses/opencode-global.md` |
| ChatGPT connectors | `docs/harnesses/chatgpt.md` |
| Perplexity web research | `docs/harnesses/perplexity.md` |
| Other | `docs/harnesses/README.md` plus a declared capability set |

Never infer local filesystem, shell, model-server, plugin, or write access from a
reference in this repository.

## Corpus facts

- Pass 1: 22,208 documents scanned -> 1,522 lyric files -> 690 unique songs.
- Pass 2: 148 same-title clusters covering 413 songs, with line-level
  Levenshtein signals.
- Low-similarity title clusters may be extraction artifacts. Treat values below
  roughly 0.3 as shared-title evidence only; values around 0.8 or above are
  stronger rerender candidates, never automatic identity.
- `:pasted-artifact` items remain in the corpus because they were rendered, but
  should normally be excluded from thematic discovery samples.

## Ecosystem relationships

- Epiphany supplies the evidence-first relationship, research/ADR/design, and
  archaeology discipline.
- Eta-mu/Rheos supplies the Markdown-backed development board when installed;
  repository cards remain readable without the tool.
- Eta-mu/Muse may supply local runtime extensions and compatibility tooling when
  installed; they are not assumed in remote harnesses.
- The Knoxx/OpenPlanner studio work remains historical design input.
- Suno and other music models are renderers and evidence sources, not canonical
  authorities for the corpus ontology.

## License

Software and process documentation: GPL-3.0-or-later.
Creative works and lore: CC-BY-SA-4.0 where applicable.
