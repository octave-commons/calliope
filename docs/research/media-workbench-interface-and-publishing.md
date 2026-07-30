---
title: "Media Workbench Interface and Publishing Research"
status: complete-for-adr-001
kind: research
created: "2026-07-27"
accessed: "2026-07-27"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
---

# Media Workbench Interface and Publishing Research

## Question

What interface and system boundaries let Fork Tales become all three of these
without surrendering source ownership or provenance?

1. a daily-driver local music player;
2. a curation and salvage workbench for uneven AI renders;
3. a release and distribution control surface.

## Corpus-specific observations

The current corpus is not a conventional album library. A conceptual work may
have multiple renders, and a render may contain only a few valuable spans. A
file-level keep/reject decision therefore destroys useful information.

The interface must support these distinct judgments:

- the whole render is enjoyable;
- a bounded span is enjoyable;
- the render is not publishable as a whole;
- one or more spans are salvageable;
- a new arrangement can be assembled from immutable source spans;
- a release candidate is acceptable for one target but not another.

The system must not make "song file" the only playable or reviewable unit.

## Daily-driver player findings

A daily-driver player needs persistent transport and queue behavior independent
of the current screen. Library browsing, rating, labeling, waveform inspection,
and release work must not interrupt playback unless the user explicitly changes
the queue.

The minimum useful shell is:

- a persistent player and queue;
- a fast sortable/filterable library;
- keyboard-first triage;
- playlists and query-backed smart lists;
- saved workspaces;
- an inspector for provenance, ratings, labels, and notes;
- a waveform editor that creates non-destructive spans and arrangements.

High-frequency playback telemetry and durable creative decisions have different
storage needs. Ratings, labels, playlists, clips, arrangements, releases, and
publication outcomes are durable intent. Raw progress ticks and every seek event
are operational telemetry and should remain local/derived unless explicitly
promoted.

## Publishing platform facts

### SoundCloud

SoundCloud's official API supports OAuth-authenticated track upload with
`POST /tracks`, metadata updates, and playlist creation. The uploaded audio is
transcoded for playback, while the source file remains the upload input.

Sources:

- https://developers.soundcloud.com/docs/api/
- https://help.soundcloud.com/hc/en-us/articles/360039171614

**Implication:** SoundCloud can have a true direct-upload adapter, but a
successful API request is only a target publication event. It does not become
the canonical release record.

### YouTube

The YouTube Data API supports video upload through `videos.insert`, including
resumable upload. YouTube does not accept an audio-only file as a normal channel
video upload, so an audio release needs a generated video container: still art,
animated artwork, lyrics, waveform, or another visual treatment.

Unverified API projects created after July 28, 2020 may have uploaded videos
restricted to private viewing until the project passes a compliance audit.

Sources:

- https://developers.google.com/youtube/v3/docs/videos/insert
- https://developers.google.com/youtube/v3/guides/using_resumable_upload_protocol
- https://support.google.com/youtube/answer/57407

**Implication:** the YouTube adapter depends on an explicit video-rendering step
and must preserve privacy/audit state rather than assuming a public upload.

### Bandcamp

Bandcamp's documented API covers account, sales-report, and merchandise-order
workflows for approved partners. It does not document a general track-upload API
for artist releases.

Source:

- https://bandcamp.com/developer

**Implication:** Bandcamp starts as an export-package/manual-handoff target. The
workbench should generate audio, artwork, metadata, credits, lyrics, and a
checklist without pretending it performed the publication.

### Spotify and distributor-mediated services

Spotify for Artists states that music reaches Spotify through a distributor.
Some video features may support direct artist upload for eligible accounts, but
that is not a general audio-release ingestion API.

Sources:

- https://artists.spotify.com/en/get-started
- https://artists.spotify.com/en/providers
- https://support.spotify.com/artists/article/uploading-videos/

**Implication:** Spotify and similar DSPs begin as distributor export targets.
Future integrations may target a chosen distributor, but the core release model
must not assume each destination exposes a direct upload API.

## Required publication capability model

A publication target should declare one of these capabilities:

- `:direct-upload` — the application can create the target resource;
- `:resumable-upload` — direct upload with durable session/checkpoint state;
- `:export-package` — the application creates a complete target-ready bundle;
- `:manual-handoff` — the application records instructions and user completion;
- `:distributor-handoff` — the application prepares a release for a distributor;
- `:metadata-sync` — the application can update selected metadata after publish.

Capability declarations are target- and account-specific. The UI must never
present an export or manual handoff as a successful remote publication.

## Design conclusions

1. **Immutable source audio.** Original renders are never destructively trimmed
   or overwritten.
2. **Playable spans are first-class.** A clip can be rated, labeled, queued,
   placed in a playlist, and used in an arrangement.
3. **Arrangements are edit decision lists.** They reference clips and transforms;
   exported files are derived artifacts.
4. **Player and editor share one playable protocol.** Render, clip, arrangement,
   and export can enter the same queue.
5. **Workspaces preserve attention.** A workspace stores saved queries, pinned
   objects, queue context, notes, and an active release/editing focus.
6. **Release precedes publication.** A release candidate is accepted locally
   before any target adapter runs.
7. **Publication is per target.** Partial success is normal and inspectable.
8. **Credentials remain outside Git.** Ledgers may retain target IDs and outcomes,
   never access or refresh tokens.
9. **Ratings are scoped dimensions.** Enjoyment, publishability, weirdness, and
   salvageability are separate judgments; one score must not erase another.
10. **The board coordinates implementation.** Research and ADRs govern design;
    Rheos cards break the accepted design into bounded work.
