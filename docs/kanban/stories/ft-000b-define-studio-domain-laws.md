---
category: "stories"
labels: "law, malli, media-workbench"
dependency: ["ft-000a-review-and-accept-or-revise-media-workbench-authority"]
process: "docs/process/product-design-and-delivery.md"
phase: "0"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785719243852-0.dfnjs80tv7o3bnbnn14"
points: "5"
title: "FT-000B: Define media workbench domain laws"
priority: "P0"
status: "done"
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
Dependency analysis (2026-08-02): depends on FT-000A. Blocks FT-000C (events and projection need the laws first). Moved breakdown -> blocked: waiting on FT-000A.

Unblocked 2026-08-02: FT-000A is done (acceptance recorded). Entering ready; beginning implementation this session.

Review evidence (2026-08-02): src/fork_tales/law/studio.cljc — versioned closed-map Malli contracts covering work/render/clip/arrangement/export (distinct types), markers with epistemic status scoped to render|arrangement, multidimensional scoped ratings (enjoyment, publishability, weirdness, salvageability, technical-quality; scale zero-to-five; subject/dimension/scale/value/actor/at all required), dispositions, namespaced labels, playlists (any playable ref, works excluded), smart lists (closed variadic query matching the approved design shape, no frozen membership), workspaces (distinct attention context), releases and per-target publication state machines with declared capabilities and no credential fields. Verification run: clojure -M:test -> 30 tests, 193 assertions, 0 failures, 0 errors, including negative fixtures for cross-scope promotion (clip sourcing a work/clip, marker on clip/export/work, work in a playlist) and malformed time ranges (reversed, zero-length, negative). Not executed: no persistence adapters, no audio, no UI — all card non-goals.

Done (2026-08-02). Completion record: changed artifacts — src/fork_tales/law/studio.cljc (new), test/fork_tales/law/studio_test.clj (new), test/fork_tales/test_runner.clj (registered new ns). Evidence — clojure -M:test 30/193/0/0 with the card-named contract tests and negative fixtures. Not executed — persistence adapters, audio, UI (non-goals). Governing authority — ADR-001 (accepted 2026-07-28), media-workbench-v1 (approved 2026-07-28). Acceptance — the card-named verification is objective and met; no human-acceptance clause on this card. Follow-ups — FT-000C consumes these laws for event envelopes. Receipt — receipts.edn gate-0 entries 2026-08-02.
---