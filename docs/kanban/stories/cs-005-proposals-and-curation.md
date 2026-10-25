---
labels: "studio, foundation, native, recovery"
dependency: ["cs-003-session-recovery"]
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792917560103-0.hrrz02zx3ia72k1qkjy"
points: "3"
title: "CS-005: Curate phrases and record separate human proposal decisions"
priority: "P0"
status: "incoming"
design: "docs/designs/studio-session-boundary.md"
uuid: "cs-005-proposals-and-curation"
research: "docs/research/studio-foundation-map.md"
---

# CS-005: Curate phrases and record separate human proposal decisions

## Context

The [studio boundary](../../designs/studio-session-boundary.md) specifies the
assignment's complete synthetic session and the accepted domain ownership.

## Outcome

Curate phrases and record separate human proposal decisions as one bounded part of the complete studio milestone.

## Scope

Implement source-linked phrase marking/clip selection, annotations and retrieval through the application boundary. Add bounded provisional proposal requests/results and separate trusted-user accept/reject commands using the studio-session contract. Providers stay outside capture/monitoring and the FX thread.

## Non-goals

A DAW replacement, new shared event laws, changed story/canon or simulation,
hardware procurement, publication, or a new persistent agent runtime.

## Acceptance criteria

- Mark/refine/select preserves original WAV/MIDI hashes and produces a non-destructive clip UUID/range. Annotation and retrieval use that identity.
- Accept and reject have independent tests. Each durable record retains proposal UUID, human actor ID, decision value, clip/source identity and causal source event.
- Read, view, provider completion, model replay and agent-context commands produce zero human decision events. Unknown proposal and stale sources return failure.
- Two JVMs recover the same phrase/clip UUID, source hashes, selection, annotation, proposal identity and attributed decision records.
- Stalled, failed and unavailable model tests leave capture commands responsive and independent. Actual inference and fixture proposals are separately labeled; unfinished local requests recover interrupted/unknown without a durable-agent claim.

## Verification

Write failing contract/behavior tests before production implementation, then
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Record
commands, exact identities, real versus simulated effects and limits in receipts.
Planning review precedes ready; current-head review/checks precede merge.

## Risks

Keep missing sources, failed writes, unavailable providers and partial effects
explicit. Device and durable-runtime evidence cannot be inferred from fixtures.

---
Provider discovery 2026-10-25: the existing Calliope model HTTP adapter reached configured local Ollama gemma4:e4b, but the returned content did not parse as JSON (EOF). A separate bounded raw API diagnostic with think=false timed out after 45s. No valid proposal, listening observation, human decision or durable run is claimed. Keep failed/empty/timeout outcomes explicit and outside capture; future provider integration must verify an actual bounded output rather than rely on catalog advertisement.

---