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
generate independent target packages and video assets, track render jobs, direct
uploads, resumable uploads, distributor handoffs, and manual actions, and expose
those activities through a native Publication Activity screen with explicit retry
and recovery actions.

## Non-goals

- Making a remote platform canonical.
- Storing OAuth credentials in Git.
- Pretending all destinations expose upload APIs.

## Decomposed into

Listed in dependency order; FT-004B and FT-004G are parallel siblings after
FT-004F. Letters are stable labels, not sequence:

- FT-004A — release and target-capability laws.
- FT-004F — native Release Builder assembly, validation, and local acceptance.
- FT-004B — target-ready export packages.
- FT-004G — YouTube video assets and durable render-job activity.
- FT-004C — SoundCloud adapter.
- FT-004D — YouTube adapter.
- FT-004E — distributor/manual handoffs.
- FT-004H — native Publication Activity and retry view.

Implement the children, never this epic directly.

FT-004F consumes FT-000D's accepted native UI/runtime and topology decision and
produces the accepted release that FT-004B and FT-004G consume. Publication
adapters consume target-ready outputs derived from that same accepted release.
Without FT-004F, completing the other children still leaves nothing able to make a
release acceptable.

Packaging and video rendering are independent branches: FT-004B and FT-004G both
consume FT-004F, so a target needing no video is never blocked on video. FT-004D is
the only adapter that joins the package and video branches.

FT-004H consumes durable render-job records and commands owned by FT-004G plus the
independent attempt records and command surfaces owned by FT-004C, FT-004D, and
FT-004E. It implements the design's Publication Activity screen through FT-000D's
selected native UI/runtime rather than introducing a second publication state
machine in the view.
