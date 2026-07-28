---
title: "Fork Tales Media Workbench v1"
status: approved
requires-decisions: [ADR-001]
dependencies:
  - docs/research/media-workbench-interface-and-publishing.md
epics: [FT-000, FT-001, FT-002, FT-003, FT-004]
created: "2026-07-27"
approved: "2026-07-28"
---

# Fork Tales Media Workbench v1

## Outcome

A local-first native Clojure/JVM application that can replace an ordinary music
player for the Fork Tales corpus while progressively adding curation,
non-destructive audio salvage, release preparation, and publication.

The first usable milestone is not a publishing dashboard. It is a player pleasant
enough to leave open all day and fast enough to classify the corpus by listening.

## Product principles

1. **Playback never feels secondary.** Transport and queue persist across views.
2. **The source is never sacrificed to the edit.** Every cut is reversible.
3. **Partial value is visible.** A rejected render may contain accepted clips.
4. **Fast and deep judgment coexist.** One key records a quick decision; the
   inspector records richer scoped observations.
5. **Facts, proposals, and decisions remain distinct.** Models may suggest; they do
   not accept.
6. **Organization tools have explicit semantics.** Playlist, smart list,
   workspace, release, and development board are not aliases.
7. **External publication is an adapter.** The local release remains authoritative.
8. **Native means native.** The first client is Clojure/JVM and does not embed a
   browser runtime.

## Information architecture

```text
Fork Tales
├── Listen
│   ├── Library
│   ├── Queue
│   ├── History
│   └── Playlists / Smart Lists
├── Curate
│   ├── Triage Inbox
│   ├── Labels
│   ├── Ratings
│   └── Workspaces
├── Edit
│   ├── Markers
│   ├── Clips
│   ├── Arrangements
│   └── Exports
├── Release
│   ├── Candidates
│   ├── Target Packages
│   ├── Publication Attempts
│   └── Published Catalog
└── System
    ├── Corpus Health
    ├── Classifier Results
    ├── Jobs
    └── Rheos Development Board
```

The Rheos board coordinates repository work. User-created media workspaces are
application objects. They may link to each other, but the UI must not conflate
them.

## Native application boundary

The first client is one native Clojure/JVM desktop process. It has four explicit
layers:

```text
native views and input
  -> application commands and queries
  -> domain laws and services
  -> ledger, index, audio, analysis, export, and publication adapters
```

The application boundary is transport-neutral and may run in-process. A local HTTP
server is not required for the first client. UI code cannot write ledgers, invoke
FFmpeg, or call remote publication APIs directly.

FT-000D selects the first UI toolkit, audio backend, rebuildable read model, and
in-process application topology through a real local spike. Those choices are not
smuggled into later player cards.

## Application shell

### Persistent regions

1. **Left rail** — navigation, workspace switcher, playlists, smart lists, and
   release shortcuts.
2. **Main surface** — library table, triage stack, waveform editor, release
   builder, or board.
3. **Inspector** — provenance, scoped ratings, labels, notes, relations, and
   target state for the current selection.
4. **Bottom player** — transport, current playable, scrubber, queue controls,
   loudness mode, loop, and output device.

The player remains alive while the main surface changes. Opening a clip, workspace,
or release does not rebuild the audio graph or lose queue position.

The desktop layout is authoritative. Narrow windows may collapse the inspector or
left rail, but transport and current playable remain visible.

## Playable domain

### Work

The conceptual song or creative project. It may own lyrics, prompts, production
briefs, artwork, and relations to multiple renders.

### Render

An immutable audio artifact from Suno or another renderer. It records content
hash, source metadata, duration, codec, and relation to a work.

### Marker

A point or range annotation on a render or arrangement. Examples include
`good-intro`, `vocal-breaks-here`, `too-weird`, `usable-transition`,
`chorus-entry`, silence, and proposed section boundaries.

Markers have epistemic status. A model marker is derived or provisional; a user
marker is an explicit durable annotation.

### Clip

A non-destructive playable span:

```clojure
{:clip/id #uuid "..."
 :clip/source {:object/type :render :object/id "..." :sha256 "..."}
 :clip/range {:start-ms 0 :end-ms 43820}
 :clip/fades {:in-ms 0 :out-ms 500}
 :clip/gain-db 0.0
 :clip/title "Good opening before drift"
 :clip/status :accepted}
```

A clip may be rated, labeled, queued, placed in playlists, compared, or used in
arrangements.

### Arrangement

An ordered edit decision list of clips, gaps, and transitions. It is playable
before export. It may combine clips from sibling renders of one work or, when
explicitly allowed, different works.

### Export

A materialized WAV, FLAC, MP3, or video derivative. It records renderer version,
source hashes, arrangement version, encoding settings, and output hash.

## Rating and curation model

### Quick disposition

- `keeper` — valuable as a whole;
- `salvage` — inspect or retain specific spans;
- `reject` — not useful for the current purpose;
- `unreviewed` — no durable disposition yet.

Rejecting a render does not delete it and does not reject its clips.

### Rating dimensions

Default 0–5 dimensions may be applied to work, render, clip, arrangement, or
export:

- **enjoyment** — desire to hear it again;
- **publishability** — suitability for release in current form;
- **weirdness** — sonic or structural strangeness, not inherently bad;
- **salvageability** — value of extracting or recombining spans;
- **technical-quality** — artifacts, clipping, continuity, intelligibility.

The library displays chosen dimensions rather than an unexplained aggregate.

### Labels

Labels are user-authored and namespace-capable, for example:

```text
mood/nocturnal
voice/clean-female
issue/late-collapse
salvage/good-intro
release/needs-artwork
world/gates-of-aker
```

Classifier-proposed labels remain visually distinct until accepted.

## Core screens

### 1. Library

A virtualized table and optional artwork grid. Columns may include title, work,
playable type, duration, disposition, selected rating dimensions, labels, render
family, clip count, publication state, listening history, model/source/date, and
anomaly indicators.

Every meaningful column is sortable. Filters compose and can be saved as a smart
list or workspace. Selection remains stable while sorting, filtering, or refreshing
projections.

### 2. Triage Inbox

A keyboard-first listening queue for unreviewed renders.

Default actions:

- `1`–`5`: enjoyment rating;
- `K`: keeper;
- `S`: salvage;
- `X`: reject;
- `M`: marker at playhead;
- `I` / `O`: provisional in/out points;
- `C`: create clip from range;
- `[` / `]`: jump between markers or sections;
- `Enter`: full inspector;
- `Space`: play/pause;
- `J` / `L`: seek backward/forward;
- `Shift+J` / `Shift+L`: previous/next item.

Bindings are configurable and discoverable from an overlay.

### 3. Waveform Salvage Editor

The editor is optimized for one source render first, not a full DAW.

Required v1 behavior:

- zoomable waveform and overview;
- playhead, loop region, and in/out selection;
- marker lanes for user, deterministic analysis, and model proposals;
- create, name, and revise clips;
- bounded fades and gain;
- boundary audition with pre/post roll;
- manual sibling-render comparison with independently controlled playheads;
- drag accepted clips into an arrangement lane;
- source and derivation visible at all times.

Automatic cross-render alignment is deferred until an evidence-backed alignment
model exists. V1 comparison does not pretend approximate semantic sections are
synchronized.

Deferred DAW features include multitrack recording, plugins, MIDI, spectral
repair, and arbitrary destructive processing.

### 4. Playlists and Smart Lists

A playlist stores explicit order and may mix renders, clips, arrangements, and
exports. Reordering appends an edit event.

A smart list stores a closed query such as:

```clojure
{:where [:and
         [:gte :rating/enjoyment 4]
         [:eq :disposition :salvage]
         [:contains :labels :salvage/good-intro]]
 :sort [[:rating/salvageability :desc]
        [:duration-ms :asc]]}
```

Its current result is a projection, not frozen hidden membership.

### 5. Workspaces

A workspace restores active query and columns, pinned objects, queue or playlist,
comparisons, notes, selected rating dimension, active arrangement or release, and
layout state worth preserving.

Examples:

- “Best nocturnal tracks”;
- “Salvage intros with clean vocals”;
- “Gates of Aker release pass”;
- “YouTube visualizer batch”;
- “Compare rerenders of the same lyric.”

### 6. Release Builder

A release candidate is assembled from accepted local assets. It includes title,
type, audio exports, artwork, optional video treatment, artist identity, sequence,
descriptions, lyrics, credits, tags, provenance, rights basis, encoding checks,
target metadata, and acceptance state.

The builder shows a target matrix rather than one global publish button:

```text
Target       Capability          State
SoundCloud   direct-upload       ready
YouTube      resumable-upload    needs-video
Bandcamp     export-package      package-ready
Spotify      distributor-handoff metadata-incomplete
```

### 7. Publication Activity

Each target attempt has an independent state machine:

```text
planned -> validating -> rendering -> ready -> authenticating -> uploading
        -> processing -> published
        -> failed | unavailable | cancelled | manual-action-required
```

Retries create new attempts or resume explicit checkpoints. External IDs, URLs,
visibility, timestamps, and response summaries are recorded without credentials.

## Persistent player behavior

Required for daily-driver use:

- play/pause, previous/next, seek, loop;
- queue append, play-next, remove, reorder, save as playlist;
- resume last session;
- optional crossfade and gapless transition where supported;
- optional loudness normalization as a playback transform;
- waveform/position updates without ledger writes on every tick;
- system media-key integration through the native adapter;
- one unreadable item is skipped with an explicit error and does not destroy the
  queue.

Gate 1 is not accepted from fake media tests alone. It requires a representative
real corpus MP3 to play, seek, pause, resume, advance through the queue, and recover
from an unreadable item in the chosen native stack.

## Command/query boundary

Illustrative durable commands:

```clojure
{:command/type :rating/record
 :subject {:object/type :clip :object/id "..."}
 :dimension :enjoyment
 :value 5
 :scale :zero-to-five
 :request/id #uuid "..."}

{:command/type :clip/create
 :source {:object/type :render :object/id "..." :sha256 "..."}
 :range {:start-ms 1200 :end-ms 43820}
 :request/id #uuid "..."}

{:command/type :publication/request
 :release/id #uuid "..."
 :target/id :soundcloud
 :request/id #uuid "..."}
```

Every durable command is idempotent. Optimistic UI state must reconcile against
the resulting durable event.

## Storage and projections

Proposed durable ledgers:

```text
ledgers/studio.edn        ratings, labels, playlists, clips, arrangements,
                          workspaces, release decisions
ledgers/publication.edn   publication requests, attempts, checkpoints, outcomes
```

Existing ingest and classification ledgers remain separate authorities.

Proposed derived or local data:

```text
target/studio/index.*       rebuildable query projection
target/studio/waveforms/    rebuildable peak data
target/studio/exports/      derived media artifacts
user-local playback store   queue/session/history telemetry
```

FT-000D chooses the first index and playback/session adapters. Exact paths are not
fixed before that evidence.

## Visual language and accessibility

- Dark and light modes; no meaning encoded by color alone.
- Artwork is prominent during listening but does not replace metadata.
- Observed, derived, provisional, accepted, rejected, stale, and unavailable have
  consistent text and icon treatments.
- Dense tables are preferred over decorative cards for corpus triage.
- Complete keyboard operation for player, tables, editor, and release validation.
- Visible focus, configurable shortcuts, screen-reader names, textual time-range
  editing, and reduced-motion behavior.

## Delivery slices

### Slice A — Native daily-driver listening

Native shell, selected audio backend, rebuildable library index, persistent queue,
resume, real-media verification, sorting/filtering, ratings, labels, and playlists.

### Slice B — Focused curation

Smart lists, workspaces, triage shortcuts, classifier overlays, and comparison
views.

### Slice C — Salvage and arrangement

Waveform peaks, markers, clips, fades, arrangement playback, and deterministic
export.

### Slice D — Release preparation

Release manifest, artwork/metadata validation, packages, provenance, and rights
checklist.

### Slice E — Publication adapters

SoundCloud direct upload, YouTube video generation/upload, then distributor and
manual-package workflows.

Each slice is independently useful. Publication work may not delay the player and
salvage editor.

## Owned decisions and remaining questions

FT-000D owns the first UI toolkit, playback backend, read-model implementation,
and in-process application topology. Later work may not invent those choices.

Remaining non-blocking questions:

1. Should listening history remain machine-local by default or optionally sync
   through a private ledger?
2. What normalization and export defaults fit the corpus?
3. What evidence is sufficient before automatic cross-render alignment is added?
4. Which distributor, if any, deserves a first-class adapter after packages work?

## Approval record

Err approved the direction after the Rheos config-discovery correction. The local
Claude review approved the design authority and requested board-mechanics repairs.
Those repairs moved prose outside `tasksDir`, adopted explicit Rheos `uuid`
identity, removed lossy snapshot authority, and created FT-000D for the uncovered
native runtime decisions.
