---
name: calliope-sonic-seed
description: "Create a deterministic MIDI and WAV seed from a Calliope song's provenance after corpus-native lyrics or production intent exist; use it for playable audio references, Suno audio inputs, and forkable generation receipts, not for finished-song rendering."
license: GPL-3.0-or-later
metadata:
  project: "calliope"
  version: "1"
---

# Calliope Sonic Seed

Create a small playable musical structure whose identity is bound to the source
material and interpretation that caused it. This skill supplies rhythm, pitch,
density, and timbral pressure. It does not decide what a song means and does not
replace corpus mining or songwriting.

## Use

Use this skill after a source cluster and song interpretation exist, normally
after `corpus-native-music` has produced lyrics and a compact style prompt. Also
use it when the user asks for a playable seed, MIDI, or audio reference for a
Calliope generation.

Do not use it to claim that a provider rendered a finished song. A local seed WAV
is a reference artifact, not a Suno render.

## Workflow

1. Construct one stable provenance key containing the selected source identity,
   interpretation or lyrics hash, decision identity, and run or fork identity.
   A revision changes the key; never overwrite a published generation.
2. Run the bundled NBB entrypoint from the skill directory:

   ```bash
   npx --yes nbb@1.3.204 -cp scripts scripts/calliope/sonic_seed/infra.nbb \
     --seed-key '<stable-provenance-key>' \
     --out '<output-directory>' \
     --min-seconds 6
   ```

3. Preserve the complete content-addressed output directory. It contains
   `seed.wav`, `seed.mid`, `seed.edn`, and `receipt.edn`.
4. Verify the reported duration is at least 6.0 seconds before offering the WAV
   as a Suno audio input. The generator satisfies the bound with whole musical
   bars, never arbitrary silence.
5. Return the audio file with the lyrics/style response and name the source or
   motif cluster that determined the provenance key.

If NBB cannot execute, report the missing runtime and do not fabricate audio or
a receipt. Do not silently substitute a different generator because that changes
the artifact identity.

## Laws

- Meaning comes from the corpus; deterministic entropy only shapes sound.
- The same provenance key, generator version, and render options reproduce the
  same semantic seed and byte-identical MIDI/WAV artifacts.
- The default and minimum Suno input duration is 6.0 seconds.
- Content-addressed outputs are immutable. Variation creates a new seed key.
- Provider responses and generated audio are untrusted until their duration and
  hashes are verified.
- Keep Clojure data authoritative. Native Node values remain inside the NBB
  infrastructure boundary.

Read [references/artifact-contract.md](references/artifact-contract.md) when
archiving results, composing a Calliope session, or handing the seed to an audio
generation provider.
