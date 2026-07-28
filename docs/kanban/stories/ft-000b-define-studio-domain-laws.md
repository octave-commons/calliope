---
id: "9c66aedc-71c3-4d82-b222-d338daf3d99c"
title: "FT-000B: Define media workbench domain laws"
status: incoming
type: story
priority: P0
phase: 0
epic: "c75d00eb-7cc8-4938-a088-7f2acefb0f4f"
owner: unassigned
points: 5
labels: [law, malli, media-workbench]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["32325ea0-6c36-4af7-9711-df9cace6cb36"]
---

# FT-000B: Define media workbench domain laws

## Outcome

Versioned Malli `.cljc` contracts represent playable references, ratings, labels,
markers, clips, arrangements, playlists, smart lists, workspaces, exports,
releases, and publication targets without collapsing their identities.

## Scope

- Closed data contracts and registry entries.
- Scope and provenance fields.
- Rating dimensions and scales.
- Immutable render references and time ranges.
- Publication capability declarations.

## Non-goals

- Persistence adapters.
- Audio decoding or FFmpeg.
- UI components.

## Acceptance criteria

- Work, render, clip, arrangement, and export are distinct object types.
- Clip laws require immutable source identity and valid positive ranges.
- Ratings identify subject, dimension, scale, value, actor, and time.
- Playlist membership accepts declared playable refs; workspace law is distinct.
- Release and publication-attempt laws preserve per-target state.
- Invalid examples cover cross-scope promotion and malformed time ranges.

## Verification

Contract tests and negative fixtures pass under `clojure -M:test`.