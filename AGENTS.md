# fork_tales_v2 — Agent Contract

A muse/eta-mu/epiphany-participant workspace for the fork_tales corpus:
~690 unique songs distilled from ~1,500 lyric files scattered across
`~/Downloads/Suno Downloads`, `~/Music`, and `~/devel` (fork_tales v1,
Lore/fork-tales, gates-of-aker).

## Orientation

- `ledgers/ingest.edn` — THE source of truth. Append-only EDN event ledger:
  one event per line (`:doc/discovered`, `:ingest/run-started`,
  `:ingest/run-completed`, `:projection/computed`). Every document carries
  `:path :sha256 :body-sha256 :classification :title`.
- `docs/lyrics/` — DERIVED. A pure projection over the ledger (pass 1:
  exact-copy dedup by normalized body hash). Never edit by hand; regenerate
  with `bb scripts/corpus.clj project`.
- `docs/lyrics/index.edn` + `ledgers/projections/songs-v1.edn` — slug →
  `{:title :body-sha256 :sha256s :sources :classification :flags}`.
- `docs/lore/` — theme, style, and world-building synthesis (agent-written).
- `resources/classifiers/` — pure-data LLM classifier programs. Definitions
  separate sources, selectors, feature extractors, context generators, prompts,
  model profiles, output contracts, and emitted ledger events.
- `src/fork_tales/law/classifier.cljc` and `law/feature.cljc` — closed Malli
  contracts; `src/fork_tales/classifier/dsl.cljc` validates cross-references and
  resolves pure execution plans.
- `src/fork_tales/classifier/runtime.clj` — JVM interpreter for filesystem
  sources, seeded selection, context execution, Ollama/llama.cpp calls, exact
  feature caching, output validation, and append-only event emission.
- `receipts.edn` — muse receipt-river ledger for this repo (use the global
  `receipt_river` tool, do not hand-edit).

## Commands

```
bb scripts/corpus.clj ingest    # scan roots, append :doc/discovered events
bb scripts/corpus.clj project   # rebuild docs/lyrics from latest run
bb scripts/corpus.clj stats     # summarize latest run
bb scripts/corpus.clj variants  # pass 2: cluster same-title variants by edit distance
clojure -M:test                 # validate DSL and interpreter behavior
clojure -M:classify -- --seed 3721599729 --dry-run
clojure -M:classify -- --seed 3721599729
```

The dry run resolves the seeded sample and builds prompts without calling a
model or appending a final classification event. A normal run uses the model
profiles declared by the selected classifier program and appends validated
feature and concept events to the declared ledger.

## Ledger discipline (epiphany rules)

1. Append-only. Never rewrite history; corrections are new events.
2. Similarity is not identity. Pass 1 = exact hashes only. Later passes
   (edit-distance, embeddings) are graded *signals*, recorded as events —
   they never silently merge documents.
3. Provenance forever: a projected song always links back to every source
   path and hash it was derived from.
4. Observed → derived → provisional → accepted. `docs/lyrics` is `derived`.
5. Classifier output is validated before append. Record selection seed, object
   IDs and hashes, extractor and prompt versions, model profile, cache key, and
   validation disposition. A model-proposed concept or relationship is never
   accepted implicitly.

## Dedup roadmap

- [x] Pass 1: sha256 + normalized body-sha256 (strip Suno `ID:` line,
      whitespace) — done, 1,522 files → 690 songs.
- [x] Pass 2: edit-distance clustering for same-title/different-body variants
      (Suno re-renders, mashups) — done, `bb scripts/corpus.clj variants`:
      148 same-title clusters / 413 songs, line-level Levenshtein, recorded
      as `:doc/variant-cluster` events + `ledgers/projections/variants-v1.edn`
      (graded signals with per-edge similarity, never merged). Caveat:
      title-extraction artifacts (e.g. the "## signal" cluster, mean-sim
      0.07) group unrelated songs — use mean-similarity to filter.
- [ ] Pass 3: embedding similarity + named-entity/theme extraction
      (epiphany's intended territory; see `~/spaces/epiphany`).

## Ecosystem relationships

- Consumer of the globally-published muse plugins (`receipt_river`,
  `session_mycology`, `edn_ledger`) — no local plugin build.
- `.ημ/ledgers/` is the actor-ledger root when eta-mu actors run here.
- v1 prototype (`~/devel/orgs/octave-commons/fork_tales`) is inspiration
  only — its receipt vocabulary and manifest ideas evolved into muse's
  receipt schema; its code is not reused.
- Music studio prototype: `~/devel/orgs/open-hax/openplanner/packages/agents/knoxx`.

## License

Code: GPL-3.0-or-later. Creative works (lyrics, lore): CC-BY-SA-4.0.
(Inherited from fork_tales v1's dual licensing.)
