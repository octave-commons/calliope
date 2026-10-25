---
uuid: "cs-004-native-studio-loop"
title: "CS-004: Demonstrate the native studio loop and explicit desktop profile"
status: incoming
priority: P0
points: 5
labels: studio, foundation, native, recovery
dependency: ["cs-003-session-recovery"]
research: docs/research/studio-foundation-map.md
adr: docs/adrs/adr-001-local-first-media-workbench.md
design: docs/designs/media-workbench-v1.md
---

# CS-004: Demonstrate the native studio loop and explicit desktop profile

## Context

The 2026-10-25 studio handoff requests the first complete synthetic music session.
See [current path map](../../research/studio-foundation-map.md) and its source links.

## Outcome

Demonstrate the native studio loop and explicit desktop profile within Calliope's existing native media ownership.

## Scope

Wire native cljfx views only through application commands/queries. Provide explicit synthetic source/arm/mark/stop/replay/select/annotate/proposal/accept/reject controls. Add one-shot inspectable i3 profile adapter with mock tests, fixed safe commands and no focus stealing. Keep writing/drawing/animation/simulation/Fork Tales references available without implementing story work. Document portable workflow and run independent process recovery demonstration.

## Non-goals

A new DAW, browser client, changed simulation/canon, publication, hardware purchasing,
or a new persistent agent runtime. No declaration that the full FT milestone is done.

## Acceptance criteria

Native view exercised at 640x480 and 960x720 with accessible controls and overflow. Complete scripted interaction goes through real application boundary and actual JavaFX playback under declared display/sink. Restart retrieves same phrase and decisions. Mock i3 covers minimum sizes, opt-outs, stale trees, manual changes and reconnect. Real desktop/hardware/model/durable agent checks remain distinct and unclaimed.

## Verification

Write failing contract/behavior tests before production implementation, then run
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Preserve
commands, hashes and execution environment in Receipt River and evidence docs.
Planning review precedes ready; code review and current-head gates precede merge.

## Risks

Historical native artifacts are not current integration. Recover sources explicitly;
keep original bytes, independent sources, decisions and failed/unavailable outcomes
inspectable. Device, display, inference and i3 availability have separate claims.

