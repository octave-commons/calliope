---
id: ADR-001
title: "Local-first native media workbench with immutable renders and non-destructive playable spans"
status: accepted
date: "2026-07-27"
accepted: "2026-07-28"
deciders: [Err]
research: "docs/research/media-workbench-interface-and-publishing.md"
design: "docs/designs/media-workbench-v1.md"
process: "docs/process/product-design-and-delivery.md"
---

# ADR-001: Local-first native media workbench

## Context

Fork Tales owns a large corpus of lyrics, prompts, artwork, metadata, and audio
renders. The corpus is not adequately modeled as a folder of finished songs.
Some renders are excellent, some are unusable, and some contain valuable openings,
verses, transitions, textures, or vocal moments inside a globally poor render.

The application must support personal listening, curation, creative salvage,
arrangement, release preparation, and publication without making an external
platform, generated derivative, or mutable database row the authority for the
work.

The user also requires a Clojure-native desktop application and does not want to
embed a browser merely to obtain an interface.

## Decision

Fork Tales will build a **local-first native Clojure/JVM media workbench** whose
durable creative state is append-oriented and whose source media is immutable.

### 1. Source media is immutable

An imported or generated audio render is never destructively edited in place.
Trimming, fading, gain changes, ordering, and composition are versioned edit
decisions referencing source hashes and time ranges.

### 2. The playable abstraction is broader than a track file

The player accepts a common playable reference to any of:

- `:render` — immutable source audio;
- `:clip` — a bounded span of one render;
- `:arrangement` — an ordered edit decision list of clips and transitions;
- `:export` — a materialized derivative produced from an arrangement or release.

A queue, playlist, rating, label, or workspace may reference any declared
playable type unless a narrower scope is explicit.

### 3. Durable user intent is event-sourced

Ratings, labels, playlist edits, clip definitions, arrangements, workspaces,
release decisions, and publication outcomes are append-only events. Mutable UI
state and high-frequency playback telemetry remain projections or local
operational state unless deliberately promoted.

A local read model may use SQLite, Datascript, or another embedded store for
responsive queries, but it is rebuildable and never the only copy of a creative
decision.

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
  queue context, notes, comparisons, and optional edit or release focus.

A workspace may contain playlists and smart lists but is not reducible to either.

### 6. Editing is non-destructive and provenance-preserving

A clip records at least source render, start/end time, optional fades, gain, and
annotations. An arrangement records ordered clip references and transition
parameters. Exports preserve the complete derivation chain.

Automatic beat, silence, section, anomaly, or alignment analysis may propose
markers, but only an explicit durable decision creates an accepted clip or
arrangement.

### 7. Release is local before publication

A release candidate contains accepted playable or export assets, artwork,
metadata, credits, lyrics, provenance, rights basis, and target-specific fields.
Publication cannot begin until a local release candidate exists.

Publication is independently tracked per target. A release may succeed on one
target and remain pending, rejected, unavailable, or manual on another.

### 8. Publishing adapters declare capabilities

Targets declare supported operations such as direct upload, resumable upload,
metadata synchronization, export package, manual handoff, or distributor handoff.
The UI must not present an export or manual step as remote publication.
Credentials and refresh tokens remain outside Git and portable ledgers.

### 9. Interfaces share one application boundary

The native UI, CLI, workers, and future integrations invoke versioned commands and
queries over the same application contracts. UI components do not directly write
ledgers, invoke FFmpeg, or call publication APIs.

The first client is a **native Clojure/JVM desktop application with no embedded
browser runtime**. The command/query boundary may run in-process initially; a
network service is optional deployment topology, not a prerequisite or product
boundary. Native media keys, notifications, file associations, waveform drawing,
and packaging belong to adapters around the same domain.

### 10. Development is governed by research, ADRs, designs, and Rheos cards

Research preserves evidence and open questions. This ADR governs architecture.
The approved design governs intended user behavior. Rheos cards coordinate
bounded work and may not silently override either.

## Consequences

### Positive

- Partial-value renders remain salvageable without corrupting source audio.
- Personal listening and production use one library and provenance model.
- Ratings and labels retain work, render, clip, arrangement, and export scope.
- Publication integrations can vary without changing release truth.
- The player can become a daily driver before editing and publishing are complete.
- The UI does not inherit a browser stack merely for convenience.

### Costs

- The domain is richer than a conventional track table.
- A responsive native player requires a chosen UI toolkit, audio backend, read
  model, waveform projections, and careful transport state.
- Export and publishing require jobs, retry, credential, and checkpoint
  infrastructure.
- Non-destructive editing requires explicit timeline and render-engine contracts.

### Risks and mitigations

- **Event noise:** raw playback ticks stay operational; durable ledgers receive
  bounded decisions and summaries.
- **Scope explosion:** delivery is gated player -> curation -> salvage -> release
  -> publication.
- **Native-stack uncertainty:** FT-000D owns an evidence-producing spike for the UI,
  playback, read-model, and application topology before those adapters become
  implementation assumptions.
- **External API drift:** adapter capabilities are versioned and availability is
  explicit.
- **Accidental source mutation:** derivatives are content-addressed and source
  hashes are verified before export.
- **False automation authority:** generated markers and classifications remain
  derived or provisional until accepted.

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

Rejected because publication needs an accepted local release, target metadata,
rights/provenance, and target-specific state.

### Make a remote music service canonical

Rejected because external resources can disappear, transcode source audio,
change APIs, or represent only a subset of the local corpus.

### Make a browser UI the first client

Rejected because it conflicts with the native daily-driver goal and would pull a
browser runtime into the application before evidence shows it is necessary.

## Acceptance record

Err approved the media-workbench direction on 2026-07-28 after requiring Rheos
configuration discovery rather than repeated `--tasks-dir` flags. The independent
local Claude review approved ADR-001 and the design authority while finding board
mechanics that were subsequently corrected. The accepted boundaries are:

1. immutable source renders;
2. first-class clips and arrangements;
3. distinct playlists, smart lists, and workspaces;
4. local release before publication;
5. per-target publication state;
6. a shared application boundary;
7. a native Clojure/JVM first client with no embedded browser.
