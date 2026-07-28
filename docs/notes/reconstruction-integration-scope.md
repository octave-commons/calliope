# Reconstruction integration scope

Status: active integration note  
Date: 2026-07-28

## Purpose

Reconcile the audio-reconstruction lineage that landed on
`design/classifier-dsl-v1` after PR #1 had already merged, without flattening its
commit provenance into the media-workbench design branch.

## Integration boundary

Included:

- reconstruction laws, programs, contracts, documentation, and committed evidence;
- `references/heresy-between/` non-render evidence and render hash manifest;
- reconstruction ledger and path-translation rules;
- one Rheos story connecting reconstruction evidence to the media workbench;
- append-only union of both branch receipt histories.

Excluded:

- committed `.cpcache/` products;
- `.ημ/session-mycology/` run-local artifacts;
- the untracked 4.6G local render tree;
- any claim that the *remote* integration harness exercised local reconstruction
  tools or model endpoints. The remote harness verified structure only. Local
  execution came afterwards and is scoped below.

## Merge policy

The integration uses an explicit merge commit with the media-workbench head as
first parent and the reconstruction lineage as second parent. Shared authority
files are reconciled deliberately rather than selected wholesale:

- preserve the accepted media-workbench `AGENTS.md` and add reconstruction facts;
- preserve and append-union both `receipts.edn` histories;
- accept reconstruction dependency/runtime additions;
- fail on any unexpected textual conflict.

The stacked PR targets `design/media-workbench-v1` so PR #3 remains readable and
its design review does not silently absorb hundreds of evidence files.

## Local verification, 2026-07-28

The remote integration proved structure. It could not prove the lanes run, so
they were run afterwards on the host that owns the artifacts. What executed:

- `clojure -M:test` — 41 tests, 184 assertions, 0 failures, 0 errors.
- `bb scripts/reconstruction/preflight.clj` over all three committed
  `evidence.json` artifacts — 13 paths each, 12 translated + 1 resolved, exit 0;
  and exit 1 with 10 missing paths on a deliberately broken scratch copy.
- `bb scripts/reconstruction/validate.clj` — exit 0 on the committed v17 planner
  assignment, exit 1 with 9 errors on a hand-built invalid `final_release`
  packet, both agreeing with the recovered Python oracle on every error.
- `reconstruction.ledger` — appended `:handoff/validated` and
  `:evidence/preflighted` events under `FORK_TALES_ROOT`, read them back, and
  validated both against `:ft.rec/Event` in the Malli registry.
- `audio_grade.py` on the preflighted evidence — reproduced the committed
  `grade.json` exactly; only the translated `evidence_ref` strings differ.
- `audio_metrics.py compare` under the venv — 75 of 87 numeric fields
  bit-identical to the committed `metrics.json`, the other 12 within 6e-9, both
  input `sha256` matching and both f0 CSVs byte-identical.
- `spectrogram_image_judge.py prompt` and `scores`, and `audio_agent.cljs` under
  nbb.

What is still unavailable, with evidence rather than assumption: Demucs (no
importable module in either interpreter), the patched OpenUTAU exporter (no
checkout on this host), and the Knoxx STT endpoint at `127.0.0.1:8010`
(connection refused). `gemma4:e4b-128k` turned out to be **present** on the
declared endpoint, contradicting the 2026-07-27 blocker; the model is reachable
but the Gemma Check lane was still not run end to end, and no audio device or
playback path was touched. Findings and repairs are recorded in `receipts.edn`.
