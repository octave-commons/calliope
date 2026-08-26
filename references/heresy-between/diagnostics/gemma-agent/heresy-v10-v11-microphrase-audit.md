# Heresy Between OpenUTAU v10/v11 microphrase audit

Date: 2026-05-14

## Purpose

Continue from v9 by targeting the two remaining high-value intelligibility failures:

1. chunk 02 degradation around `強がってスクロールして心だけ取り残す`;
2. chunk 03 `世界` being heard as `正義` in the full v9 canvas.

## New artifacts

### v10 microphrase USTX branch

- Generator: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/generate-mora-preroll-openutau-v10-microphrase.py`
- Manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-microphrase-openutau-v10.json`
- Chunk dir: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v10-microphrase-chunks/`
- Full render: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-microphrase-openutau-v10.wav`

v10 changes:

- Preserves `っ` as a short literal small-tsu note instead of dropping it.
- Uses source transcript text for segment 11 so `スクロール` keeps its long vowel.
- Splits the earlier chunk 02/03 failure zones into smaller segment-safe chunks.
- Keeps v9's audited pitch policy: stable tone 60 for segments 10-36, source pitch elsewhere.

### v11 best-audit composite

- Manifest: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-best-audit-composite-openutau-v11.json`
- Part dir: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/v11-best-audit-composite/`
- Full composite: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-best-audit-composite-openutau-v11.wav`
- Duration: `303.320000s`

v11 is an audio-level composite from audited OpenUTAU chunks and phrase probes. It is not a single USTX branch; it is a best-heard reference canvas that preserves exact full-song duration.

## Phrase probes

Controlled phrase probes showed that Ritsu can sing several previously degraded phrases clearly when rendered as short phrase-level USTX:

| probe | local STT result | finding |
|---|---|---|
| `tsuyogatte-scroll-literal-smalltsu-fit454.wav` | `強がってスクロールして心だけ取り残す` | Literal short `っ` plus preserved `ー` fixes the segment 11 phrase at the original segment duration. |
| `soredemo-sekai-with-next-v9-durations-aligned10_22.wav` | `それでも一行だけで世界に参加していたいもしも明日ただの友達で笑えたら` | Phrase-level render fixes `世界` vs `正義` when aligned back to the original 10.22s span. |
| `timeline-fit-baseline.wav` | `タイムラインに走らない誰かの笑顔` | Local STT consistently confuses `には知らない` with `に走らない` for this synthetic phrase when isolated. |

## v10 full audit

Local STT service: `http://127.0.0.1:8010/transcribe-timed`

| region | v10 STT | assessment |
|---|---|---|
| 0.00s + 12s | `元気だよって一行だけのメッセージ ...` | opening anchor pass |
| 55.26s + 14s | `タイムラインに走らない誰かの笑顔強がってクロールして心だけ取り残す...` | segment 11 improves but `スクロール` loses initial `ス`; timeline remains ambiguous |
| 103.64s + 12s | `それでも一行だけで正義に参加していたい...` | still fails `世界` in full chunk render |
| 173.44s + 8s | `それで十分だ` | pass |
| 243.36s + 12s | `返事なんてもう来なくてもするよ...` | closing anchor pass |

## v11 full audit

| region | v11 STT | assessment |
|---|---|---|
| 0.00s + 12s | `元気だよって一行だけのメッセージ 既読のアイガネおそこたいてういえかだご` | opening anchor pass |
| 55.26s + 14s | `タイムラインに走らない誰かの笑顔強がってスクロールして心だけ取り残す打ち明けの言葉が震えて消えてくタイム` | segment 11/12 substantially improved; timeline `知らない` still ambiguous as `走らない` |
| 103.64s + 12s | `それでも一行だけで世界に参加していたいもしも明日ただの友達で笑えたらそれだでいいや` | fixes the `世界`/`正義` failure |
| 173.44s + 8s | `それで十分だ` | pass |
| 243.36s + 12s | `返事なんてもう来なくてもするよ一部消せ` | closing anchor pass; trailing phrase degraded |

## Decisions

- Keep v10 as the canonical USTX microphrase branch because it records reusable rendering rules: small-tsu preservation, long-vowel preservation, and segment-safe microchunking.
- Keep v11 as the best current audio reference canvas because it combines the strongest audited pieces and fixes the `世界` anchor.
- Do not discard v9: it remains the best single-branch baseline for some local-STT ambiguities and a useful comparison point.

## Remaining issues

- `タイムラインには知らない` is still heard by local STT as `タイムラインに走らない` in many synthetic-only contexts. Original vocal STT hears the intended phrase, so this remains an OpenUTAU articulation/ASR ambiguity.
- v11 is audio-composite, not a single USTX project. The next durable step is to encode the v11 successful phrase-probe settings back into a single generated USTX/chunk strategy.
- The closing tail after `返事なんてもう来なくてもするよ` still degrades.

## Next recommended iteration

Build v12 as a single-USTX/chunk generator that internalizes the v11 successful phrase-probe behavior by adding explicit per-segment render-tail policy and phrase-isolated render chunks for segments 11 and 21-22, while preserving the v11 full-song timeline assembly.
