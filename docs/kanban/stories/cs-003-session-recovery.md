---
uuid: "cs-003-session-recovery"
title: "CS-003: Implement durable phrase curation and separate human decisions"
status: incoming
priority: P0
points: 5
labels: studio, foundation, native, recovery
dependency: ["cs-002-capture-laws"]
research: docs/research/studio-foundation-map.md
adr: docs/adrs/adr-001-local-first-media-workbench.md
design: docs/designs/media-workbench-v1.md
---

# CS-003: Implement durable phrase curation and separate human decisions

## Context

The 2026-10-25 studio handoff requests the first complete synthetic music session.
See [current path map](../../research/studio-foundation-map.md) and its source links.

## Outcome

Implement durable phrase curation and separate human decisions within Calliope's existing native media ownership.

## Scope

Add in-process command/query services, immutable source storage and recoverable append-only session state. Mark recent phrase, refine non-destructive clip boundaries, select, annotate, retrieve and review a provisional suggestion. Accept/reject is a separate attributed human command. SQLite remains disposable; durable records are the source of recovery.

## Non-goals

A new DAW, browser client, changed simulation/canon, publication, hardware purchasing,
or a new persistent agent runtime. No declaration that the full FT milestone is done.

## Acceptance criteria

Two independent JVM processes recover identical phrase/source hashes, selection, annotation and decisions. Read/view cannot accept proposals. Model self-promotion and unknown proposal fail. Missing/stale sources, partial/corrupt ledger and failed writes report explicit non-success. Duplicate requests are idempotent; concurrent writers are guarded. Restart never opens devices or rearms capture.

## Verification

Write failing contract/behavior tests before production implementation, then run
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Preserve
commands, hashes and execution environment in Receipt River and evidence docs.
Planning review precedes ready; code review and current-head gates precede merge.

## Risks

Historical native artifacts are not current integration. Recover sources explicitly;
keep original bytes, independent sources, decisions and failed/unavailable outcomes
inspectable. Device, display, inference and i3 availability have separate claims.

