# Heresy Between v17 Ending-Tail Repair Seed

## Scope

- Segment: `243.36s–249.86s`
- Original: `/home/err/devel/Music/fork-tales/references/heresy-between/stems/htdemucs/存在論的な“反・虚無”の論証として/vocals.wav`
- Candidate baseline: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-exact-phrase-grid-openutau-v16.wav`
- Expected lyric clue: `返事なんてもう来なくても 名前を呼べた気がするよ`

## Generated Check

- Check id: `heresy-v17-ending-tail-v16-ending-tail-243.36-6.5`
- Evidence: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/evidence.json`
- Metrics: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.json`
- Grade: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/grade-suno_reverse_accuracy.json`
- Image-judge prompt: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/spectrogram-image-judge-prompt.md`
- Planner assignment: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/handoffs/heresy-v17-ending-tail-planner-assignment.json`

Gemma model call did not complete (`fetch failed`), but deterministic local evidence was produced. This is still useful as a Primary-agent pre-review check seed; do not treat it as a Gemma pass/fail.

## Evidence Summary

Local STT:

- Original: `返事なんてのかなくても名前を呼べた気がするよ`
- Candidate: `返事なんてもう来なくてもするよ`

Interpretation: local STT is noisy (`のかなくても` likely for `もう来なくても`), but it preserves `名前を呼べた気がするよ` in the original and collapses the candidate to `するよ`. This is strong enough to target v17 at the dropped/collapsed name phrase, not strong enough to certify exact transcription.

Signal metrics:

- mean absolute pitch error: `360.73 cents`
- f0 correlation: `0.0487`
- voiced overlap ratio: `0.4643`
- mel RMSE: `28.87 dB`
- mel correlation: `0.0722`

Grade (`suno_reverse_accuracy`):

- overall: `0.4074` / `F`
- confidence: `0.1155`
- coverage: `0.3104`
- promote: `false`
- critical gates:
  - lyric identity: `false`
  - lyric timing: `true` but low confidence/coverage
  - pitch notes: `false`
  - rhythm grid: `false`

## Planner Decision

Create v17 as a focused ending-tail repair, not a whole-song render.

Primary repair targets:

1. Preserve the phrase `名前を呼べた気がするよ` after `返事なんてもう来なくても`.
2. Improve pitch contour before claiming any musical/pitch success.
3. Preserve v16 baseline; write new v17 artifacts only.

Suggested first Primary-agent action:

1. Isolate `unit-07-ending` from v16.
2. Build a phrase probe for `返事なんてもう来なくても 名前を呼べた気がするよ` with exact duration around `6.5s`.
3. Use the v10/v16 exact-phrase-grid technique rather than editing the full ending first.
4. Re-run `gemma-check`, `grade`, and image-judge prompt on the probe.

## Blockers / Caveats

- Gemma Check model transport failed in this run (`fetch failed`), so no Gemma audio judgment exists yet.
- STT is a noisy sensor; the target phrase should be validated with audio, USTX grid, f0 metrics, and ideally Gemma/image judge once available.
- Pitch metrics are very poor, so a lyric-only v17 fix must not be described as pitch-correct.
