---
category: "stories"
labels: "arrangement, export, playback"
dependency: ["ft-003a-implement-marker-and-clip-commands-over-immutable-renders", "ft-003b-build-waveform-salvage-editor"]
process: "docs/process/product-design-and-delivery.md"
phase: "3"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714742673-0.czex1mscdl571jaojpt"
points: "5"
title: "FT-003C: Implement arrangement playback and deterministic export"
priority: "P0"
status: "blocked"
epic: "ft-003-recover-valuable-spans-and-arrange-them-non-destructively"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-003c-implement-arrangement-playback-and-deterministic-export"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
---

# FT-003C: Implement arrangement playback and deterministic export

## Outcome

Accepted clips can be ordered into a playable arrangement and rendered into a
content-addressed derivative with complete settings and source identity.

## Scope

- Arrangement edit commands and ordered timeline.
- Gaps, bounded overlaps/crossfades, clip gain, and fades.
- Playback resolution without first exporting.
- Background deterministic audio export.
- Export job progress, failure, retry, and output hash.

## Non-goals

- Arbitrary effects/plugins.
- Remote publication.
- Automatic mastering claims.

## Acceptance criteria

- Arrangement playback and export consume the same versioned edit decision list.
- Re-running the same export inputs/settings produces the same declared identity.
- Source hash drift fails before rendering.
- Failed exports do not create successful export records.
- Export settings and tool versions remain inspectable.

## Verification

Golden-fixture tests cover timeline resolution, fades/gaps, deterministic job
identity, source drift, failed rendering, and successful output hashing.

---
Dependency analysis (2026-08-02): depends on FT-003A (icebox) and FT-003B. Blocks nothing active (FT-003D is icebox). Moved breakdown -> blocked: waiting on FT-003A and FT-003B.
---