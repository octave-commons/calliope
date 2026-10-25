---
labels: "studio, foundation, native, recovery"
dependency: ["cs-005-proposals-and-curation"]
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792916611649-0.05akjjgjrlvtnora17hq"
points: "5"
title: "CS-004: Wire the native studio controls and playback"
priority: "P0"
status: "incoming"
design: "docs/designs/media-workbench-v1.md"
uuid: "cs-004-native-studio-loop"
research: "docs/research/studio-foundation-map.md"
---

# CS-004: Wire the native studio controls and playback

## Context

The 2026-10-25 studio handoff requests the first complete synthetic music session.
See [current path map](../../research/studio-foundation-map.md) and its source links.

## Outcome

Wire native studio controls and playback within Calliope's existing ownership.
Desktop effects move to CS-006; the independent-process demonstration is CS-007.

## Scope

Wire native cljfx views only through application commands/queries and JavaFX Media.
Provide explicit synthetic source/arm/mark/stop/replay/select/annotate/proposal/
accept/reject controls according to the [studio boundary](../../designs/studio-session-boundary.md).
Keep writing/drawing/animation/simulation/Fork Tales references without story work.
This five-point story owns native interaction/playback; CS-006 owns the independent
i3 adapter/harness and CS-007 owns cross-process demonstration/evidence.

## Non-goals

A new DAW, browser client, changed simulation/canon, publication, hardware purchasing,
or a new persistent agent runtime. No declaration that the full FT milestone is done.

## Acceptance criteria

- At 640x480 and 960x720: startup is visibly UNARMED with ingest/mark disabled;
  explicit arm shows ARMED and enables ingest; synthetic ingest displays source
  hashes/duration; mark records a range; stop shows STOPPED and disables ingest.
- Replay advances actual JavaFX transport over the selected phrase. Selection
  and annotation retain clip UUID/range/source hashes and show committed results.
- Request shows pending then provisional or explicit failure/unavailability.
  Viewing leaves decision count unchanged; each separate accept/reject action
  displays its resulting attributed record only after command success.
- Tab reaches each enabled control; Space/Enter activates focused actions;
  text/range fields are keyboard editable; every action has a nonempty accessible
  name. At both sizes transport/capture state stays visible and scrollable
  overflow makes every action reachable without resizing.
- UI code neither writes ledgers nor invokes providers/FFmpeg directly.
  CS-007 verifies after restart the same phrase UUID, source/derivative hashes,
  event identities, selected range, annotations and attributed decision records.
- CS-006 mock tests verify the one-shot i3 command allowlist, rejected commands,
  unchanged focus, minimum sizes/opt-outs, stale-tree/manual-change refusal and
  fresh-plan reconnect. Real desktop/hardware/model/agent checks stay distinct.

## Verification

Write failing contract/behavior tests before production implementation, then run
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Preserve
commands, hashes and execution environment in Receipt River and evidence docs.
Planning review precedes ready; code review and current-head gates precede merge.

## Risks

Historical native artifacts are not current integration. Recover sources explicitly;
keep original bytes, independent sources, decisions and failed/unavailable outcomes
inspectable. Device, display, inference and i3 availability have separate claims.

---
Planning split for PR #22 review 5642202249: this 5-point story owns native controls/playback only. CS-006 owns desktop-profile effects and mock harness; CS-007 owns the complete independent-process demonstration. Dependency is CS-005, changed through Rheos. Native action/state, keyboard, accessible-name and overflow checks are explicit in the body.

---