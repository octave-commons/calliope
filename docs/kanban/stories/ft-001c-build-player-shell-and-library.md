---
uuid: "ft-001c-build-persistent-player-shell-and-library-browser"
title: "FT-001C: Build persistent player shell and library browser"
status: incoming
type: story
priority: P0
phase: 1
epic: "ft-001-ship-a-daily-driver-library-and-player"
owner: unassigned
points: 5
labels: ui, player, library
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-001b-implement-playback-resolver-persistent-queue-and-resume"]
---

# FT-001C: Build persistent player shell and library browser

## Outcome

A desktop-first application shell keeps playback mounted while Err browses and
sorts the corpus in a responsive library table.

## Scope

- Left navigation, main surface, inspector, and bottom player.
- Virtualized library rows.
- Column selection, sort, filter, and stable selection.
- Queue drawer and current-playable details.
- Keyboard and screen-reader foundations.

## Non-goals

- Waveform salvage editor.
- Final visual branding.
- Native desktop packaging.

## Acceptance criteria

- Navigation does not reset transport or queue state.
- Large library queries remain responsive with virtualized rendering.
- Refreshing projections does not silently move selection to another object.
- Unavailable and provisional states include text/icon treatment, not color alone.
- The shell is fully operable by keyboard for library selection and transport.

## Verification

UI state tests cover route changes, selection stability, queue visibility, and
keyboard transport. A representative corpus fixture demonstrates virtualization.
