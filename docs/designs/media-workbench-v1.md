---
title: "Fork Tales Media Workbench v1"
status: open
requires-decisions: [ADR-001]
dependencies:
  - docs/research/media-workbench-interface-and-publishing.md
epics: [FT-001, FT-002, FT-003, FT-004]
created: "2026-07-27"
---

# Fork Tales Media Workbench v1

## Outcome

A local-first application that can replace an ordinary personal music player for
the Fork Tales corpus while progressively adding curation, non-destructive audio
salvage, release preparation, and publication.

The first usable milestone is not a publishing dashboard. It is a player that is
pleasant enough to leave open all day and fast enough to classify the corpus by
listening.

## Product principles

1. **Playback never feels secondary.** The transport and queue persist across all
   views.
2. **The source is never sacrificed to the edit.** Every cut is reversible.
3. **Partial value is visible.** A render can be globally rejected and still
   contain starred clips.
4. **Fast judgment and deep judgment coexist.** One keystroke can record a quick
   rating; the inspector can record richer scoped observations.
5. **The system distinguishes facts, proposals, and decisions.** Model markers
   and classifiers may suggest; they do not accept.
6. **Organization tools have explicit semantics.** Playlist, smart list,
   workspace, release, and board are not aliases.
7. **External publication is an adapter.** The local release remains authoritative.

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

The development board is repository/project coordination. User-created media
workspaces are application objects. They may link to each other, but the UI must
not conflate them.

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

The player remains mounted while the main surface changes. Opening a clip or
release must not rebuild the audio graph or lose queue position.

### Responsive priority

The desktop layout is authoritative. Narrow layouts may collapse the inspector
and left rail, but the player and current playable remain visible.

## Playable domain

### Work

The conceptual song or creative project. It may own lyrics, prompts, production
briefs, artwork, and relations to multiple renders.

### Render

An immutable audio artifact from Suno or another renderer. The render records its
content hash, source metadata, duration, codec, and relation to a work.

### Marker

A point or range annotation on a render or arrangement. Examples:

- `good-intro`;
- `vocal-breaks-here`;
- `too-weird`;
- `usable-transition`;
- `chorus-entry`;
- `silence`;
- classifier-proposed section boundary.

Markers have epistemic status. A model marker is derived/provisional; a user
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

An ordered edit decision list of clips, gaps, and transitions. The arrangement
is playable before export. It may combine clips from multiple renders of the
same work or, when explicitly allowed, different works.

### Export

A materialized derivative such as WAV, FLAC, MP3, or video. It records renderer
version, source hashes, arrangement version, encoding settings, and output hash.

## Rating and curation model

### Quick disposition

The triage view provides a fast disposition independent of numeric ratings:

- `keeper` — valuable as a whole;
- `salvage` — inspect or retain specific spans;
- `reject` — not useful for current purposes;
- `unreviewed` — no durable disposition yet.

Rejecting a render does not delete it and does not reject its clips.

### Rating dimensions

Default dimensions use a 0–5 scale and may be applied to work, render, clip,
arrangement, or export:

- **enjoyment** — desire to hear it again;
- **publishability** — suitability for release in current form;
- **weirdness** — degree of sonic/structural strangeness, not inherently bad;
- **salvageability** — value of extracting or recombining spans;
- **technical-quality** — artifacts, clipping, continuity, intelligibility.

The library exposes one chosen primary dimension at a time. It does not collapse
all dimensions into an unexplained aggregate.

### Labels

Labels are user-authored and namespace-capable:

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

## 1. Library

A virtualized table and optional artwork grid. Columns may include:

- title and work;
- playable type;
- duration;
- disposition;
- selected rating dimensions;
- labels;
- render family/variant count;
- clip count;
- publish state;
- last played / play count;
- model/source/date;
- anomaly indicators.

Every column is sortable where meaningful. Filters compose and can be saved as a
smart list or workspace view.

Selection must be stable while sorting, filtering, or classifier projections
refresh.

## 2. Triage Inbox

A keyboard-first listening queue for unreviewed renders.

Default actions:

- `1`–`5`: enjoyment rating;
- `K`: keeper;
- `S`: salvage;
- `X`: reject;
- `M`: add marker at playhead;
- `I` / `O`: set provisional in/out points;
- `C`: create clip from in/out range;
- `[` / `]`: jump between markers/sections;
- `Enter`: open full inspector;
- `Space`: play/pause;
- `J` / `L`: seek backward/forward;
- `Shift+J` / `Shift+L`: previous/next item.

Key bindings are configurable and discoverable from an overlay.

## 3. Waveform Salvage Editor

The editor is optimized for one source render first, not a full DAW.

Required v1 behavior:

- zoomable waveform and overview;
- playhead, loop region, in/out selection;
- marker lanes for user, deterministic analysis, and model proposals;
- create and rename clips;
- trim and adjust fades/gain non-destructively;
- audition clip boundaries without leaving the editor;
- compare sibling renders at the same approximate section;
- drag accepted clips into an arrangement lane;
- display source and derivation at all times.

Deferred DAW features include multitrack recording, plugins, MIDI, spectral repair,
and arbitrary destructive processing.

## 4. Playlists and Smart Lists

A playlist stores explicit order and may mix renders, clips, arrangements, and
exports. Reordering appends an edit event; history remains inspectable.

A smart list stores a query such as:

```clojure
{:where [:and
         [:gte :rating/enjoyment 4]
         [:eq :disposition :salvage]
         [:contains :labels :salvage/good-intro]]
 :sort [[:rating/salvageability :desc]
        [:duration-ms :asc]]}
```

The current result is a projection, not a frozen membership list.

## 5. Workspaces

A workspace restores a focused context:

- active saved query and visible columns;
- pinned works/renders/clips;
- current queue or playlist;
- open comparisons;
- notes and questions;
- selected rating dimension;
- active arrangement or release candidate;
- layout state worth preserving.

Examples:

- “Best nocturnal tracks”;
- “Salvage intros with clean vocals”;
- “Gates of Aker release pass”;
- “YouTube visualizer batch”;
- “Compare rerenders of the same lyric.”

## 6. Release Builder

A release candidate is assembled from accepted local assets.

Required fields:

- release title and type;
- primary audio export(s);
- artwork and optional video treatment;
- artist/display identity;
- track titles and ordering;
- descriptions, lyrics, credits, and tags;
- source/provenance summary;
- rights basis and user attestation;
- loudness/encoding checks;
- target-specific metadata;
- acceptance state.

The builder shows a target matrix rather than one global publish button.

```text
Target       Capability          State
SoundCloud   direct-upload       ready
YouTube      resumable-upload    needs-video
Bandcamp     export-package      package-ready
Spotify      distributor-handoff metadata-incomplete
```

## 7. Publication Activity

Each target attempt has its own state machine:

```text
planned -> validating -> rendering -> ready -> authenticating -> uploading
        -> processing -> published
        -> failed | unavailable | cancelled | manual-action-required
```

Retries create new attempt events or resume a checkpointed attempt. External IDs,
URLs, visibility, timestamps, and response summaries are recorded without
credentials.

## Persistent player behavior

Required for daily-driver use:

- play/pause, previous/next, seek, loop;
- queue append, play-next, remove, reorder, save as playlist;
- resume last session;
- optional crossfade and gapless transition where source formats permit;
- optional loudness normalization as a playback transform;
- waveform/position updates without ledger writes on every tick;
- system media-key integration when a desktop shell exists;
- failure isolation: one unreadable item is skipped with an explicit error and
  does not destroy the queue.

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

{:command/type :playlist/append
 :playlist/id #uuid "..."
 :playable {:object/type :clip :object/id "..."}
 :request/id #uuid "..."}

{:command/type :publication/request
 :release/id #uuid "..."
 :target/id :soundcloud
 :request/id #uuid "..."}
```

Illustrative queries:

```clojure
{:query/type :library/search
 :where [:and [:eq :disposition :salvage]
              [:gte :rating/enjoyment 4]]
 :sort [[:rating/salvageability :desc]]}

{:query/type :playable/resolve
 :playable {:object/type :arrangement :object/id "..."}}
```

Every durable command is idempotent. The UI uses optimistic projections only when
it can reconcile them against the resulting durable event.

## Storage and projection shape

Proposed durable ledgers:

```text
ledgers/studio.edn        ratings, labels, playlists, clips, arrangements,
                          workspaces, release decisions
ledgers/publication.edn   publication requests, attempts, checkpoints, outcomes
```

Existing ingest and classification ledgers remain separate authorities.

Proposed derived/local data:

```text
target/studio/index.sqlite       query/read projection
target/studio/waveforms/         rebuildable peak data
target/studio/exports/           derived media artifacts
user-local playback store        queue/session/history telemetry
```

Exact paths remain provisional until the law and adapter cards decide packaging
and portability.

## Visual language

- Dark and light modes, but no meaning encoded by color alone.
- Artwork is prominent during listening but does not replace metadata.
- Observed, derived, provisional, accepted, rejected, stale, and unavailable have
  text/icon treatments consistent with `PROCESS.md`.
- Waveform markers show source/actor and confidence on hover/focus.
- Dense tables are preferred over decorative cards for corpus triage.
- Animation is limited to transport, waveform, progress, and clear state changes.

## Accessibility

- Complete keyboard operation for player, triage, tables, editor, and release
  validation.
- Visible focus and configurable shortcuts.
- Screen-reader names for waveform controls and marker lists.
- Time ranges editable as text as well as pointer gestures.
- Status never communicated by color alone.
- Reduced-motion mode.

## Delivery slices

### Slice A — Daily-driver listening

Library index, persistent player, queue, resume, sorting/filtering, enjoyment
rating, disposition, labels, and playlists.

### Slice B — Focused curation

Smart lists, workspaces, triage shortcuts, classifier overlays, comparison views.

### Slice C — Salvage and arrangement

Waveform peaks, markers, clips, fades, arrangement playback, deterministic export.

### Slice D — Release preparation

Release manifest, artwork/metadata validation, export packages, provenance and
rights checklist.

### Slice E — Publication adapters

SoundCloud direct upload, YouTube video generation/upload, then distributor and
manual-package workflows.

Each slice must be independently usable. Publication work may not delay the
player and salvage editor.

## Open questions

1. Which derived index is preferred for the first local read model: SQLite,
   Datascript, or another embedded store?
2. Which desktop shell, if any, is required for the first daily-driver release?
3. Should raw listening history remain machine-local by default or optionally
   sync through a separate private ledger?
4. What normalization and export defaults are appropriate for the corpus?
5. How should cross-render section alignment be represented before robust audio
   analysis exists?
6. Which distributor, if any, deserves a first-class adapter after export
   packages are proven?
