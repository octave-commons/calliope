---
labels: "studio, foundation, native, recovery"
dependency: ["cs-001-native-runtime-recovery"]
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792937377677-0.xq4xuw6x1qpq2vdgiea"
points: "3"
title: "CS-006: Add an explicit one-shot music desktop profile"
priority: "P0"
status: "in_progress"
design: "docs/designs/studio-session-boundary.md"
uuid: "cs-006-desktop-profile"
research: "docs/research/studio-foundation-map.md"
---

# CS-006: Add an explicit one-shot music desktop profile

## Context

The [studio boundary](../../designs/studio-session-boundary.md) specifies the
assignment's complete synthetic session and the accepted domain ownership.

## Outcome

Add an explicit one-shot music desktop profile as one bounded part of the complete studio milestone.

## Scope

Implement an inspectable i3 plan/apply adapter for explicitly selected containers, fixed operations and declared minimum sizes. Preserve other creative applications, user focus, opt-outs and deliberate geometry changes. No i3 configuration replacement or background enforcement.

## Non-goals

A DAW replacement, new shared event laws, changed story/canon or simulation,
hardware procurement, publication, or a new persistent agent runtime.

## Acceptance criteria

- Mock records prove only allowlisted operations with validated integer IDs/sizes execute. Caller-supplied command strings and unknown operations execute zero commands.
- Focus container ID is unchanged before/after apply. Only explicitly selected/owned containers are targeted; missing, undersized or opted-out windows are skipped with reasons.
- A tree revision mismatch or manual geometry change between preview/apply cancels with zero commands.
- Disconnect returns unavailable/partial outcome truthfully; reconnect requires a fresh tree and plan, never silent reapply.
- Real workstation read-only discovery, mock effects and actual user-invoked layout effects are reported separately.

## Verification

Write failing contract/behavior tests before production implementation, then
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Record
commands, exact identities, real versus simulated effects and limits in receipts.
Planning review precedes ready; current-head review/checks precede merge.

## Risks

Keep missing sources, failed writes, unavailable providers and partial effects
explicit. Device and durable-runtime evidence cannot be inferred from fixtures.

---
Codex claims independent CS-006 after planning convergence at c0d39fa (CodeRabbit completion6397531323, MiMo approval5643155974, canonical planning PASS) and CS-001 done. Implement a pure inspectable plan plus one-shot i3 adapter with fixed operations, exact tree/geometry preconditions, focus preservation and explicit partial/unavailable outcomes. No live layout effects, config replacement or background reconciliation during development; mocks, read-only discovery and any later user-invoked effects remain separate.

GREEN evidence: full JVM suite150tests1068assertions, zero failures/errors under umask0022, full src/test/spike lint zero errors/warnings. Actual production controller passed an isolated Xvfb/i3 run on two disposable xmessage clients: six acknowledged fixed operations, exact960x1080 rectangles, unchanged focus, duplicate apply and manual-change stale apply with zero further commands. Stronger per-operation postconditions also pass the repeated isolated run. Actual workstation adapter discovery separately reads seven windows with zero effects. Evidence and platform/IPC atomicity limits are in docs/research/studio-desktop-implementation.md and spike/studio-desktop/README.md. No current workstation layout or actual Calliope/DAW UI was changed or claimed verified. Moving to review, not done.

---