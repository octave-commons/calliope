# Audio Reconstruction — the Scribe-Mode loop

Consolidated 2026-07-27 from four scattered locations in `riatzukiza/devel`.
This directory is now the only place the reconstruction system is defined.

## What this is

Given an owned Suno render, recover it as *local, executable, inspectable*
music: stems, pitch, lyric timing, a kana performance layer, a USTX vocal plan,
an OpenUTAU render, a mix — and a graded verdict on whether the result is
actually faithful.

The governing rule lives in [`operating-model.md`](./operating-model.md):

> No tool is the judge by itself. Every tool emits bounded evidence; the
> adjudication layer scores claims against a rubric using weighted, contestable
> evidence.

That is the same law the corpus sings in [`../lore/world.md`](../lore/world.md):
the Gate opens only for what survives the hook, `μ` demotes to `η` on drift, and
nothing calls itself `μ` until it survives two echoes. Reconstruction is that
law applied to a waveform.

## The loop

```text
source render
  -> stem split            (Demucs / htdemucs)
  -> instrument transcribe (Basic Pitch on bass/other/piano)
  -> vocal pitch           (pYIN / torchcrepe f0 -> note events)
  -> lyric timing          (Whisper stt-npu /transcribe-timed)
  -> kana performance layer
  -> USTX
  -> OpenUTAU render
  -> mix
  -> Gemma Check -> grade -> QC review -> restart with feedback
```

Two rules that are easy to get wrong, both from `operating-model.md`:

- **Gemma is not the judge.** `gemma4:e4b` is a cheap local pre-review checker —
  lint, not sign-off. `gemma-check` is the command name; `audit` is a legacy
  alias for the same thing.
- **Lyrics never define timing.** The isolated vocal stem is the timing and pitch
  evidence. The lyric file is a reference dictionary for *text*, nothing else —
  not repetitions, rests, breaths, holds, or phrase tails.

## Where everything is

| Piece | Path |
|---|---|
| Operating model, roles, handoff | [`operating-model.md`](./operating-model.md) |
| Rubric (prose) | [`rubrics.md`](./rubrics.md) |
| Rubric weights (data) | [`../../resources/reconstruction/rubrics.json`](../../resources/reconstruction/rubrics.json) |
| Handoff schemas (data) | [`../../resources/reconstruction/handoff-schemas.json`](../../resources/reconstruction/handoff-schemas.json) |
| Malli μ registry | [`../../src/fork_tales/law/audio.cljc`](../../src/fork_tales/law/audio.cljc) |
| Reconstruction contract (prose) | [`reconstruction-contract.md`](./reconstruction-contract.md) |
| Gemma agent workflow | [`gemma-audio-agent.md`](./gemma-audio-agent.md) |
| Validated-reference category theory | [`validated-reference-category.md`](./validated-reference-category.md) |
| Suno → OpenUTAU/MIDI pipeline | [`suno-to-openutau-midi-reference.md`](./suno-to-openutau-midi-reference.md) |
| Separator survey | [`audio-separation-tools.md`](./audio-separation-tools.md) |
| Agent contracts | [`../../resources/reconstruction/contracts/`](../../resources/reconstruction/contracts/) |
| Programs | [`../../scripts/reconstruction/`](../../scripts/reconstruction/) |
| Runtime boundary (bb vs JVM) | [`runtime-split.md`](./runtime-split.md) |
| Event ledger law | [`../../src/fork_tales/law/reconstruction.cljc`](../../src/fork_tales/law/reconstruction.cljc) |
| Evidence artifacts | [`../../references/heresy-between/`](../../references/heresy-between/) |

Handoff validation is Clojure: `scripts/reconstruction/validate.clj` over the
pure interpreter in `src/fork_tales/reconstruction/handoff.cljc`. The remaining
Python tools are graders and judges, not the pipeline — `audio_grade.py`,
`audio_metrics.py`, `spectrogram_image_judge.py` — and are being ported. The
NBB runner `audio_agent.cljs` drives the Gemma Check lane.

## Artifacts: what is tracked and what is not

`references/heresy-between/` holds **426 files, 15M** — every `.json`, `.ustx`,
`.mid`, `.edn`, `.csv`, `.md`, and generator `.py`/`.mjs`. That is the evidence
and the programs: f0 contours, note fixtures, alignment JSON, USTX vocal plans,
coverage diagnostics, per-version manifests.

Renders — **540 files, 4.6G** of `.wav`/`.png`/`.mp3` — are deliberately **not**
tracked. They are regenerable output, and this repo already carries 4.2G of
corpus LFS. They live at:

```
~/Music/fork-tales/references/heresy-between/
```

Every one is recorded with size and `sha256` in
[`../../references/heresy-between/RENDERS-MANIFEST.edn`](../../references/heresy-between/RENDERS-MANIFEST.edn),
so provenance stays inspectable without the bytes. Ledger discipline asks that
content hashes remain checkable; it does not ask that they be re-uploaded.

## A path note that matters

Every doc, script, and contract in this directory has had its paths corrected.
The old tree said `/home/err/devel/Music/fork-tales/...`; the artifacts have
always actually been at `/home/err/Music/fork-tales/...`. That single wrong
segment is why this system became unfindable — following the documented paths
landed in a near-empty directory.

**The 426 imported artifacts were not rewritten.** They still contain the old
`devel/Music` strings. They are historical records of runs that really happened,
and the ledger is append-only: correcting evidence after the fact would destroy
the provenance it exists to hold. Read them as written, translate on the way out.

## Where the loop actually stands

Ten full reconstruction versions (through `v10-melband-hybrid-body`) and sixteen
OpenUTAU iterations (through `v16-exact-phrase-grid`) exist, with A/B diagnostic
sets per version.

It is stalled on the vocal, and the diagnosis is already written down in
`references/heresy-between/reconstructions/openutau/heresy-between-vocal-repair-plan-v2.md`:

- note coverage **34.72s / 303.32s — 11.45%**
- **13** gaps longer than 5s
- worst gaps `261.44–282.54s` and `282.73–303.32s`, which erase the final chorus
  and the outro entirely

The pYIN collapse produced islands of notes and the USTX cycled placeholder kana
over them. It proves OpenUTAU renders; it does not produce a performance. Plan v2
is to replace it with a lyric-aware, section-anchored Ritsu VCV pass. That is the
next move, and it is not blocked by the items below.

## Blockers, recorded not inferred

Observed 2026-07-27, and mirrored in
`resources/reconstruction/contracts/fork_tales_audio_reconstruction.edn`
under `:availability`:

| What | Status | Basis |
|---|---|---|
| OpenUTAU patched exporter | `:unavailable` | path absent; `.opencode/` missing from the checkout. Source is present and clean at `cb393b01` — the patched build was never committed and needs rebuilding. |
| Demucs | `:unavailable` | not importable; no venv provides it. Existing htdemucs stems are on disk, so analysis still works; re-splitting does not. |
| Metrics / DSP lane | **available** | venv rebuilt 2026-07-27 at `~/Music/fork-tales/references/mir-workbench/.venv`; `audio_metrics.py` verified to reproduce committed metrics. See [`runtime-split.md`](./runtime-split.md). |
| `gemma4:e4b-128k` | `:unavailable` | `192.168.12.68:11434` listed only `nomic-embed-text`. Needs pulling before Gemma Check runs. |

An absent binary is `:unavailable`. It is never a successful empty result.

## Commands

Require a harness with a local shell, a JVM/NBB toolchain, and the declared
endpoints. Their presence here does not prove the current harness can run them.

```bash
bb scripts/reconstruction/validate.clj PACKET...   # μ1-μ6 handoff invariants
clojure -M:test                                    # law + interpreter tests
clojure -M:metrics ...                             # DSP lane (librosa via libpython-clj)
```

The pipeline is event-sourced: each lane appends to `ledgers/reconstruction.edn`
rather than returning a value, and the babashka and JVM lanes communicate only
through that file. See [`runtime-split.md`](./runtime-split.md) — in particular,
libpython-clj cannot run under babashka, and no pod is needed to work around it.

All five scripts are verified to run — see the port table in
[`runtime-split.md`](./runtime-split.md). Still Python, not yet ported:
`audio_grade.py` and `spectrogram_image_judge.py` (both pure stdlib, direct
translations) and `audio_metrics.py` (librosa + matplotlib, belongs behind
`-M:metrics`).

`audio_agent.cljs` and `handoff-schemas.json` were also repaired here: a historical
secret-scrub had replaced the literals `node` and `root` with `REDACTED_SECRET`,
breaking `node:fs`/`node:path`/`node:child_process` requires and the
`artifact-root`/`root-dir` bindings, and had corrupted `publication_refs` in the
schema. Originals were recovered from pre-purge git blobs, not guessed.

## Still elsewhere

Deliberately out of scope for this consolidation, and still living in devel:

- The wider Knoxx voice/persona contract library — `contracts/fork-tales/`
  personas, themes, instruments, environments, voices (Ritsu, Teto), plus the
  music roles and capabilities. Runtime contracts for a running service; moving
  them is a separate decision.
- `~/Music/fork-tales/` beyond `references/`: `projects/`, `renders/`,
  `instrumentals/`, `corpus/`, ~50 loose `.ustx`/`.wav` files, and
  `VOICE_SYSTEM_PROGRESS.md`.
- The duplicated lore trees in devel (`Lore/`, `LORE/`, `Lorg/`, `Audio/`,
  `Voice/`, `projects/vaults-fork-tales`).
