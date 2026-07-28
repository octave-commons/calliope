---
id: "b462ceab-e5ca-4a37-92d3-1d44ac8658a0"
title: "FT-004E: Implement distributor and manual publication handoffs"
status: icebox
type: story
priority: P1
phase: 4
epic: "9351aa82-a474-4dbc-8fee-be4acc09e02a"
owner: unassigned
points: 5
labels: [publishing, distributor, bandcamp, spotify]
category: stories
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
dependency: ["4b93e25c-f574-4ec3-ba5b-d7f5336dfb9e", "55a69664-5b24-4921-8761-c38a9234265b"]
---

# FT-004E: Implement distributor and manual publication handoffs

## Outcome

Targets without a suitable direct upload API can receive a complete release
package and an auditable manual or distributor workflow without false success
claims.

## Scope

- Spotify/distributor-oriented metadata package.
- Bandcamp/manual upload package and checklist.
- User-confirmed handoff and publication events.
- Target URLs/IDs recorded after external completion.
- Validation of target-specific missing fields.

## Non-goals

- Browser automation that impersonates unsupported APIs.
- Choosing a distributor without a separate decision.
- Treating package creation as publication.

## Acceptance criteria

- Package-ready, handed-off, submitted, and published remain distinct states.
- Manual confirmation identifies actor/time and supporting external reference.
- Target metadata omissions are visible before handoff.
- Credentials and private account details remain outside portable artifacts.
- A later direct/distributor adapter can replace the manual step without changing
  the local release identity.

## Verification

Tests cover package validation, manual confirmation, partial target completion,
revised submissions, and prohibited secret fields.