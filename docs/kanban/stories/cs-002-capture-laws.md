---
labels: "studio, foundation, native, recovery"
dependency: ["cs-001-native-runtime-recovery"]
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792933033101-0.z2pqs36tgqg88ekb4ip"
points: "5"
title: "CS-002: Define source-preserving session laws and deterministic capture adapters"
priority: "P0"
status: "review"
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

- Only the trusted native UI entrypoint with the configured user actor and an
  active session/process grant may ingest. The ING-AGENT, ING-SYSTEM and
  ING-SPOOF cases in the studio boundary must prove explicit refusal before
  filesystem access, with zero source payload opens/copies and zero events.
- V1 inputs are regular-file leaf names in the granted staging root. ING-PATH,
  ING-LINK and ING-ROOT must reject absolute/traversal/sibling-prefix paths,
  links/nonregular entries and changed roots. Open through the anchored root
  without following links; refuse hosts that cannot enforce confinement.
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

Planning review 5642914512: the v1 ingest contract is explicitly restricted to the trusted native UI entrypoint with the configured user actor and an active session/process grant. Added named ING acceptance cases for agent/system/spoofed entrypoints and absolute, traversal, linked, nonregular or changed-root inputs. V1 source arguments are regular-file leaf names under the root bound into the arm grant; source payloads outside it are never opened or copied.

The acceptance body was hand-authored as supported card content, then recorded with this canonical comment. Executable rejection tests belong to this story before adapters; no production ingest implementation or passing authorization tests are claimed in the planning PR.

Codex claims CS-002 for the authorized synthetic music milestone. Parent planning gate passed on c0d39fa with exact-head CodeRabbit completion6397531323 and MiMo approval5643155974; all19findings settled. CS-001 is now done through Rheos with actual JVM tests/lint, inherited human-accepted native choices and separately documented corpus/error probes. Implement named capture laws and failing clock/MIDI/authorization/confinement tests first, then pure interpretation and JVM source adapters. No real device, human creative decision, durable agent execution or complete studio workflow is claimed.

Implemented named capture/take laws, exact pure MIDI clocks/lifecycle, deterministic 4-second stereo PCM WAV plus six-note SMF, and trusted UI-user staged-source snapshots. RED checkpoints1db248f/1198dcb; current full suite138tests992assertions zero failures/errors; full lint zero errors/warnings. JVM suite runs actual Linux descriptor tests (4 Python cases), including a leaf replaced by an outside symlink after metadata open, FIFO/nonregular refusal before read-capable opens, root replacement and hash mismatch. Controlled pending model worker does not prevent source ingress; failed/unavailable stubs are not called. See docs/research/studio-capture-implementation.md for interfaces, 8MiB/source bound, Linux/procfs/Python requirements and limits. No canonical publication/Clio app session/native studio UI/restart/device/human listening or creative acceptance is claimed; those remain dependent stories. Moving to review, not done, pending peer code review.

---