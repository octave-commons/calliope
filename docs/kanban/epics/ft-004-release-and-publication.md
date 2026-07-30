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

An accepted local release can be assembled through a native Release Builder,
generate complete target packages, and independently track direct uploads,
resumable uploads, distributor handoffs, and manual actions.

## Non-goals

- Making a remote platform canonical.
- Storing OAuth credentials in Git.
- Pretending all destinations expose upload APIs.

## Decomposed into

Listed in dependency order. Letters are stable labels, not sequence:

- FT-004A — release and target-capability laws.
- FT-004F — native Release Builder assembly, validation, and local acceptance.
- FT-004B — target-ready export packages.
- FT-004G — YouTube video assets.
- FT-004C — SoundCloud adapter.
- FT-004D — YouTube adapter.
- FT-004E — distributor/manual handoffs.

Implement the children, never this epic directly.

FT-004F consumes FT-000D's accepted native UI/runtime and topology decision and
produces the accepted release that FT-004B and every publication adapter require
as input. Without it, completing the other children still leaves nothing able to
make a release acceptable.

Video generation is FT-004G, separate from FT-004B's packaging, so that a target
needing no video is never blocked on video. Only FT-004D depends on FT-004G.
