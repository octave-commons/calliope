---
category: "stories"
labels: "native-ui, player, adapter"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-001c-build-persistent-player-shell-and-library-browser"]
process: "docs/process/product-design-and-delivery.md"
phase: "1"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785713921129-0.dmzyyv4i9xr8s2rxnqv"
points: "3"
title: "FT-001E: Integrate system media keys through the native shell adapter"
priority: "P1"
status: "icebox"
epic: "ft-001-ship-a-daily-driver-library-and-player"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-001e-integrate-system-media-keys-through-the-native-shell-adapter"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
---

# FT-001E: Integrate system media keys through the native shell adapter

## Outcome

Err controls play/pause, previous, and next from the operating system's media keys
while the application is unfocused, and the keys drive the same transport commands
as the in-app controls.

## Scope

- A native shell adapter that receives OS media-key events on the platform
  selected by FT-000D.
- Play/pause, previous, next, and stop mapped to existing application transport
  commands.
- Adapter absence or registration failure degrades to in-app control with an
  explicit, surfaced reason.
- Documented platform support boundary for the selected native stack.

## Non-goals

- OS notifications, file associations, and packaging. Those are sibling adapter
  concerns and are not required for the daily-driver gate.
- Global shortcuts for non-transport actions such as rating or triage.
- A second transport command path. The adapter dispatches existing commands only.
- Replacing FT-001C's in-app keyboard transport.

## Acceptance criteria

- Media keys drive transport while the application window is unfocused.
- Key events dispatch the same application commands as in-app transport, with no
  adapter-local playback or ledger writes.
- A failed or unavailable media-key registration is reported and does not break
  in-app transport or the queue.
- The supported platform boundary is stated, and unsupported platforms fail
  explicitly rather than silently doing nothing.

## Verification

Adapter tests cover event-to-command mapping, unfocused dispatch, and degraded
behavior on registration failure, with the command boundary asserted rather than
playback invoked directly. A local smoke run demonstrates play/pause, previous, and
next from the OS media keys against real corpus audio with the window unfocused.

## Why this card exists

The approved design lists "system media-key integration through the native adapter"
under behavior *required for daily-driver use*
(`docs/designs/media-workbench-v1.md`, "Persistent player behavior"), and the
accepted ADR assigns native media keys to an adapter around the domain rather than
to the shell (`docs/adrs/adr-001-local-first-media-workbench.md`). FT-001B lists
system media keys as a non-goal until this adapter exists, and FT-001C owns only
in-app keyboard transport, so Gate 1 as previously decomposed left a
design-required capability unowned. Raised in PR #3 review.