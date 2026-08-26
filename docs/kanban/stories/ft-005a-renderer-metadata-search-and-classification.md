---
category: "stories"
labels: "search, embeddings, classifier-dsl, suno-metadata"
type: "story"
write-id: "1785714121763-0.if535elp2yr6w9yqztt"
title: "FT-005A: Renderer metadata search and classification"
priority: "P0"
status: "accepted"
uuid: "ft-005a-renderer-metadata-search-and-classification"
research: "docs/research/suno-metadata-index-and-search.md"
---

# FT-005A: Renderer metadata search and classification

## Outcome

The renderer metadata observed by pass 4 (`scripts/suno_meta.clj`,
`ledgers/projections/suno-meta-v1.edn`) becomes searchable and classifiable:
lexical + vector search over titles, tags, and prompts, and derived
tag/prompt structure via the classifier DSL.

## Scope

- Embedded Lucene index (lexical + KNN vectors, one index) per the research
  recommendation, reusing epiphany's adapter patterns and Ollama
  model-digest pinning.
- Embedding adapter exposed to the classifier DSL through the reserved
  `:anchor-neighbors` selector seam.
- Tag normalization classifier program toward the `docs/lore/style.md`
  controlled vocabulary; prompt section-structure and pollution-flag features.
- Liked-clip preference signals as provisional concepts only.

## Non-goals

- Pre-empting FT-000D's read-model or native-runtime decisions.
- A GraalVM single-binary spike (research notes defer it to FT-000D).
- DuckDB as a working index (research recommends against at this scale; a
  single-file export may be reconsidered as a publication format later).
- Treating renderer likes/tags/prompts as ontology; they stay observed
  renderer metadata.

## Acceptance criteria

- Hybrid search (lexical + semantic, explicit UNAVAILABLE without the
  embedding endpoint) over all 825 observed clips, with CJK-safe analysis.
- At least one classifier program consuming `suno-meta-v1.edn` and emitting
  derived feature events into `ledgers/classification.edn`.
- The liked index is always reported as json-generation-scoped (274 clips).
- No silent merges from similarity; all projections rebuildable from ledgers.

## Verification

- `clojure -M:test` green with new adapter law-suite coverage.
- A recorded search session against the real corpus (query examples in
  receipts) demonstrating lexical, semantic, and hybrid paths.