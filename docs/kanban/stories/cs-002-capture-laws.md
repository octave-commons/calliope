---
labels: "studio, foundation, native, recovery"
dependency: ["cs-001-native-runtime-recovery"]
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792916611071-0.r238ujboi3mchrry7g9"
points: "5"
title: "CS-002: Define source-preserving session laws and deterministic capture adapters"
priority: "P0"
status: "incoming"
design: "docs/designs/media-workbench-v1.md"
uuid: "cs-002-capture-laws"
research: "docs/research/studio-foundation-map.md"
---

# CS-002: Define source-preserving session laws and deterministic capture adapters

## Context

The 2026-10-25 studio handoff requests the first complete synthetic music session.
See [current path map](../../research/studio-foundation-map.md) and its source links.

## Outcome

Define source-preserving session laws and deterministic capture adapters within Calliope's existing native media ownership.

## Scope

Produce portable named Malli laws, pure transitions and deterministic synthetic
adapters for the capture-specific [studio boundary](../../designs/studio-session-boundary.md).
Define/test the version-1 command and normalized-take shapes before implementing
adapters. Reuse Calliope media identity and upstream Clio event/durability ownership.
Normalize MIDI lifecycle, sustain, channel/note overlap, raw tick/sample clocks and
reconnect outcomes. Preserve original WAV/SMF bytes and metadata. No monitoring or
device capture is routed through inference; this is bounded synthetic capture,
not a pro-audio engine or general MIDI editor.

## Non-goals

A new DAW, browser client, changed simulation/canon, publication, hardware purchasing,
or a new persistent agent runtime. No declaration that the full FT milestone is done.

## Acceptance criteria

- Same source bytes/configuration produce identical normalized events, clocks and
  source hashes. Preserve raw WAV/SMF bytes and separate audio-render/MIDI identity.
- Unarmed and after-stop ingest each return :capture/not-armed with unchanged
  session, ledger, canonical source inventory, media bytes and hashes; rejected
  input is not opened/copied/published. Test both rejection paths independently.
- At 48000 Hz, PPQ 480 and tempo 500000, ticks 0/240/480 map to samples
  0/12000/24000 using declared floor rounding. Reject invalid configurations and
  backward/overflowing/out-of-range time; preserve equal-tick source order.
- C4 on/off at ticks 0/480 yields [0,24000). Velocity-zero onset means off;
  channels are independent; overlapping same-pitch onsets pair FIFO.
  Orphan off, duplicate packet sequence and backward tick fail explicitly.
- CC64>=64 sustains released notes until CC64<64 per channel, with physical-key
  and sounding spans distinct and all original CC bytes preserved.
- Disconnect at N ends the device epoch and reports active voices interrupted at
  N. Packets while disconnected fail. Same-device reconnect uses epoch+1 and an
  explicit nondecreasing session sample-origin despite reset raw ticks.
  Undeclared-device/missing-reconnect/after-stop packets fail without admitted take.
- Slow/failed/unavailable model tests cannot block local capture. No copied
  generic Clio admission law or adoption of the kit's provisional JSON schema.

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
Planning revision for PR #22: capture-specific proposed contract is docs/designs/studio-session-boundary.md; current accepted media-workbench design remains governing. The installed frontmatter surface refuses design-key updates, so the proposed companion contract is linked in the supported Markdown body. No status transition or authority promotion is claimed.

---