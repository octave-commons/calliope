---
uuid: "ft-ops-004-untie-media-from-git-lfs"
title: "FT-OPS-004: Untie media from git LFS onto a manifest-addressed dataset"
status: incoming
type: chore
priority: P0
phase: 0
owner: unassigned
points: 3
labels: media-dataset, storage, migration
category: chores
adr: "docs/adrs/adr-002-manifest-addressed-external-media-dataset.md"
process: "docs/process/product-design-and-delivery.md"
---

# FT-OPS-004: Untie media from git LFS onto a manifest-addressed dataset

## Outcome

The corpus media bytes under `tracks/` leave git LFS entirely. The repo tracks
only text truth (`MANIFEST.edn`, Suno JSON metadata, ledgers); the MP3/JPEG
bytes form a self-describing dataset that any folder can host, synchronized to
a personal Google Drive via rclone, and the software resolves the dataset root
portably.

## Scope

- Dataset contract and manifest format (ADR-002): `calliope.media.dataset`
  library, `calliope.law.media` Malli contracts, `bb scripts/media.clj`.
- `corpus.clj tracks` writes dataset-relative `:dest` plus full `:sha256` and
  `:dataset/id`, and regenerates the manifest after each run.
- One-time migration: generate `MANIFEST.edn` from the 1633 materialized LFS
  files, `git rm --cached` the media, drop LFS filters from `.gitattributes`,
  ignore media patterns in `.gitignore`, initial `rclone sync` to
  `gdrive:calliope-media`.
- Verification: `bb scripts/media.clj verify --ledger` and `rclone check`.

## Non-goals

- Rewriting git history or force-pushing; the ~4.2 GB already in GitHub LFS
  storage stays until a separate, explicitly approved purge.
- Absorbing the heresy-between renders dataset (`RENDERS-MANIFEST.edn`).
- Any UI or playback behavior; FT-001A consumes the dataset later.

## Acceptance criteria

- `clojure -M:test` passes including the new dataset and law suites.
- `bb scripts/media.clj verify` reports `:ok true` with hash checking on the
  full dataset.
- `bb scripts/media.clj verify --ledger` reports no `:missing-from-manifest`
  and no `:bytes-drift`.
- `git ls-files` no longer lists any `tracks/**/*.mp3` or `tracks/**/*.jpeg`.
- `.gitattributes` contains no LFS filters.
- `rclone check --one-way` exits zero against `gdrive:calliope-media`.
- A receipt records the migration with evidence.

## Verification

```bash
clojure -M:test
bb scripts/media.clj where
bb scripts/media.clj verify --ledger
git ls-files -- ':(glob)tracks/**/*.mp3' ':(glob)tracks/**/*.jpeg'
bb scripts/media.clj check
```
