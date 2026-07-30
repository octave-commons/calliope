---
uuid: "ft-001c-build-persistent-player-shell-and-library-browser"
title: "FT-001C: Build native persistent player shell and library browser"
status: incoming
type: story
priority: P0
phase: 1
epic: "ft-001-ship-a-daily-driver-library-and-player"
owner: unassigned
points: 5
labels: native-ui, player, library
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-001b-implement-playback-resolver-persistent-queue-and-resume"]
---

# FT-001C: Build native persistent player shell and library browser

## Outcome

A native Clojure/JVM desktop shell keeps playback mounted while Err browses,
filters, and sorts the corpus in a responsive library table.

## Scope

- FT-000D-selected native UI toolkit; no embedded browser.
- Left navigation, main surface, inspector, and persistent bottom player.
- Virtualized or equivalently scalable library rows.
- Column selection, sort, filter, and stable selection.
- Queue drawer and current-playable details.
- Keyboard, focus, and screen-reader foundations supported by the toolkit.
- In-process application command/query integration.

## Non-goals

- Waveform salvage editor.
- Final visual branding.
- Replacing the application boundary with direct ledger/audio calls from views.
- Browser frontend or webview packaging.

## Acceptance criteria

- Navigation does not reset transport or queue state.
- The shell contains no embedded browser runtime.
- Large library queries remain responsive with representative corpus volume.
- Projection refresh does not silently move selection to another object.
- Unavailable and provisional states include text/icon treatment, not color alone.
- Library selection and transport are keyboard-operable.
- View handlers dispatch application commands/queries rather than invoking ledgers,
  FFmpeg, or audio-file mutation directly.

## Verification

Native UI state tests cover view changes, selection stability, queue visibility,
keyboard transport, and application-boundary calls. A local smoke run demonstrates
the native window playing real corpus audio through FT-001B.
