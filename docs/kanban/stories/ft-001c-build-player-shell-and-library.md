---
id: "2cc53668-22a0-4701-91b9-001f2deb9ca6"
title: "FT-001C: Build persistent player shell and library browser"
status: incoming
type: story
priority: P0
phase: 1
epic: "033b5cda-ed7e-429b-95ac-a56c1ab7156d"
owner: unassigned
points: 5
labels: [ui, player, library]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["63a5b66e-a80b-414b-9522-f3972cc6a4dd"]
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