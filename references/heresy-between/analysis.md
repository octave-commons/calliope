# Reference Analysis: 存在論的な“反・虚無”の論証として

## Files

- Audio: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として.mp3`
- Lyrics/prompt: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.txt`
- Metadata: `/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.json`
- Spectrogram: `spectrogram.png`
- Waveform: `waveform.png`

## Audio Facts

- Duration: `303.36s`
- Format: MP3, stereo, `48kHz`
- Mean level: about `-13.3 dB`
- Peak: `0.0 dB`
- Source metadata says `made with suno`, created `2026-02-25T03:57:12Z`, id `6ec687e6-b51d-4a4b-9472-27bd8be77ffa`.

## What Worked

- The prompt made drums the protagonist. This is the biggest difference from the local MusicGen attempts: the bed does not merely establish mood; it carries the body of the song.
- The lyric is small and concrete. The metaphysics comes from one action: sending a one-line message. The song avoids abstract thesis-singing by binding philosophy to finger, screen, breath, read receipt, silence.
- The vocal demand is intentionally minimal: breath, whisper, spoken Japanese, ghost choir. This is feasible for OpenUTAU if we do not try to force every line into a conventional sung melody.
- Breakcore is used as emotional noise, not genre cosplay. It should appear as fills, fractures, and post-chorus/stress transients.
- Sax/upright bass/orchestra are cameo colors. They should not become the base arrangement.

## Local Reconstruction Strategy

1. Build a MIDI rhythm skeleton first: live-drum intro, half-time kick/snare gravity, breakcore fills at section seams.
2. Build a sparse bass MIDI: stable sub roots, occasional upright-bass color notes.
3. Build a vocal USTX as a hybrid of spoken-short-note and held-oath phrases. Do not attempt dense melisma.
4. Use Whisper timing only after stem separation. The NPU STT service now exposes `/transcribe-timed` for segment/word timing JSON.
5. Use Basic Pitch only on separated pitched stems or clean local sketches. Do not run it on the full mix and treat the result as score truth.
6. Use torchcrepe/CREPE on vocals for f0; then manually simplify into OpenUTAU note events.

## USTX Performance Notes

- Human lyric text can keep kanji and punctuation.
- OpenUTAU performance lyric must be kana-safe.
- Breath and notification cues should become separate audio/SFX events, not sung lyrics.
- Suggested lead range: C4-G4 for Ritsu VCV first pass; final chorus can touch A4 only if the voicebank renders cleanly.
- Suggested phrase law: 2-4 short notes, then a held vowel or silence. The silence is part of the hook.

## Arrangement Law

The song is not about sadness or optimism. It is about refusing the cheap determinism of silence. Every section should answer: what does a tiny action do to the body?

- Intro: action has not happened yet; breath and drums prepare the hand.
- Verse: the screen becomes a body surface.
- Pre-chorus: the finger acts before reasons can defend it.
- Chorus: responsibility becomes singable.
- Bridge: silence is revealed as an action too.
- Final chorus: sending becomes a secular leap.
- Outro: one line remains, enough.

## Next Artifacts To Produce

- `sections.json`: approximate section timing from waveform/spectrogram plus lyrics.
- `drums.mid`: half-time live drum scaffold with breakcore fills.
- `bass.mid`: stable sub/upright hybrid scaffold.
- `vocal-notes.mid`: simplified vocal melody from f0 or hand-authored melody.
- `lyrics/alignment.json`: `/transcribe-timed` output on Demucs vocal stem.
- `openutau/heresy-proof.ustx`: kana vocal performance plan.
