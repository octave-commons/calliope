# Heresy Between opening word audit — v5 vs v6

Date: 2026-05-14

## Goal

Use local audio-analysis tools to determine why the OpenUTAU candidate does not reliably sing the opening "元気だよって一行だけのメッセージ" and produce a better reference branch.

## Inputs

- Original isolated vocal: `/home/err/devel/Music/fork-tales/references/heresy-between/stems/htdemucs/存在論的な“反・虚無”の論証として/vocals.wav`
- v5 OpenUTAU candidate: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-reading-aware-openutau-v5.wav`
- v6 chunk-01 raw candidate: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v6-mora-preroll-chunks/heresy-between-mora-preroll-openutau-v6-chunk-01-raw.wav`
- Controlled clear Ritsu test: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v6-mora-preroll-opening/ritsu-genki-clear-test.wav`

## Findings

### v5 failed the opening-word test

Gemma single-file, no-reference hearing of v5 0–6s:

- Response: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/responses/hear-heresy-opening-v5-word-audit-candidate-v5-no-clue-1778795883597.json`
- Heard: `なよ、いち、うら、おめ、じげ、きら、えい、きど`

Local OpenVINO Whisper timed STT of the same v5 slice:

- Response: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/responses/stt-heresy-opening-v5-word-audit-candidate-v5-stt-0-6-1778797407023.json`
- Heard: `I'm a little bit of a I'm a little bit of a I'm a little bit of a`

Conclusion: v5 is not a reliable word reference. It does not robustly surface `元気` to either local ear.

### Root cause in v5 generation

v5 assigned lyrics by cycling segment morae across pitch/gesture events:

- Segment 0 has 16 morae: `げ ん き だ よ て い ち ぎょ う だ け の め せ じ`
- Gesture layer gives 24 pitch events for the same segment.
- v5 reused `event_index % mora_count`, causing the phrase to restart inside the segment.

This created wrong/repeated sung syllables when pitch-event count exceeded lyric-mora count.

### v6 fixes the lyric strategy

Generated v6 artifacts:

- Full v6 generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v6-full.py`
- Full v6 manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-mora-preroll-openutau-v6.json`
- Full v6 chunks: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v6-mora-preroll-chunks/`
- Opening probe generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v6.py`

v6 policy:

1. One sung note per kana mora.
2. Never cycle/repeat lyrics to fill pitch events.
3. Use pitch events only to estimate note tone.
4. Add 0.5s render pre-roll to reduce first-phoneme clipping.

Opening v6 first lyrics:

`げ ん き だ よ て い ち ぎょ う だ け の め せ じ き ど く の あ か り が ...`

### v6 passes the local STT word test

Local OpenVINO Whisper timed STT of v6 chunk-01 raw 0–8s:

- Response: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/responses/stt-heresy-opening-v6-word-audit-candidate-v6-full-chunk01-raw-stt-0-8-1778797349719.json`
- Heard: `元気だよって一行だけのメッセージ 既読の愛が`

Controlled Ritsu VCV phrase test:

- USTX: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v6-mora-preroll-opening/ritsu-genki-clear-test.ustx`
- WAV: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v6-mora-preroll-opening/ritsu-genki-clear-test.wav`
- STT response: `/home/err/devel/Music/fork-tales/references/heresy-between/diagnostics/gemma-agent/responses/stt-openvino-ritsu-genki-clear-0-8.json`
- Heard: `元気だよって一行だけのメッセージ`

Conclusion: OpenUTAU/Ritsu can generate the opening words correctly when the project uses one note per mora and avoids lyric cycling.

## Remaining caveats

- Gemma sometimes hears v6 as `けんき`, while local Whisper hears `元気`; treat Gemma as useful but not authoritative for voiced/unvoiced Japanese consonants.
- `っ` is skipped because this Ritsu VCV bank does not expose an obvious `っ` oto alias; the following consonant still carries most of the perceptual cue.
- The v6 full branch is a lyric-first reference branch, not a final mix. Rhythm/pitch refinement comes after word correctness.

## Next reconstruction rule

Do not create future OpenUTAU branches by cycling lyrics over pitch events. Pitch events may split or bend notes only after the lyric/mora sequence is fixed; extra pitch notes must become slurs/continuations, not new sung syllables.
