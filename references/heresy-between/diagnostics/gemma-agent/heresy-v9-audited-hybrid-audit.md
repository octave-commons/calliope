# Heresy Between OpenUTAU v9 audited-hybrid render audit

Date: 2026-05-14

## Goal

Proceed from the v6 opening proof into full-song chunk rendering and audit while preserving prior branches. The target is not final musical polish; it is an intelligible OpenUTAU/Ritsu reference canvas whose lyrics are generated from one kana mora per sung note and can be checked against the isolated vocal.

## Inputs

- Original isolated vocal truth: `/home/err/devel/Music/fork-tales/references/heresy-between/stems/htdemucs/存在論的な“反・虚無”の論証として/vocals.wav`
- v6 manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-mora-preroll-openutau-v6.json`
- v7 manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-segment-boundary-openutau-v7.json`
- v8 manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-lyric-clear-openutau-v8.json`
- v9 manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-audited-hybrid-openutau-v9.json`

## Branches produced this pass

### v6 fixed-boundary chunks

Rendered raw WAVs for chunks 02-05 in:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v6-mora-preroll-chunks/`

Result: rendering succeeded, but first-phrase STT showed chunks 02/03 were not reliable. The root cause was chunk boundaries still cutting lyric segments mid-line:

- boundary 58.0s split segment 10: `タイムラインには知らない誰かの笑顔` (`55.26–59.44`)
- boundary 106.0s split segment 21: `それでもいちぎょうだけでせかいに参加していたい` (`103.64–108.90`)
- boundary 174.0s split segment 37: `それでじゅうぶんだ` (`173.44–176.88`)
- boundary 244.0s split segment 39: `返事なんてもう来なくても` (`243.36–246.14`)

### v7 segment-boundary chunks

Generator:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v7-segment-chunks.py`

Output dir:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v7-segment-boundary-chunks/`

Change: moved chunk edges to transcript segment starts/ends so no chunk begins mid-phrase. Chunks render successfully, but chunks 02/03 still had poor word recognition under local STT/Gemma when retaining source-pitch estimates.

### v8 lyric-clear chunks

Generator:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v8-lyric-clear.py`

Output dir:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v8-lyric-clear-chunks/`

Change: kept v7 segment-safe chunking but replaced source-pitch estimates with stable tone 60 everywhere. This improved chunk 02/03 recognition but weakened the opening STT, so it became evidence for a hybrid strategy rather than the final branch.

### v9 audited-hybrid chunks and full canvas

Generator:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v9-audited-hybrid.py`

Output dir:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v9-audited-hybrid-chunks/`

Full assembled WAV:

`/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-audited-hybrid-openutau-v9.wav`

Duration: `303.320000s`

Strategy:

- preserve one sung note per kana mora;
- preserve segment-boundary chunking from v7;
- use source pitch except segments `10–36`;
- use stable tone 60 for segments `10–36`, because v7 failed there and v8 improved recognition;
- trim each chunk's 0.5s preroll and pad/trim to exact chunk-span duration before concatenating.

## Render verification

All v9 raw chunks rendered successfully after one retry for chunk 03, which was killed once with exit 137 and then succeeded on retry.

| chunk | raw WAV duration | aligned duration |
|---|---:|---:|
| 01 | 55.507506s | 55.260000s |
| 02 | 48.877506s | 48.380000s |
| 03 | 60.003764s | 69.800000s |
| 04 | 70.422517s | 69.920000s |
| 05 | 37.618753s | 59.960000s |
| full | n/a | 303.320000s |

Note: aligned chunks are preroll-trimmed and padded/trimmed to manifest span, preserving full-song timeline gaps.

## STT audit highlights

Local STT service: `http://127.0.0.1:8010/transcribe-timed`

### Original vocal controls

| region | original STT |
|---|---|
| 55.26s + 12s | `タイムラインには知らない誰か タイムラインには知らない誰かの笑顔 強がってスクロールして心だけ取り残す 打ち掛けの言葉が` |
| 103.64s + 12s | `それでも一協だけで世界に参加してたいもしも明日ただの友達で笑えたらそれだけでいい` |

These controls confirm the STT can recognize the source vocal at the same song positions.

### v9 full assembly audit

| region | expected anchor | v9 full STT | assessment |
|---|---|---|---|
| 0.00s + 12s | `元気だよって一行だけのメッセージ` | `元気だよって一行だけのメッセージ 既読のアイガネおそこたいてういえなからごめん` | pass for opening anchor |
| 55.26s + 12s | `タイムラインには知らない誰かの笑顔` | `タイムラインには知らない誰かの笑顔強って苦しここだトリコ打ち明けの言葉が振る` | pass for first line anchor; later words degraded |
| 103.64s + 12s | `それでも一行だけで世界に参加していたい` | `それでも一行だけで正義に参加していたいもしもあったただの友達で笑ったらそれだでいい` | pass for sentence structure; `世界` heard as `正義`, `明日` as `あった` |
| 173.44s + 8s | `それで十分だ` | `それで十分だ` | pass |
| 243.36s + 12s | `返事なんてもう来なくてもするよ` | `返事なんてもう来なくてもするよ一受けせ` | pass for closing anchor; trailing phrase degraded |

## Key findings

1. v6 solved the original lyric-cycling bug but retained unsafe fixed chunk boundaries.
2. v7 solved chunk-boundary splits, but source-pitch estimates still hurt recognition in the mid-song sections.
3. Controlled phrase tests showed Ritsu can render `タイムラインには知らない誰かの笑顔` recognizably at the same mora speed when pitch is stable.
4. v8 stable pitch improved mid-song recognition, proving source-pitch variation was the immediate failure mode for chunks 02/03.
5. v9 hybrid is the best branch so far: it preserves the good opening/source-pitch behavior and applies stable pitch only to the audited failure zone.

## Limitations

- v9 is not line-perfect across every word. It passes the major first-phrase anchors, but some internal words remain degraded.
- `世界` is heard as `正義` in chunk 03; this is likely a phonetic/ASR ambiguity caused by the current kana-only mora plan and Ritsu articulation.
- Small-tsu handling remains imperfect because the current branch drops `っ` rather than explicitly modeling consonant gemination.
- v9 is a lyric-intelligibility reference branch, not a final expressive vocal arrangement.

## Next recommended iteration

Build v10 as a phrase-level refinement branch that keeps v9 chunking/pitch policy but adds explicit handling for difficult mora clusters and small-tsu/geminate consonants, starting with the chunk 03 `世界`/`正義` ambiguity and chunk 02 `強がってスクロールして心だけ取り残す` degradation.
