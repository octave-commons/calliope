---
uuid: "cs-006-desktop-profile"
title: "CS-006: Add an explicit one-shot music desktop profile"
status: incoming
priority: P0
points: 3
labels: studio, foundation, native, recovery
dependency: ["cs-001-native-runtime-recovery"]
research: docs/research/studio-foundation-map.md
adr: docs/adrs/adr-001-local-first-media-workbench.md
design: docs/designs/studio-session-boundary.md
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

