# Research: Suno renderer metadata — index and search

Status: evidence and constraints. This document does not decide architecture;
ADR-001 and FT-000D own read-model and runtime decisions.

Date: 2026-08-02. Authors: OpenCode session (implementation-model role), grounded
by five parallel discovery passes over the workspace.

## 1. What the renderer actually ships

Source: census of `/home/err/Downloads/Suno Downloads` on 2026-08-02
(343 folders; 824 mp3, 810 jpeg, 1099 txt, 275 json) plus full reads of sample
JSON and txt files.

Two metadata generations coexist:

- **txt generation** (all 1099 clips): `Title:` / `ID:` / `Prompt:` headers;
  267 newer txt files add a `Tags:` style line. No likes, no model, no sliders.
- **json generation** (275 files across 124 of 343 folders): full Suno API clip
  objects. Observed schema (275/275 unless noted):

```clojure
{:id :string :title :string :isLiked :bool :audio_url :string :image_url :string
 :isCover :bool :isRemaster :bool
 :metadata {:tags :string            ; free-form style prompt, may be ""
            :prompt :string          ; lyrics, may be "", may be markdown-polluted
            :type "gen"|"upload"     ; 254 / 21
            :duration :float|int     ; type drift observed (19 int)
            :negative_tags :string   ; 58/275
            :control_sliders {:audio_weight :float|int
                              :style_weight :float|int
                              :weirdness_constraint :float|int} ; 100/275
            :model_badges {..display_name "v5"|"v4.5-all"..}    ; 254/275
            :task :string            ; 87/275: playlist_condition, cover, mashup_condition
            ..}}
```

Constraints that shape any index:

1. **No timestamps anywhere.** No `created_at`; only file mtime. Temporal
   ordering must come from ledger event time, not renderer data.
2. **Naming asymmetry.** Media files use `<title> (N).mp3` (space before paren);
   JSON uses `<title>(N).json` (no space). Naive filename pairing misses
   numbered variations. Folder names sanitize `/` to `_`; JSON titles do not.
3. **Sparse additive fields.** `video_is_stale`, `task`, `songcard`,
   `negative_tags`, `artist_reference_warning` appear in subsets — the schema
   grows with extension versions. Closed-map validation would reject real data.
4. **Prompt pollution.** Some `metadata.prompt` values contain pasted markdown
   blobs (download links, sha256 hashes, program notes) as "lyrics".
5. **Likes are json-generation only.** 107 of 274 json clips liked (39.1%);
   551 txt-generation clips carry no like signal at all. Any "liked" index is
   an index over the json generation and must say so.
6. **Duplicate downloads.** 6 clip IDs appear in two folders each (halfwidth
   vs fullwidth paren variants of the same title), with different content
   hashes. Same renderer identity, different bytes: both observations are
   recorded; the projection takes latest. Similarity is never a silent merge.

## 2. What exists now (pass 4, this session)

`scripts/suno_meta.clj` — babashka pass over the downloads folder (default
root, `--root` override):

- observes both generations into `:suno/clip-observed` events appended to
  `ledgers/ingest.edn` (idempotent: unchanged clip-id + content-hash pairs are
  not re-appended);
- rebuilds `ledgers/projections/suno-meta-v1.edn`: 825 clips keyed by renderer
  id, with `:index {:liked :tags :models :sources}`;
- navigation commands: `stats`, `liked`, `tags`, `show TERM`.

First run: 831 clips scanned, 825 unique ids, 107 liked, 909 tag tokens,
models v5 (204) / v4.5-all (49), 68209s total duration, mean 249s.

This is the `:observed` tier. Tags are tokenized mechanically
(comma/semicolon/newline split, lowercased); structure (BPM, genre, texture)
is classifier work, not ingest work.

## 3. The engine question: DuckDB, Lucene, or both

The user asked whether DuckDB + Lucene makes sense, with vector search and a
single-binary deployment in mind.

### Facts (verified against vendor docs, 2026-08-02)

- **DuckDB `fts` extension**: BM25 (`match_bm25`) over an inverted index;
  static snapshot — no automatic incremental update (rebuild on change;
  newer versions add an `incremental` trigger-maintained mode). New
  `opensearch_standard` tokenizer splits CJK into single-character tokens —
  relevant: this corpus is heavily Japanese-titled.
- **DuckDB `vss` extension**: HNSW (via USearch) over `FLOAT[n]` arrays;
  cosine/L2/IP metrics. Persistence is experimental
  (`hnsw_enable_experimental_persistence`): WAL recovery for custom indexes is
  not implemented, the whole index re-serializes on every checkpoint, and the
  index must fit in RAM outside DuckDB's `memory_limit`.
- **Lucene (embedded)**: one FSDirectory index holds both lexical docs and
  `KnnFloatVectorField` vectors. Mature incremental indexing, persistence, and
  recovery. Already running on this machine: epiphany's adapter
  (`~/spaces/epiphany/src/epiphany/infra/adapters/lucene.clj`, index v4,
  Lucene 10.5) with Ollama embeddings, model-digest pinning, and pure hybrid
  score fusion (`domain/hybrid_search.clj`), 743 tests green.

### Scale check

825 clips, ~909 tag tokens, prompts up to a few KB. Even at 100x growth this
corpus fits any engine entirely in RAM. DuckDB's headline advantages (columnar
build speed, disk economy, fuzzy-at-scale) are measured at 100M documents;
they do not differentiate at 10^3.

### Assessment

- **DuckDB + Lucene together**: two engines, two consistency stories, no
  compensating benefit at this scale. Not recommended.
- **DuckDB alone**: viable and genuinely attractive as a *single-file artifact*
  — one `.duckdb` containing metadata tables, FTS index, and vectors is a
  good distribution/export format. But vss persistence is experimental, the
  repo's runtime is JVM Clojure (duckdb_jdbc adds a native-lib dependency),
  and nothing in the workspace has operational experience with it.
- **Lucene alone**: matches the stack (FT-000D's native JVM client), matches
  proven in-workspace machinery (epiphany's adapter, Ollama digest pinning,
  hybrid fusion, `UNAVAILABLE`-not-fallback discipline), and plugs the exact
  seam the classifier DSL already reserves: `:anchor-neighbors` retrieval
  adapter (`src/fork_tales/classifier/runtime.clj`).

Recommendation as research input: Lucene embedded for the working index;
treat a single-file DuckDB export as a possible *publication* format later
(rebuildable derivative, not truth). The truth stays the EDN ledger either way.

### Single-binary note

babashka already is the single binary for ingest/navigation (this pass runs on
it). A native search binary means GraalVM native-image over Clojure + Lucene —
possible but a real spike (reflection/resource config; Lucene native-image
friction is documented upstream). Near-term honest shapes: `bb` for the EDN
half, uberjar (`clojure -M`) for the search service. The native-image question
belongs to FT-000D's runtime decision and should not be pre-empted here.

## 4. Classification plan (derived/provisional tiers)

All of the following run through the existing classifier DSL
(`resources/classifiers/*.edn`, `:source/type :edn-projection` over
`suno-meta-v1.edn`), never as hand edits:

1. **Tag normalization** — map the 909 raw tag tokens toward the controlled
   vocabulary already synthesized in `docs/lore/style.md`; extract structured
   features (BPM, genre, texture, vocal treatment) from the free-form style
   strings. Output: `:derived` feature events.
2. **Prompt structure** — section-header census per prompt using the pass-1
   section-tag regex; flag markdown-polluted prompts (constraint 4 above) for
   exclusion from thematic discovery, matching the pasted-artifact precedent.
3. **Preference model** — likes as the only renderer-side preference signal
   (constraint 5): candidate concepts correlating liked clips with tag/model
   features. Output: `:provisional` concepts; promotion requires review events.
4. **Embeddings** — embed title + tags-raw + prompt via the local Ollama
   endpoint with model-digest pinning; store in the Lucene index; expose to
   the DSL through the `:anchor-neighbors` selector. Semantic neighbors are
   signals, never merges.

## 5. Open constraints for downstream decisions

- FT-000D owns read-model choice; this document is evidence, not a decision.
- The liked index must always be reported as json-generation-scoped.
- CJK tokenization matters for titles and tags; engine/analyzer choice must
  handle it (Lucene: CJK analyzer; DuckDB fts: `opensearch_standard`).
- Non-git artifact identity (epiphany's deferred decision in
  `docs/designs/artifact-identity-model.md`) is forced by this dataset; the
  ledger's content-hash provenance is this repo's local answer.

## Evidence anchors

- Census + schema: full-folder scan and per-key type census of all 275 JSON
  files, 2026-08-02 (this session; reproducible with `bb scripts/suno_meta.clj stats`).
- DuckDB behavior: duckdb.org docs for `fts` and `vss` extensions and the
  2024-05/2024-10 VSS announcements, retrieved 2026-08-02.
- Lucene/epiphany behavior: `~/spaces/epiphany/src/epiphany/infra/adapters/lucene.clj`,
  `infra/adapters/ollama.clj`, `domain/hybrid_search.clj`; roadmap and ADRs
  under `~/spaces/epiphany/docs/`.
- Repo doctrine: `AGENTS.md` (ledger discipline, epistemic tiers),
  `docs/classifier-dsl.md`, `docs/lore/style.md`.
