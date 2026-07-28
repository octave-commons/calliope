---
id: ADR-001
title: "Local-first media workbench with immutable renders and non-destructive playable spans"
status: proposed
date: "2026-07-27"
deciders: [Err]
research: "docs/research/media-workbench-interface-and-publishing.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
---

# ADR-001: Local-first media workbench with immutable renders and non-destructive playable spans

## Context

Fork Tales owns a large corpus of lyrics, prompts, artwork, metadata, and audio
renders. The corpus is not adequately modeled as a folder of finished songs.
Some renders are excellent, some are unusable, and some contain valuable openings,
verses, transitions, textures, or vocal moments inside a globally poor render.

The desired application must support personal listening, curation, creative
salvage, arrangement, release preparation, and publication without making an
external platform or generated media file the authority for the work.

The existing repository already distinguishes canonical observations, derived
projections, provisional claims, and accepted decisions. The media interface must
preserve the same discipline.

## Decision

Fork Tales will build a **local-first media workbench** whose durable creative
state is append-oriented and whose source media is immutable.

### 1. Source media is immutable

An imported or generated audio render is never destructively edited in place.
Trimming, fading, gain changes, ordering, and composition are represented as
versioned edit decisions that reference source hashes and time ranges.

### 2. The playable abstraction is broader than a track file

The player accepts a common playable reference to any of:

- `:render` — immutable source audio;
- `:clip` — a bounded span of one render;
- `:arrangement` — an ordered edit decision list of clips and transitions;
- `:export` — a rendered derivative produced from an arrangement or release.

A playlist, queue, rating, label, or workspace may reference any playable type
unless a narrower scope is explicitly required.

### 3. Durable user intent is event-sourced

Ratings, labels, playlist edits, clip definitions, arrangements, workspaces,
release decisions, and publication outcomes are append-only events. Mutable UI
state and high-frequency playback telemetry are projections or local operational
state unless deliberately promoted.

Derived read models may use SQLite or another local index for responsive queries,
but that index is rebuildable and is never the only copy of a creative decision.

### 4. Ratings are multidimensional and scoped

The application supports independent judgments such as:

- enjoyment;
- publishability;
- weirdness;
- salvageability;
- technical quality.

A rating names its subject, dimension, value, scale, actor, and timestamp. A
clip-level rating does not silently become a render-level rating, and a render
rating does not become a work-level judgment.

### 5. Playlists, smart lists, and workspaces are distinct

- A **playlist** is an ordered durable sequence of playable references.
- A **smart list** is a saved query evaluated against current projections.
- A **workspace** is a saved attention context containing filters, pinned objects,
  queue context, notes, active comparisons, and optional edit/release focus.

A workspace may contain playlists and smart lists but is not reducible to either.

### 6. Editing is non-destructive and provenance-preserving

A clip records at least source render, start/end time, optional fades, gain, and
annotations. An arrangement records ordered clip references and transition
parameters. Exports preserve the complete derivation chain.

Automatic beat, silence, section, or anomaly detection may propose markers, but
only an explicit durable decision creates an accepted clip or arrangement.

### 7. Release is a local object before publication

A release candidate contains the accepted playable/export assets, artwork,
metadata, credits, lyrics, provenance, rights basis, and target-specific fields.
Publication cannot begin until a local release candidate exists.

Publication is independently tracked per target. A release may succeed on one
target and remain pending, rejected, unavailable, or manual on another.

### 8. Publishing adapters declare capabilities

Targets declare supported operations such as direct upload, resumable upload,
metadata synchronization, export package, manual handoff, or distributor handoff.
The UI must not present an export or manual step as a remote publication.
Credentials and refresh tokens remain outside Git and outside portable ledgers.

### 9. Interfaces share one application boundary

The browser/desktop UI, CLI, background workers, and future integrations invoke
versioned commands and queries over the same domain contracts. The UI does not
write ledgers, invoke FFmpeg, or call publication APIs directly.

The first implementation is a local service plus a desktop-first web UI. A thin
native shell may later provide media keys, file associations, notifications, and
packaging without changing the domain boundary.

### 10. Development is governed by research, ADR, design, and Rheos cards

Research preserves evidence and open questions. This ADR governs architecture.
The design document governs the intended user experience. Rheos cards coordinate
bounded implementation and may not silently override either.

## Consequences

### Positive

- Partial-value renders are salvageable without corrupting source audio.
- Personal listening and production use the same library and provenance model.
- Ratings and labels remain meaningful at work, render, clip, and arrangement
  scope.
- Publication integrations can vary without changing release truth.
- The interface can become a daily driver before all editing and publishing
  features exist.

### Costs

- The domain is richer than a conventional track table.
- A responsive player requires derived indexes, waveform/peak projections, and
  careful queue state.
- Export and publishing require background job, retry, credential, and checkpoint
  infrastructure.
- Non-destructive editing requires explicit timeline and render-engine contracts.

### Risks and mitigations

- **Event noise:** raw playback ticks stay operational; durable ledgers receive
  bounded decisions and summaries.
- **Scope explosion:** delivery is gated player -> curation -> salvage -> release
  -> publication.
- **External API drift:** adapter capabilities are versioned and availability is
  explicit.
- **Accidental source mutation:** media writes are content-addressed derivatives;
  source hashes are verified before export.
- **False automation authority:** generated markers and classifications remain
  derived/provisional until accepted.

## Rejected alternatives

### Treat each audio file as the song

Rejected because it cannot represent multiple renders, useful sub-spans, or new
arrangements without copying and losing lineage.

### Destructively edit source MP3 files

Rejected because it destroys evidence, makes experiments irreversible, and
breaks repeatable export.

### Use playlists as the universal organization model

Rejected because ordered listening, query views, creative project context, and
release state have different semantics.

### Publish directly from arbitrary library items

Rejected because publication needs an accepted local release record, target
metadata, rights/provenance, and target-specific state.

### Make a remote music service canonical

Rejected because external resources can disappear, transcode source audio,
change APIs, or represent only a subset of the local corpus.

## Acceptance conditions

This ADR may become `accepted` when Err confirms these boundaries:

1. immutable source renders;
2. first-class clips and arrangements;
3. distinct playlists, smart lists, and workspaces;
4. local release before publication;
5. per-target publication state;
6. local-first service and shared application boundary.