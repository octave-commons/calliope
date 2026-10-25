---
uuid: "cs-002-capture-laws"
title: "CS-002: Define source-preserving session laws and deterministic capture adapters"
status: incoming
priority: P0
points: 5
labels: studio, foundation, native, recovery
dependency: ["cs-001-native-runtime-recovery"]
research: docs/research/studio-foundation-map.md
adr: docs/adrs/adr-001-local-first-media-workbench.md
design: docs/designs/media-workbench-v1.md
---

# CS-002: Define source-preserving session laws and deterministic capture adapters

## Context

The 2026-10-25 studio handoff requests the first complete synthetic music session.
See [current path map](../../research/studio-foundation-map.md) and its source links.

## Outcome

Define source-preserving session laws and deterministic capture adapters within Calliope's existing native media ownership.

## Scope

Implement portable named Malli laws and pure transitions for explicit arm, bounded audio/MIDI capture, mark and stop. Reuse current Calliope and upstream Clio ownership. Normalize MIDI lifecycle, sustain, channel/note overlap, raw tick/sample clocks and reconnect outcomes. Original WAV/MIDI bytes have content identity. No real-time monitoring or device capture is routed through inference.

## Non-goals

A new DAW, browser client, changed simulation/canon, publication, hardware purchasing,
or a new persistent agent runtime. No declaration that the full FT milestone is done.

## Acceptance criteria

Same input and declared clock config produce identical interpretation and source hashes. Unarmed/after-stop capture fails. Orphan notes, invalid clocks/ranges, duplicates, ordering violations and reconnect are tested. Slow/failed/unavailable model tests cannot block local capture. No copied generic event-admission law or provisional JSON schema adoption.

## Verification

Write failing contract/behavior tests before production implementation, then run
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Preserve
commands, hashes and execution environment in Receipt River and evidence docs.
Planning review precedes ready; code review and current-head gates precede merge.

## Risks

Historical native artifacts are not current integration. Recover sources explicitly;
keep original bytes, independent sources, decisions and failed/unavailable outcomes
inspectable. Device, display, inference and i3 availability have separate claims.

