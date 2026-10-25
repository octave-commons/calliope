---
labels: "studio, foundation, native, recovery"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology"]
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792924583848-0.p8mnqjr3ozdr4n1rx6e"
points: "3"
title: "CS-001: Reconcile native runtime and reproduce window/audio/rebuild evidence"
priority: "P0"
status: "incoming"
design: "docs/designs/media-workbench-v1.md"
uuid: "cs-001-native-runtime-recovery"
research: "docs/research/studio-foundation-map.md"
---

# CS-001: Reconcile native runtime and reproduce window/audio/rebuild evidence

## Context

The 2026-10-25 studio handoff requests the first complete synthetic music session.
See [current path map](../../research/studio-foundation-map.md) and its source links.

## Outcome

Reconcile native runtime and reproduce window/audio/rebuild evidence within Calliope's existing native media ownership.

## Scope

Recover the accepted runtime artifact from PR #7 without stale board history. Preserve historic acceptance, existing ADR-002 dataset authority and explicit current limits. Re-run cljfx window, JavaFX Media open/play/seek/pause/resume/stop, and SQLite rebuild with declared asset hash. Reconcile FT-000D/issue #12 evidence via Rheos. This story produces the runtime reconciliation; no new toolkit decision or full player.

## Non-goals

A new DAW, browser client, changed simulation/canon, publication, hardware purchasing,
or a new persistent agent runtime. No declaration that the full FT milestone is done.

## Acceptance criteria

Historic ADR source is byte-faithful and hash-pinned; new terminology is separate.
Native tests execute and assert open/READY with positive duration, play with
advancing position, seek to the requested tolerance, pause with held position,
resume with advancing position and stop/STOPPED. Corpus MP3 and fixture evidence
are separately labeled; record the exact asset, byte count, SHA-256 algorithm and
digest. Window uses an in-process command/query boundary. SQLite rebuild compares
exact selected columns and row values, ordered by the query's declared ORDER BY
(the discovery fixture uses id,note ordered by id).

## Verification

Write failing contract/behavior tests before production implementation, then run
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Preserve
commands, hashes and execution environment in Receipt River and evidence docs.
The disposable probe must fail nonzero if any required transport or rebuild
assertion fails; printing a status is insufficient.
Planning review precedes ready; code review and current-head gates precede merge.

## Risks

Historical native artifacts are not current integration. Recover sources explicitly;
keep original bytes, independent sources, decisions and failed/unavailable outcomes
inspectable. Device, display, inference and i3 availability have separate claims.

---
Planning review 4370152387: added the explicit FT-000D dependency through Rheos. CS-001 cannot enter ready before that owner decision is reconciled/accepted through its existing FT-000A dependency. The current native probe and recovered artifact are orientation/research evidence for the owning FT-000D/issue #12 recovery, not downstream production assumptions or a waiver. No existing FT status or acceptance has been changed.

Planning review5642720031 diagnostic fixes: current ADR now links its affected approved design plus historical alternatives/acceptance conditions; recovered ADR bytes unchanged. READY polling reports Media/MediaPlayer errors with original cause. Actual corpus transport/rebuild still passes. ready-probe separately exercises fake asynchronous getters through real FX dispatch; it is not a real codec-error claim. Root112tests699assertions and full lint pass. Story remains incoming pending planning convergence.

---