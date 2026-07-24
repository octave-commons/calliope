---
name: fork-tales-corpus
description: Work with the fork_tales_v2 lyric corpus — the event-sourced ingest ledger, dedup projection, and songbook conventions. Use whenever touching docs/lyrics, ledgers/, or running corpus scripts in this repo.
---

# fork_tales corpus

## The one rule

`docs/lyrics/` is a **projection**, never a source. All truth lives in
`ledgers/ingest.edn` (append-only EDN, one event per line). To change the
songbook, change the pipeline or the sources — then re-project.

## Pipeline

```
bb scripts/corpus.clj ingest    # append :doc/discovered events (latest run wins for projection)
bb scripts/corpus.clj project   # rebuild docs/lyrics + ledgers/projections/songs-v1.edn
bb scripts/corpus.clj stats     # classification counts for latest run
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
specs, briefs) · `:lore` (fork_tales/Lore world docs) · `:other`.

Projection groups `:suno-lyric`+`:hand-lyric` by `:body-sha256` (file text
minus the Suno `ID:` line, whitespace-normalized), picks a canonical source
(`~/Music` > octave-commons/fork_tales > Lore > devel > Downloads), copies it
verbatim to `docs/lyrics/<slug>.<ext>`.

## Known corpus facts (pass 1, 2026-07-24)

- 690 unique songs from 1,522 lyric files (832 duplicates collapsed).
- Same-title hash-suffixed siblings (e.g. `manifest-oath-84-bpm-<hash8>.txt`)
  are Suno re-render variants — pass 2 (edit distance) territory, NOT bugs.
- `:pasted-artifact` flags = Suno "songs" whose body is a pasted chat log or
  zip manifest. Kept (they were rendered), but filter them for lore analysis.

## Working with ledgers

Use the global `edn_ledger` muse tool (append/tail/query/count/filter) for
ad-hoc reads and writes of `ledgers/*.edn` — never `echo >>` by hand.
