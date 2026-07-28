---
uuid: "ft-003e-index-reconstruction-evidence-and-auditions"
title: "FT-003E: Index reconstruction evidence and auditions"
status: incoming
type: story
priority: P2
phase: 3
epic: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
owner: unassigned
points: 3
labels: reconstruction, evidence, audition, provenance
category: stories
research: "docs/reconstruction/README.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-001a-index-playable-media-metadata-and-waveform-jobs", "ft-003d-preserve-render-to-release-derivation-graph"]
---

# FT-003E: Index reconstruction evidence and auditions

## Outcome

The media workbench can open a reconstruction workspace that relates immutable source
renders, derived stems and plans, audition clips, metrics, grader output, and local
render availability without promoting any derived artifact into canonical source.

## Scope

- Index reconstruction manifests, ledgers, handoff packets, metrics, and audit output.
- Relate evidence and auditions to source works, renders, time ranges, and derivations.
- Represent manifest-listed but locally absent media as unavailable playable evidence.
- Open reconstruction evidence from the normal inspector and workspace surfaces.
- Preserve observed, derived, provisional, and accepted authority distinctions.

## Non-goals

- Running Demucs, OpenUTAU, Whisper, Gemma, or DSP jobs from the first indexer.
- Copying the untracked 4.6G render tree into Git.
- Treating a grader score or similarity result as human acceptance.

## Acceptance criteria

- The committed `references/` corpus can be indexed deterministically.
- `RENDERS-MANIFEST.edn` entries resolve to available or unavailable media explicitly.
- A selected audition displays its source range, generating program, evidence, and grade.
- Historical stale paths remain immutable evidence and are translated only through the
  recorded path-root rules.
- Rebuilding the read model produces the same reconstruction relationships.

## Verification

Fixture and corpus-slice tests cover manifest-only renders, translated historical paths,
multiple auditions from one source span, missing local media, and derivation round trips.
