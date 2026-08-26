---
category: "stories"
labels: "native-ui, player, library"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-001b-implement-playback-resolver-persistent-queue-and-resume"]
process: "docs/process/product-design-and-delivery.md"
phase: "1"
type: "story"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1785714740220-0.fojwnons5g9m1htr0kq"
points: "5"
title: "FT-001C: Build native persistent player shell and library browser"
priority: "P0"
status: "blocked"
epic: "ft-001-ship-a-daily-driver-library-and-player"
design: "docs/designs/media-workbench-v1.md"
uuid: "ft-001c-build-persistent-player-shell-and-library-browser"
research: "docs/research/media-workbench-interface-and-publishing.md"
owner: "unassigned"
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

---
Dependency analysis (2026-08-02): depends on FT-000D and FT-001B. Blocks FT-001D and FT-003B. Moved breakdown -> blocked: waiting on FT-000D and FT-001B.
---