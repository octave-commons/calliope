# Heresy OpenUTAU Vocal Repair Plan v2

## Purpose

Repair the Heresy vocal reconstruction by replacing the sparse pYIN-only OpenUTAU pass with a lyric-aware, section-anchored Ritsu VCV performance. This is a repair plan for the next generated artifacts, not a replacement for the existing v1 files.

## Current Failure

- Existing USTX: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-ritsu-pyin-vocal-v1.ustx`
- Existing vocal render: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-ritsu-pyin-vocal-v1.wav`
- Existing note source: `/home/err/devel/Music/fork-tales/references/heresy-between/vocal/vocals.pyin.notes.json`
- Measured note coverage: `34.72s / 303.32s`, or `11.45%`.
- Long gaps over 5 seconds: `13`.
- Worst gaps: `261.44-282.54s` and `282.73-303.32s`, which erase the final chorus/outro region.

The failure is structural. The pYIN collapse generated short islands of notes and the USTX cycled placeholder kana over them. That proves OpenUTAU can render, but it does not produce a usable vocal performance.

## Source Of Truth

- Original full mix: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として.mp3`
- Original lyric/prompt: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.txt`
- Best current vocal reference: `/home/err/devel/Music/fork-tales/references/heresy-between/stems/htdemucs/存在論的な“反・虚無”の論証として/vocals.wav`
- Rough f0 fixture: `/home/err/devel/Music/fork-tales/references/heresy-between/vocal/vocals.pyin.f0.json`
- Rough note fixture: `/home/err/devel/Music/fork-tales/references/heresy-between/vocal/vocals.pyin.notes.json`
- Rough STT timing: `/home/err/devel/Music/fork-tales/references/heresy-between/lyrics/alignment.multilingual-medium.json`

Treat the STT file as timing scaffolding only. It visibly misrecognizes Japanese text and starts at the wrong semantic point. The lyric text file is the text source of truth.

## Target v2 Artifacts

- `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-ritsu-lyric-vocal-v2.ustx`
- `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-ritsu-lyric-vocal-v2.notes.json`
- `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-ritsu-lyric-vocal-v2.wav`
- `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau/heresy-between-openutau-vocal-repair-v2.json`
- Optional mix: `/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/heresy-between-vocal-repair-reconstruction-v11-master.wav`

Use `v2` for vocal-only artifacts. If the repaired vocal is mixed with the current best piano/body branch, use the next full reconstruction version after v10, so `v11`.

## Design Law

- Preserve the original lyric order exactly.
- Convert performance lyrics to kana-safe syllables for OpenUTAU.
- Represent breath and spoken cues explicitly; do not let them become accidental silence.
- Keep Ritsu in a stable first-pass range: C4-G4, with A4 only for final chorus emphasis if clean.
- Prefer phrase continuity over exact micro-f0. A complete imperfect vocal is better than a sparse accurate fragment.
- Leave intentional silence around breath/contrast moments, but cap unintended gaps at `4s`.
- Do not use romaji vowels, hyphens, or OpenUTAU rest notes that have previously hung the renderer.

## Section Map For v2

These are rough phrase windows for the first lyric-aware pass. Adjust by ear after rendering against the original vocal stem.

| Section | Window | Text Anchor | Performance Strategy |
| --- | ---: | --- | --- |
| Intro breath/spoken | 0-12s | `（息）…はぁ… / 「元気？」 / 三文字だけで、世界が揺れる` | Short whispered notes plus optional breath/noise events outside OpenUTAU. |
| Verse 1A | 12-24s | `「元気だよ」って / 一行だけのメッセージ / 既読の灯りが / 胸の底を叩いてる` | 2-4 short notes per phrase, held vowel tails. |
| Verse 1B | 24-40s | `言えなかった「ごめんね」より / もっと怖いのは / “何も送らない”っていう / 静かな選択` | Low-mid spoken singing, avoid melisma. |
| Pre-Chorus 1 | 40-58s | `理由はいつも / あとから並べられる / でも指先は先に / 生き方を押してしまう` | Slight rise, more connected phrase tails. |
| Chorus 1A | 58-82s | `一行だけのメッセージで / この夜の形が決まるなら / 僕は逃げ道の言い訳より / 小さな責任を選びたい` | Strongest continuous coverage so far, C4-G4. |
| Chorus 1B | 82-106s | `さよならの続き書けなくて / 空白ばかり増えていくけど / 一行だけのメッセージで / “意味”を置いて帰るよ` | Fix the current 84.89-98.73s gap with explicit lyrics. |
| Verse 2A | 106-128s | `タイムラインには / 知らない誰かの笑顔 / 強がってスクロールして / 心だけ取り残す` | Pull back, breathier, lower density than chorus. |
| Verse 2B | 128-150s | `打ちかけの言葉が / 震えて消えてくたび / 「どうせ…」の物語が / また王冠をかぶる` | Fix the current 140.34-156.32s gap. |
| Pre-Chorus 2 | 150-174s | `“きっと決まってる”って / 口にした瞬間 / 僕の手のひらから / 明日が落ちていく` | Continue through the current 156.44-176.80s gap. |
| Chorus 2A | 174-198s | `一行だけのメッセージで / 戻れる場所を探す僕 / 正しさは外に落ちてなくて / 選んだ跡に立ち上がる` | More confident, allow one A4 touch only if stable. |
| Chorus 2B | 198-222s | `言えなかったこと数えたら / 画面に入りきらないほど / それでも一行だけで / 世界に参加していたい` | Maintain coverage through 212.42-218.64s. |
| Bridge | 222-244s | `もしも明日 / ただの友だちで笑えたら / それだけでいい / そう言い聞かせて` | Sparse but deliberate; do not leave full silent block. |
| Bridge reveal | 244-262s | `でも本当は知ってる / 沈黙もまた / 誰かの夜を / 増やしてしまうって` | Low, intimate, small held vowels. |
| Breath/leap | 262-274s | `（息）…はぁ… / 打ち直す「元気？」の三文字 / 跳ぶのは信仰じゃなくて / “投げ出さない”という決意` | Repair the current 261.44-282.54s void. |
| Final Chorus | 274-296s | `一行だけのメッセージで / つながる夜があるのなら / 返事なんてもう来なくても / 名前を呼べた気がするよ / “どうせ”の続き書けなくて / それでも送信を押してみる / 一行だけのメッセージで / 僕は明日を肯定する` | Highest coverage, strongest note confidence, but avoid over-dense syllables. |
| Outro | 296-303.32s | `（息）… / 一行だけ / それで十分だ` | Very sparse intentional close; not accidental silence. |

## Kana Conversion Rules

- Convert kanji/kana text to kana performance syllables manually or via a local morphological/kana tool, then hand-fix high-value lines.
- Keep punctuation out of OpenUTAU lyrics.
- Split small kana where useful for timing, but avoid pathological one-character stutter across entire lines.
- Use Japanese VCV-friendly kana only.
- Breath markers should be either omitted from USTX and represented as separate breath/noise events, or rendered as short safe vowels like `は`/`あ` only if the voicebank handles them cleanly.

Priority lines to hand-fix first:

- `げんき`
- `げんきだよって`
- `いちぎょうだけのめっせーじ`
- `いみをおいてかえるよ`
- `ぼくはあしたをこうていする`
- `それでじゅうぶんだ`

## Pitch And Timing Strategy

1. Build phrase windows from the section map before using f0.
2. For each lyric phrase, allocate syllables evenly across the window with 80-220ms note durations and occasional held vowel tails.
3. Pull median pitch from pYIN frames inside the phrase window when voiced data exists.
4. If pYIN is missing or unstable, use a hand-authored contour around C4-G4 rather than dropping the phrase.
5. Merge micro-notes into singable syllable notes; do not preserve every f0 wobble as a separate note.
6. Add deliberate short rests between phrases, but keep unintentional gaps under 4 seconds.

## Acceptance Checks

- Vocal-only WAV renders to at least `303s` or has a documented intentional outro tail.
- Note coverage is at least `60%`; preferred is `70%`.
- No gap over `4s` except documented intentional breath/contrast gaps.
- Final chorus region `262-296s` contains explicit lyric notes.
- Outro `296-303.32s` contains `一行だけ / それで十分だ` or documented breath events.
- Render uses `OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer` and `WORLDLINE-R`.
- No old files are overwritten.

## Bass Follow-Up

Only repair bass after v2 vocals exist. The current bass feels exposed because vocal continuity is missing. The first bass pass should be a low-weight reinforcement against the original full mix, not a new lead element.
