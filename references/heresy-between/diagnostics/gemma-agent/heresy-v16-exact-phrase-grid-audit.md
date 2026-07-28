# Heresy Between OpenUTAU v16 exact-phrase-grid audit

Date: 2026-05-15

## Purpose

Continue the v11/v12 plan: fold the successful phrase-isolated renders back into a reproducible generated branch instead of keeping them as ad-hoc audio patches.

## New durable artifacts

- v12 render-unit generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v12-render-units.py`
- v13 tail-context generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v13-tail-context.py`
- v14 custom-tail generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v14-custom-tail.py`
- v15 exact-tail-grid generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v15-exact-tail-grid.py`
- v16 exact-phrase-grid generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v16-exact-phrase-grids.py`
- Generic render-unit assembler: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/assemble-render-unit-openutau.py`
- v16 manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-exact-phrase-grid-openutau-v16.json`
- v16 render unit dir: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v16-exact-phrase-grid-chunks/`
- v16 full WAV: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-exact-phrase-grid-openutau-v16.wav`

Full duration: `303.320000s`.

## Branch evolution this pass

| branch | result |
|---|---|
| v12 render units | Reproduced the v11 composite idea in a generated render-unit manifest, but `世界` still degraded because the successful phrase probe included extra tail notes not present in v12. |
| v13 real tail context | Included the whole next transcript segment as render context, but local STT still heard `正義`; the context was not the same as the passing probe. |
| v14 custom tail | Added synthetic `それだけでいい` tail notes after segment 21-22. Raw length matched the passing probe, but tiny integer tick drift from source-time rounding still made STT choose `正義`. |
| v15 exact tail grid | Reproduced the phrase probe's integer tick grid for the `世界` unit and fixed `世界` in the full-song STT audit. |
| v16 exact phrase grids | Also reproduced the small-tsu-aware exact tick grid for `強がってスクロール...`, fixing the full-song `クロール` regression back to `スクロール`. |

## Key technical finding

Local STT was sensitive to 1-4 tick gaps/overlaps in OpenUTAU note grids.

The source-time rounding generator created tiny drift between adjacent notes. Phrase-isolated probes that used cumulative integer tick grids were more intelligible. v16 therefore uses exact phrase grids for:

- segment 11: `強がってスクロールして心だけ取り残す`, with literal `っ` as 80 ticks and the remaining morae as 169 ticks;
- segment 21-22 plus a synthetic tail: `それでも一行だけで世界に参加していたいもしも明日ただの友達で笑えたら` followed by hidden render-context `それだけでいい` tail notes.

## v16 local STT audit

Local STT service: `http://127.0.0.1:8010/transcribe-timed`

| region | v16 STT | assessment |
|---|---|---|
| 0.00s + 12s | `元気だよって一行だけのメッセージ 既読のアイガネおそこたいてういえかだご` | opening anchor pass |
| 55.26s + 14s | `タイムラインに走らない誰かの笑顔強がってスクロールして心だけ取り残す打ち明けの言葉が震えて消えてくタイム` | `スクロール` restored; `には知らない` is still a phonetic/semantic homophone ambiguity as `に走らない` |
| 103.64s + 12s | `それでも一行だけで世界に参加していたいもしも明日ただの友達で笑えたらそれだでいいや` | `世界` fixed in full generated branch |
| 173.44s + 8s | `それで十分だ` | pass |
| 243.36s + 12s | `返事なんてもう来なくてもするよ一部消せ` | closing anchor pass; trailing phrase degraded |

## Current best branch

Use v16 as the best reproducible generated reference branch.

Use v11 only as historical evidence of the audio-composite prototype. v16 internalizes the v11 phrase-probe behavior in generated USTX render units and exact assembly metadata.

## Remaining issues

- `タイムラインには知らない` vs `タイムラインに走らない` is a real homophone at the phonetic level (`ni wa shiranai` vs `ni hashiranai`) and may require lyric-aware scoring rather than pure STT text equality.
- The closing tail after `返事なんてもう来なくてもするよ` still degrades.
- v16 is still a render-unit branch, not a single monolithic USTX. That is intentional for intelligibility; a later musical branch can merge units once word accuracy is stable.

## Reproduction commands

Generate v16 USTX render units:

```bash
/home/err/devel/Music/fork-tales/references/mir-workbench/.venv/bin/python \
  /home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v16-exact-phrase-grids.py
```

Render each generated USTX with the existing renderer, then assemble:

```bash
python3 /home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/assemble-render-unit-openutau.py \
  --manifest /home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-exact-phrase-grid-openutau-v16.json \
  --output /home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-exact-phrase-grid-openutau-v16.wav \
  --force
```

## Next recommended iteration

Target the ending tail after `返事なんてもう来なくてもするよ`, using the same v16 method: isolate exact-duration phrase probes, find the passing integer grid/context, then promote it into a v17 render unit.
