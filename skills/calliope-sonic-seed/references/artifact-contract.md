# Sonic seed artifact contract

Read this reference when a seed leaves the local generation step.

## Input identity

The `:seed/key` must bind the source and interpretation that caused the sound.
Prefer stable identifiers and hashes over prose alone. A useful key contains:

```text
source:<repository-or-drive-id>@<content-hash>|interpretation:<hash>|decision:<id>|run:<id>
```

Changing lyrics, interpretation, source selection, generator version, or render
options creates a new identity. Do not reuse a key for materially different
inputs.

## Outputs

Each content-addressed directory contains:

| File | Meaning |
| --- | --- |
| `seed.edn` | Portable semantic music and render specification. |
| `seed.mid` | Symbolic realization of the repeated seed pattern. |
| `seed.wav` | Mono 44.1 kHz PCM reference satisfying the duration contract. |
| `receipt.edn` | Input identity, generator identity, duration, and artifact hashes. |

The directory name is the SHA-256 identity of the canonical `seed.edn` value.
Existing files may be reused only when their hashes match the new render exactly.

## Calliope integration

- GitHub holds laws, shapes, recipes, manifests, and reviewable provenance.
- Google Drive holds WAV, MIDI, provider renders, stems, and portable session
  bundles. Text artifacts may be copied there for cross-agent access.
- A lyrics-bearing Drive session may place `prompts.md` beside the immutable
  sonic-seed files. Bind the exact prompts hash into `:seed/key`; the companion
  document does not become part of the content-addressed seed or its receipt.
- One stable content hash connects the GitHub record to the Drive artifact.
- Calliope session orchestration may wrap the receipt in a Clio event and compose
  it through a Katamorph resource. The sonic-seed skill itself does not append to
  a shared ledger or mutate a remote repository without task authorization.
- Provider renders are new immutable artifacts referencing this seed; they never
  replace `seed.wav` or become ontology authority.

## Suno boundary

`seed.wav` is suitable as an audio input only when its verified duration is at
least 6.0 seconds. A successful local render does not prove a Suno upload or song
generation occurred. Preserve the Suno clip identifier, returned media hash, and
prompt/style identity separately when that adapter exists.
