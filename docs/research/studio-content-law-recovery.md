# Studio content-law recovery

Date: 2026-10-25. License: GPL-3.0-or-later.
Scope: implementation evidence for the content portion of FT-000B / issue #9.
Release and publication admission remain separately scoped; this does not claim
the entire broader FT-000B card or the music-studio workflow complete.

## Source and authority

The [recovery issue](https://github.com/octave-commons/calliope/issues/9) names
the immutable source at PR #7 head
194512b4ce7e9e706f555a4f933449c632bb8fb5:
`src/fork_tales/law/studio.cljc` and its tests. Original law SHA-256:
fb07d8bd70d3d3b3435d5128c3575ed1de4d2dc11c151a8451883a7a94366e0c.
ADR-001 and the approved media-workbench design govern the recovered meanings.
The original tree, namespace, board snapshots and release/publication portion
were not mechanically merged.

## Current interfaces

`calliope.law.studio/registry` is plain EDN-serializable data with qualified
registry keys and composable references. Regex constraints use strings rather
than opaque host Pattern objects. `law/validator` emits a compiled Malli
structural validator for a named type.

Structural validation is not complete admission. Use the pure
`calliope.studio.content` boundary:

- `valid?`: named shape plus intrinsic range, fade, timestamp, finite-value,
  marker-location/tier, ordering and data-only query/encoding constraints.
- `clip-check`: exact render identity/hash, positive ordered range bounded by
  the referenced duration, and fitting fades.
- `marker-check`: matching render/arrangement identity and bounded point/range.
  For an arrangement, supply its resolved duration explicitly; this library does
  not invent a playback engine or calculate arrangement timing.
- `rating-for?`: both subject type and identity must match; values are never
  promoted from a clip to its render or work.
- `arrangement-check`: resolve correctly identified accepted clips and immutable
  render metadata, reject duplicate positions, return entries in declared position
  order, and retain work IDs/source hashes. Cross-work composition requires the
  explicit flag. Inputs are never changed.
- `export-check`: bind arrangement identity/version, the complete distinct source
  hash set, encoding, renderer and output-hash metadata.

Context checks return `{:ok? true ...}` or explicit
`{:ok? false :errors [{:error/code ...}]}` data. They perform no filesystem,
audio, rendering, storage or publication effect. In particular, hash comparison
here validates metadata identity; the later source adapter must verify actual
media bytes.

Model-authored markers remain observed/derived/provisional. The raw marker
constructor cannot self-declare an accepted human annotation. The studio's
separate proposal/decision workflow must retain model authorship and record a
human judgment independently; changing attribution is not an acceptance mechanism.

## Evidence

The RED commit adds named laws and boundary tests and records the intended
missing `calliope.studio.content` implementation failure. A subsequent focused
RED check also caught two intrinsic admission gaps: nonfinite query scalars and
executable encoding values. Those now fail without executing caller values.
A further regression reproduced JVM long overflow for oversized fade pairs.
Admission now compares each fade against remaining duration rather than summing
them: invalid extremes return a structured refusal while an exactly fitting
maximum-integer source range remains valid.

Current local result: 123 tests / 809 assertions, zero failures/errors under
umask 0022. New tests cover object cross-promotion, exact source identity,
range/fade boundaries, marker and rating scope, accepted-clip ordering,
cross-work provenance, complete multi-source exports, closed query vocabulary,
plain-data schema round trips and no input mutation.

Full source/test/probe lint: zero errors/warnings. Hosted CI and current-head
code review are still required. No audio device, native studio UI, capture,
session persistence or provider call was executed by these content-law tests.
