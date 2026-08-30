---
category: "stories"
labels: "recovery, skill, sonic-seed, testing"
type: "task"
write-id: "1788049616176-0.sm2vjugv2vasr8ymruu"
points: "5"
title: "Land the portable sonic-seed skill with hosted tests"
priority: "P1"
status: "incoming"
uuid: "calliope-issue-13-portable-sonic-seed-skill"
created_at: "2026-08-29T23:49:48.767Z"
---

# Land the portable sonic-seed skill with hosted tests

GitHub issue: https://github.com/octave-commons/calliope/issues/13

## Outcome

Calliope contains the portable, reviewable sonic-seed skill and deterministic
domain implementation, with boundary schemas and hosted evidence. Runtime and
binary/media distribution remain a separate follow-up.

## Source evidence to preserve

Extract the useful source from draft PR #8 head
`8fde6f899f31bc222285bc0ca4c03f7c77200ca2` onto current main: concise skill
instructions and UI metadata, CLJC law/shape/generator, content-addressed
EDN/MIDI/WAV/receipt rendering, immutable writes, and the six-second whole-bar
minimum. Treat the old head as evidence, not merge proof.

## Scope

- One pure request-to-semantic-seed function with a stable canonical identity.
- Deterministic MIDI/WAV realization behind explicit adapters.
- Closed request, seed, and receipt contracts enforced at boundaries.
- Immutable content-addressed output and portable source-tree instructions.
- One reviewed shared adapter core or a mechanically enforced runtime parity suite.

## Acceptance criteria

- Validation rejects blank/oversized keys, non-finite numbers, duration below the
  minimum, and a documented resource-safe maximum before allocation.
- Same canonical request and generator version yields the same seed identity and
  byte-identical EDN, MIDI, WAV, and receipt artifacts.
- Semantic, rendering, or generator-version changes change the identity.
- Existing artifacts are reused only after exact hash verification; conflicts fail.
- MIDI and WAV structure, duration, format, and receipt hashes agree.
- Source-checkout runtime requirements are explicit and truthful.
- Table, unit, property, determinism, immutable-conflict, CLI-error, and
  cross-runtime golden tests run in hosted CI with zero-warning lint.
- Skill validation and a clean extracted-source smoke pass.
- A Receipt River entry pins versions, commands, assertions, hashes, and limits.

## Follow-up dependency

Runtime packaging and publication are tracked by
https://github.com/octave-commons/calliope/issues/14 and must not be folded into
this card.

## Non-goals

Bundled executables, release ZIPs, generated session media, providers, finished
songs, lyrics, product review, layout, and publication are out of scope.

---
Review clarification: deterministic reference MIDI/WAV generation and byte-identical golden artifacts remain required verification outputs for this skill card. The generated-session-media non-goal excludes distributing or retaining session outputs as bundled/product deliverables; it does not exclude the bounded reference artifacts required by the acceptance criteria.
---