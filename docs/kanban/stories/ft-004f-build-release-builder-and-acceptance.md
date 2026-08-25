---
uuid: "ft-004f-build-release-builder-assembly-validation-and-local-acceptance"
title: "FT-004F: Build release builder assembly, validation, and local acceptance"
status: icebox
type: story
priority: P1
phase: 4
epic: "ft-004-prepare-releases-and-publish-through-explicit-target-capabilities"
owner: unassigned
points: 5
labels: release, application-service, publishing, native-ui
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["ft-000d-decide-native-desktop-playback-read-model-and-application-topology", "ft-004a-define-release-manifest-and-publication-target-laws"]
---

# FT-004F: Build release builder assembly, validation, and local acceptance

## Outcome

Err uses a native Release Builder to assemble a release candidate from accepted
local assets, see per-target readiness in a capability matrix, and record an
explicit local acceptance that downstream packaging and publication require.

## Scope

- Commands to create, revise, and abandon a release candidate.
- Assembly of title, type, audio exports, artwork, sequence, descriptions, lyrics,
  credits, tags, provenance, and rights basis under FT-004A's manifest law.
- Validation that reports missing or invalid fields per target rather than
  blocking the whole candidate.
- A target matrix query projecting each target's capability and readiness state.
- The local acceptance command that emits FT-004A's acceptance event.
- A native Release Builder screen implemented through FT-000D's selected UI/runtime
  and topology, with asset/metadata editing, validation results, the target matrix,
  and an explicit acceptance control.
- Pointer and keyboard interaction that dispatches the same application commands
  and renders candidate/matrix state from the same query projections.

## Non-goals

- Redefining release or target-capability law. That is FT-004A.
- Producing target packages. That is FT-004B.
- Producing video assets. That is FT-004G.
- Any upload, distributor, or manual-handoff adapter.
- A global publish action. Readiness stays per target.

## Acceptance criteria

- A candidate can be assembled, revised, and abandoned without mutating source
  renders or exports.
- The native Release Builder exposes every required candidate field and accepted
  local asset through pointer and keyboard operation.
- UI interactions dispatch the application-service commands; the screen does not
  maintain a second candidate or acceptance state outside the projection.
- Validation reports per-target readiness in the view, and one incomplete target
  does not render an otherwise-valid candidate unacceptable.
- Local acceptance is an explicit recorded decision, never inferred from a
  candidate merely being complete, and the UI surfaces refusal reasons before an
  acceptance event can be emitted.
- The target matrix distinguishes direct, resumable, export-package, manual, and
  distributor capabilities and refreshes after candidate changes.
- An accepted release is retrievable as the declared input to FT-004B and to every
  publication adapter.

## Verification

Application-service and projection tests cover assembly, per-target validation,
refusal to infer acceptance, matrix state derivation, and retrieval of an accepted
release by downstream consumers. Native interaction tests cover pointer/keyboard
field editing, asset selection, validation, matrix refresh, explicit acceptance,
refusal display, and command parity without UI-only candidate state. Confirm
through Rheos that FT-004B depends on this card and that no publication adapter
depends on an unaccepted candidate.

## Why this card exists

The approved design specifies a Release Builder that assembles a candidate from
accepted local assets and presents a per-target capability matrix
(`docs/designs/media-workbench-v1.md`, "Release Builder"). FT-004A defines release
and target law and explicitly excludes upload adapters; FT-004B's outcome begins
from an *already accepted* release. No card owned the service and native view that
produce one, so completing every previously declared FT-004 child still left
FT-004B and all publication adapters without a producible input. Raised in PR #3
review.

Letter ordering is a stable label, not a claim about sequence: this card runs after
FT-004A and before FT-004B. Sequence is defined by `dependency` edges. Existing
cards were not renumbered, because their UUIDs are canonical identity and are
referenced from sibling cards, the epic, and the delivery map.
