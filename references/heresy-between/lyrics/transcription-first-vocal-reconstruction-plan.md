# Transcription-First Vocal Reconstruction Plan

## Source Correction

The isolated vocal stem is the source of truth for the sung words, ordering, repeats, pauses, and delivery:

`/home/err/devel/Music/fork-tales/references/heresy-between/stems/htdemucs/存在論的な“反・虚無”の論証として/vocals.wav`

The prompt/lyrics text file is a reference only. It must not be used to force section order or chorus repetition into the reconstruction. The rejected `heresy-between-ritsu-lyric-vocal-v2.*` branch did exactly that and is therefore not a valid vocal-truth branch.

## What The Current Medium Pass Proves

Fresh Knoxx Whisper medium artifacts:

- Raw vocal: `alignment.knoxx-whisper-medium-vocals-20260514.json`
- Denoised/normalized: `alignment.knoxx-whisper-medium-vocals-denoise-norm-20260514.json`
- Compressed: `alignment.knoxx-whisper-medium-vocals-compressed-20260514.json`
- Gated: `alignment.knoxx-whisper-medium-vocals-gated-20260514.json`
- Comparison: `alignment.knoxx-whisper-medium-preprocess-comparison-20260514.json`
- Selected rough layer: `word-timing-layer-v1-medium-gated.json`

The gated preprocessing variant is the best medium run for rough segment timing:

- `91.3%` segment coverage before cleanup.
- `94.72%` word coverage before cleanup.
- No raw-pass `HIP HIP` hallucinated tail.
- After hallucination cleanup: `78` usable segments, `88%` segment coverage.

However, medium word-level output is not good enough for exact word/syllable placement. The selected layer has only `58` usable word entries after cleanup, so it is a segment scaffold, not a final word layer.

## Required Two-Pass Pipeline

### Pass 1: Words And Timings

Goal: recover what was actually sung, when, from `vocals.wav`.

Inputs:

- Original isolated vocal stem.
- Speech-focused preprocessing variants that preserve timeline.
- Knoxx `/transcribe-timed` outputs.

Outputs:

- Segment timing layer with confidence and rejected hallucination spans.
- Word/syllable timing layer only after a stronger ASR/alignment pass succeeds.

Rules:

- Do not force the prompt lyric file into the result.
- Use the prompt lyric file only as a weak sanity reference for likely vocabulary.
- Reject hallucinated tails and non-song prompt/meta text.
- Preserve repeat structure from audio, even if it differs from the lyric prompt.

### Pass 2: Notes, Tone, And Delivery Within Word Windows

Goal: extract pitch and delivery only inside trusted word/segment windows.

Inputs:

- Selected word/segment timing layer.
- Original isolated vocal stem.
- pYIN/CREPE/torchcrepe f0 within each timing window.

Outputs:

- Per-word or per-syllable f0 median/range/confidence.
- Phrase contour labels: spoken, sung, held, breath, ghost/choir, noise.
- OpenUTAU note events constrained to actual sung windows.

Rules:

- Never use global pYIN collapse as the performance note source.
- Extract f0 only where the word/segment layer says voice exists.
- For unreliable f0, preserve timing and mark pitch as approximate instead of dropping the phrase.

## Bigger Whisper Escalation

Medium is useful for rough timing but insufficient for final Japanese lyric recognition on this sung vocal. Next escalation should be one of:

- Run a larger Whisper model as a one-off batch job outside the PM2 realtime service.
- Add a separate `STT_TIMED_MODEL_ID` experiment only with explicit approval if service config changes or PM2 restart is required.
- Evaluate an alternate forced-alignment/ASR stack that can take the isolated vocal and produce better Japanese word timing.

Do not replace the live `.en` realtime model. This is a batch song-reference path only.

## Bigger Whisper Result

Added a one-off `faster-whisper` batch environment under:

`/home/err/devel/Music/fork-tales/references/mir-workbench/.venv-faster-whisper`

Reusable script:

`/home/err/devel/Music/fork-tales/references/heresy-between/lyrics/run-faster-whisper-batch.py`

CUDA `large-v3-turbo` failed to load with GPU out-of-memory on the 8GB laptop GPU. For future CUDA attempts, first evict MusicGen with `POST http://127.0.0.1:8083/unload`; longer term, the MusicGen server should self-evict after an idle timeout.

Completed CPU int8 run:

`/home/err/devel/Music/fork-tales/references/heresy-between/lyrics/alignment.faster-whisper-large-v3-turbo-gated-cpu-int8-20260514.json`

Comparison:

`/home/err/devel/Music/fork-tales/references/heresy-between/lyrics/alignment.large-vs-medium-comparison-20260514.json`

Cleaned best-current word scaffold:

`/home/err/devel/Music/fork-tales/references/heresy-between/lyrics/word-timing-layer-v2-large-turbo-gated-cleaned.json`

Large-v3-turbo improves Japanese lyric recognition and real word timestamps versus OpenVINO medium, especially at the opening and middle sections. It still hallucinates or degrades near the tail/final chorus, so the cleaned v2 word layer rejects those spans and remains a scaffold, not final sung truth.

## Current Decision

- Reject `heresy-between-ritsu-lyric-vocal-v2.*` as a truth source because it was generated from the prompt lyric file instead of the isolated sung vocal.
- Keep it only as a renderability/chunking proof.
- Use `word-timing-layer-v1-medium-gated.json` only as a rough segment timing scaffold.
- Prefer `word-timing-layer-v2-large-turbo-gated-cleaned.json` for current word-window constrained f0 work, but treat rejected tail spans as unresolved.
- Next artifact should extract f0/tone only inside accepted v2 large-word windows, while separately targeting final chorus/tail with another ASR/alignment pass.
