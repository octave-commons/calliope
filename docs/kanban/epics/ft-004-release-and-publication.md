---
uuid: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
title: "FT-004: Prepare releases and publish through explicit target capabilities"
status: icebox
type: epic
priority: P1
phase: 4
points: 34
labels: media-workbench, release, publishing, decomposed
category: epics
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-003-recover-valuable-spans-and-arrange-them-non-destructively"]
---

# FT-004: Prepare releases and publish through explicit target capabilities

## Outcome

An accepted local release can generate complete target packages and independently
track direct uploads, resumable uploads, distributor handoffs, and manual actions.

## Non-goals

- Making a remote platform canonical.
- Storing OAuth credentials in Git.
- Pretending all destinations expose upload APIs.

## Decomposed into

Listed in dependency order. Letters are stable labels, not sequence:

- FT-004A — release and target-capability laws.
- FT-004F — release builder assembly, validation, and local acceptance.
- FT-004B — export packages and YouTube video assets.
- FT-004C — SoundCloud adapter.
- FT-004D — YouTube adapter.
- FT-004E — distributor/manual handoffs.

Implement the children, never this epic directly.

FT-004F produces the accepted release that FT-004B and every publication adapter
require as input. Without it, completing the other children still leaves nothing
able to make a release acceptable.
