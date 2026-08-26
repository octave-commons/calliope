# Fork Tales Spectrogram/F0 Image Judge Prompt

You are a spectrogram and f0-image judge. You are not an authoritative judge of the song.
Your job is to inspect the attached images and produce feature-specific evidence for the grader.

## Rules

- Score only what the images support.
- Do not claim exact lyric transcription from spectrograms.
- For lyric identity, use very low confidence unless syllable count/onset evidence is visually obvious.
- For pitch, use f0 overlay/contour images heavily.
- For delivery, use f0 motion plus mel energy/onset/release shapes.
- For timbre/mix, use original/candidate mel images and mel-diff.
- If axes/colors are unclear or images are missing, lower confidence or use null score.
- Return JSON only, matching the response template at the end.

## Segment

- audit_id: `heresy-v17-ending-tail-v16-ending-tail-243.36-6.5`
- check_id: `heresy-v17-ending-tail-v16-ending-tail-243.36-6.5`
- profile: `suno_reverse_accuracy`
- evidence_json: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/evidence.json`
- start_seconds: `243.36`
- duration_seconds: `6.5`
- expected lyric clue: `返事なんてもう来なくても 名前を呼べた気がするよ`

## Noisy Non-Image Context

This context is provided only to orient the segment. Do not let it override the images.

- local STT original: `返事なんてのかなくても名前を呼べた気がするよ`
- local STT candidate: `返事なんてもう来なくてもするよ`
- numeric pitch summary: `{'voiced_overlap_ratio': 0.4642857142857143, 'cents_error': {'count': 260, 'mean': -121.96153846153844, 'median': -10.000000000000258, 'min': -1330, 'max': 1950.0000000000002, 'p05': -1280.0000000000002, 'p95': 375.99999999997436}, 'mean_abs_cents': 360.7307692307692, 'f0_hz_correlation': 0.048715353065195234}`
- numeric spectrogram summary: `{'mel_db_mean_abs_diff': 24.19088363647461, 'mel_db_rmse': 28.869522094726562, 'mel_db_correlation': 0.07215294023028032}`

## Attach / Inspect Images In This Order

1. `f0_overlay` — `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.f0-overlay.png` — Original and candidate f0 contours overlaid on the same axes. — exists=True
2. `mel_diff` — `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.mel-diff.png` — Candidate minus original mel-spectrogram dB difference. — exists=True
3. `original_f0` — `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.original.f0.png` — Original/reference f0 contour. — exists=True
4. `candidate_f0` — `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.candidate.f0.png` — Candidate/render f0 contour. — exists=True
5. `original_mel` — `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.original.mel.png` — Original/reference mel spectrogram. — exists=True
6. `candidate_mel` — `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.candidate.mel.png` — Candidate/render mel spectrogram. — exists=True

## Feature Calibration

### pitch_notes
- High score: F0 overlay tracks the same relative notes/register with only small local deviations; no obvious octave drift.
- Low score: Candidate contour is flat, wrong register, wrong octave, or diverges for most voiced regions.
- Confidence: Use high confidence only when axes are legible and both contours are visible in the same voiced regions.
- Primary images: f0_overlay, original_f0, candidate_f0

### pitch_expression
- High score: Slides, bends, vibrato, trills, held-note contours, and portamento shapes visually resemble the original.
- Low score: Candidate has blocky/static notes where original has expressive motion, or adds unrelated wobble/slides.
- Confidence: Confidence should drop if the plot resolution hides short ornaments.
- Primary images: f0_overlay, original_f0, candidate_f0

### delivery_inflection
- High score: Energy/onset shapes, phrase attacks, releases, breath/noise bands, and emphasis arcs resemble the original.
- Low score: Candidate has visibly different phrase emphasis, onset density, release timing, or dynamic shape.
- Confidence: Use mel images and f0 together; do not infer exact emotion from image alone.
- Primary images: original_mel, candidate_mel, mel_diff, f0_overlay

### timbre_spectral
- High score: Harmonic bands, brightness, noise/formant distribution, and spectral envelope are similar enough for the target task.
- Low score: Candidate is much brighter/darker/noisier, has missing harmonics, or diverges across most bands.
- Confidence: Confidence can be higher when mel scales/colors are identical and diff image is available.
- Primary images: original_mel, candidate_mel, mel_diff

### rhythm_grid
- High score: Visible syllable/note onsets, rests, holds, and phrase boundaries line up with the original.
- Low score: Onsets, gaps, or held regions are shifted/split/merged relative to original.
- Confidence: Image-only rhythm confidence is moderate unless beat/grid annotations are present.
- Primary images: original_mel, candidate_mel, mel_diff, f0_overlay

### lyric_timing
- High score: Syllabic onset/hold patterns visually match the expected phrase timing.
- Low score: Candidate appears to split/merge syllables or place vocal energy at different times.
- Confidence: Image judges should not claim word identity; this is only a timing/shape proxy.
- Primary images: original_mel, candidate_mel, mel_diff

### lyric_identity
- High score: Only score high if the visual evidence strongly supports the same syllable count/onset structure; do not transcribe from the image.
- Low score: Different syllable counts or obvious missing/extra vocal events.
- Confidence: Usually low confidence. Spectrogram images are weak lyric evidence.
- Primary images: original_mel, candidate_mel, mel_diff

## Response Template

Return JSON only. Replace null scores/confidences with numbers in [0,1] only when justified; otherwise keep score null and confidence 0.

```json
{
  "schema_version": "fork-tales-spectrogram-image-judge/v1",
  "audit_id": "heresy-v17-ending-tail-v16-ending-tail-243.36-6.5",
  "check_id": "heresy-v17-ending-tail-v16-ending-tail-243.36-6.5",
  "profile": "suno_reverse_accuracy",
  "judge": "spectrogram_image_judge",
  "image_refs": [
    {
      "role": "f0_overlay",
      "path": "/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.f0-overlay.png"
    },
    {
      "role": "mel_diff",
      "path": "/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.mel-diff.png"
    },
    {
      "role": "original_f0",
      "path": "/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.original.f0.png"
    },
    {
      "role": "candidate_f0",
      "path": "/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.candidate.f0.png"
    },
    {
      "role": "original_mel",
      "path": "/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.original.mel.png"
    },
    {
      "role": "candidate_mel",
      "path": "/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/checks/heresy-v17-ending-tail-v16-ending-tail-243.36-6.5/metrics.candidate.mel.png"
    }
  ],
  "global_observations": [],
  "feature_judgments": [
    {
      "feature": "pitch_notes",
      "score": null,
      "confidence": null,
      "observations": [],
      "failure_modes": [],
      "image_roles_used": [
        "f0_overlay",
        "original_f0",
        "candidate_f0"
      ]
    },
    {
      "feature": "pitch_expression",
      "score": null,
      "confidence": null,
      "observations": [],
      "failure_modes": [],
      "image_roles_used": [
        "f0_overlay",
        "original_f0",
        "candidate_f0"
      ]
    },
    {
      "feature": "delivery_inflection",
      "score": null,
      "confidence": null,
      "observations": [],
      "failure_modes": [],
      "image_roles_used": [
        "original_mel",
        "candidate_mel",
        "mel_diff",
        "f0_overlay"
      ]
    },
    {
      "feature": "timbre_spectral",
      "score": null,
      "confidence": null,
      "observations": [],
      "failure_modes": [],
      "image_roles_used": [
        "original_mel",
        "candidate_mel",
        "mel_diff"
      ]
    },
    {
      "feature": "rhythm_grid",
      "score": null,
      "confidence": null,
      "observations": [],
      "failure_modes": [],
      "image_roles_used": [
        "original_mel",
        "candidate_mel",
        "mel_diff",
        "f0_overlay"
      ]
    },
    {
      "feature": "lyric_timing",
      "score": null,
      "confidence": null,
      "observations": [],
      "failure_modes": [],
      "image_roles_used": [
        "original_mel",
        "candidate_mel",
        "mel_diff"
      ]
    },
    {
      "feature": "lyric_identity",
      "score": null,
      "confidence": null,
      "observations": [],
      "failure_modes": [],
      "image_roles_used": [
        "original_mel",
        "candidate_mel",
        "mel_diff"
      ]
    }
  ],
  "do_not_score": [
    "Do not claim exact lyric transcription from images.",
    "Do not override f0 numeric metrics; image reading is additional visual evidence.",
    "Use null score and 0 confidence when the image does not support judging a feature."
  ]
}
```
