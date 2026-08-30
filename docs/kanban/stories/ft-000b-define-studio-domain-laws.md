---
category: "stories"
labels: "law, malli, media-workbench"
dependency: ["ft-000a-review-and-accept-or-revise-media-workbench-authority"]
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1788047453951-0.czkmcnxdlxeniwicer7"
points: "5"
title: "FT-000B: Define media workbench domain laws"
priority: "P0"
status: "breakdown"
epic: "ft-000-establish-media-workbench-authority-and-durable-studio-foundation"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-000b-define-media-workbench-domain-laws"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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

---
Recovery crosswalk (2026-08-30): GitHub issue #9 (https://github.com/octave-commons/calliope/issues/9) preserves the stale PR #7 studio-law evidence and stricter contextual-validation proof for this existing canonical card. Do not merge the old tree mechanically. The issue remains open implementation backlog.

Readiness repair (2026-08-30): moved from ready back to breakdown through Rheos because declared dependency FT-000A is still incoming. Re-enter ready only after the dependency is satisfied or explicitly revised through canonical authority.

Date correction: the preceding reconciliation comments say 2026-08-30, but Rheos recorded these operations on 2026-08-29 UTC. The substance is unchanged; this append-only correction preserves the original ledger history.
---