---
name: calliope-corpus
description: Work with the calliope_v2 lyric corpus — the event-sourced ingest ledger, dedup projection, and songbook conventions. Use whenever touching docs/lyrics, ledgers/, or running corpus scripts in this repo.
---

# calliope corpus

This is an OpenCode-local skill. Root `PROCESS.md` remains authoritative for
harness-neutral process, receipts, evidence, and fallback behavior.

## The one rule

`docs/lyrics/` is a **projection**, never a source. All truth lives in
`ledgers/ingest.edn` (append-only EDN, one event per line). To change the
songbook, change the pipeline or the sources — then re-project.

## Pipeline

```
bb scripts/corpus.clj ingest    # append :doc/discovered events (latest run wins for projection)
bb scripts/corpus.clj project   # rebuild docs/lyrics + ledgers/projections/songs-v1.edn
bb scripts/corpus.clj stats     # classification counts for latest run
bb scripts/corpus.clj variants  # pass 2: same-title clusters → :doc/variant-cluster events
```

## Event shapes

```clojure
{:event/id "uuid" :event/type :doc/discovered :run/id "uuid" :ts "iso"
 :path "/abs/path" :sha256 "raw-bytes" :body-sha256 "normalized"
 :bytes 1234 :classification :suno-lyric :title "..."
 :basis [:suno-header] :flags [:pasted-artifact]}
```

Classes: `:suno-lyric` (Title/ID/Prompt header) · `:hand-lyric` (≥2 line-anchored
section tags, or 1 tag + lyric-ish name/path) · `:lyric-adjacent` (style prompts,
specs, briefs) · `:lore` (calliope/Lore world docs) · `:other`.

Projection groups `:suno-lyric`+`:hand-lyric` by `:body-sha256` (file text
minus the Suno `ID:` line, whitespace-normalized), picks a canonical source
(`~/Music` > octave-commons/calliope > Lore > devel > Downloads), copies it
verbatim to `docs/lyrics/<slug>.<ext>`.

Treat those local paths as available only after checking the active machine.
They are historical source preferences, not portable harness capabilities.

## Known corpus facts (pass 1, 2026-07-24)

- 690 unique songs from 1,522 lyric files (832 duplicates collapsed).
- Same-title hash-suffixed siblings (e.g. `manifest-oath-84-bpm-<hash8>.txt`)
  are Suno re-render variants — pass 2 has clustered them (148 same-title
  clusters, 413 songs): see `:doc/variant-cluster` ledger events and
  `ledgers/projections/variants-v1.edn`. Each edge carries a line-level
  Levenshtein `:similarity` (0..1) — a graded signal, never a merge.
- Pass-2 caveat: title-extraction artifacts create false clusters. Docs whose
  "title" is a generic markdown header (e.g. `## signal`, 13 members,
  mean-sim 0.07) are unrelated songs. Treat `:mean-similarity` < ~0.3 as
  "shared title only", ≥ ~0.8 as true re-render candidates.
- `:pasted-artifact` flags = Suno "songs" whose body is a pasted chat log or
  zip manifest. Keep them in the rendered corpus, but exclude them from thematic
  discovery and lore analysis by default. Include them only when deliberately
  requested.

## Working with ledgers

When the configured OpenCode environment exposes the global `edn_ledger` Muse
tool, use it for governed `ledgers/*.edn` reads and appends. Use
`receipt_river` for root `receipts.edn` after substantive work.

If either tool is unavailable, follow the exact append-only fallback in
`PROCESS.md` and record the unavailability. Never use ad-hoc `echo >>` writes,
never edit prior events, and never claim a global plugin was used merely because
this skill mentions it.
