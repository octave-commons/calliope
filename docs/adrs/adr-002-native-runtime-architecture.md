---
id: ADR-002
title: "Native runtime: cljfx/JavaFX UI, JavaFX Media playback, SQLite read model, in-process application boundary"
status: accepted
date: "2026-08-02"
accepted: "2026-08-25"
deciders: [Err]
research: "docs/research/media-workbench-interface-and-publishing.md"
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
design: "docs/designs/media-workbench-v1.md"
card: "FT-000D"
spike: "spike/ft-000d/"
---

# ADR-002: First native runtime architecture (FT-000D)

## Context

ADR-001 commits Fork Tales to a local-first native Clojure/JVM media workbench
with no embedded browser, and names four choices it deliberately defers to an
evidence-producing spike: UI toolkit, audio playback backend, rebuildable read
model, and in-process application topology. FT-000D owns those choices; later
player cards (FT-001B/C/D, FT-003B) may not invent them.

Evidence was produced on 2026-08-02 against the real corpus (a Suno MP3 render
of "Aquila Regina", 4,389,702 bytes, 187.8 s) on Linux (X11, PulseAudio,
OpenJDK 21.0.11). Exact commands and full spike code are in `spike/ft-000d/`.

## Decision

### 1. UI toolkit: cljfx over JavaFX 21 (linux classifier)

The first client uses **cljfx 1.9.5 on JavaFX 21.0.3**. JavaFX is a native JVM
widget toolkit; no browser runtime is loaded or used. A minimal cljfx window
rendered on the live desktop, listed liked clips loaded through the query
boundary, and served transport commands through the command boundary
(`spike/ft-000d/src/window_spike.clj`, "WINDOW SPIKE: PASS").

### 2. Audio backend: JavaFX Media (javafx-media)

Playback uses **javafx.scene.media.MediaPlayer**, the media stack bundled with
the chosen toolkit — one native dependency family covers both UI and audio.
Verified against the corpus MP3: duration reported (187,824 ms), play, seek to
30 % (position 56,846 ms), pause (status PAUSED, position held), resume
(status PLAYING, position advancing), stop and dispose clean
(`spike/ft-000d/src/audio_spike.clj`, "AUDIO SPIKE: PASS"). Corpus MP3s are
the render format, and MP3 is JavaFX Media's core supported codec.

### 3. Read model: SQLite (xerial sqlite-jdbc) via next.jdbc

The rebuildable query projection is a **single-file SQLite database** built
from durable EDN ledgers/projections. The spike built an index over the
suno-meta projection (825 clips, 107 liked), answered representative player
queries, deleted the file, and rebuilt byte-equivalent results — the store is
disposable; EDN remains truth (`spike/ft-000d/src/read_model_spike.clj`,
"READ-MODEL SPIKE: PASS"). A Lucene-derived search index may be added later as
a *separate* rebuildable projection (FT-005A scope); it is not the primary
read model.

### 4. Application topology: in-process command/query boundary

The UI invokes **versioned commands and queries as plain Clojure functions**
over an in-process boundary. No HTTP server is required or started. UI code
holds no references to ledger, FFmpeg, or publication adapters; adapters sit
behind the boundary (demonstrated in `window_spike.clj`: the view calls only
`handle-command` / `handle-query`).

## Consequences

### Positive

- One native dependency family (OpenJFX) covers UI, media, and future
  waveform canvas drawing; packaging can use jlink/jpackage per platform.
- Playback transport (play/pause/seek/resume/duration) is verified on real
  corpus audio before any player card builds on it.
- The read model is disposable by construction; rebuild determinism is tested
  behavior, not an aspiration.
- The boundary is testable without a UI, a server, or audio hardware.

### Costs / risks

- **Packaging:** JavaFX requires platform-classified artifacts and a
  jlink/jpackage step per OS; GraalVM native-image of a JavaFX application is
  not assumed (Gluon Substrate exists but is unverified here). The single-binary
  aspiration is therefore served by uberjar + jpackage, not native-image, until
  evidence says otherwise.
- **Media keys / notifications:** OS integration (MPRIS on Linux) is an adapter
  to build (FT-001E), not provided by the toolkit.
- **Headless CI:** media and window tests need a display or Monocle
  (`-Dprism.order=sw -Dglass.platform=Monocle`); CI must mark real-audio
  verification as environment-gated.
- JavaFX Media is not a pro-audio engine: sample-accurate salvage audition may
  eventually need a decoding adapter behind the boundary (deferred; FT-003
  scope to prove need).

## Rejected alternatives

- **Swing/AWT (Seesaw):** zero extra dependencies and proven, but audio would
  require a second, weaker stack (JLayer has no reliable seek/duration), and
  the table/accessibility work is heavier than cljfx. Kept as fallback only.
- **Skija/LWJGL:** immediate-mode GPU canvas; strong for waveforms, but every
  widget, focus, and accessibility behavior would be hand-built, and it has no
  media stack.
- **Embedded browser (JavaFX WebView, Electron, Tauri):** rejected by ADR-001;
  cljfx pulls javafx-web transitively onto the classpath but no web component
  is instantiated anywhere.
- **JLayer / javax.sound + mp3spi:** decode-oriented, unreliable seek and no
  duration metadata without full scans.
- **FFmpeg subprocess for transport playback:** acceptable for export jobs
  (FT-003C), wrong latency/control model for a daily-driver transport.
- **Datascript as the read model:** excellent in-memory queries but not durable
  by itself; SQLite covers restart-surviving indexes without rehydration.
  Datascript may still appear as an in-memory layer later; not this decision.
- **XTDB / external databases:** operational weight unjustified for a
  single-process local-first app.

## Acceptance conditions

Err's disposition on this ADR moves it to accepted and unblocks FT-000C (whose
projection law assumes this read-model choice) and the FT-001/FT-003
implementation spine. The spike evidence is reproducible:

```bash
cd spike/ft-000d
clojure -M -m audio-spike        # real corpus MP3 transport verification
clojure -M -m window-spike       # native window + in-process boundary
clojure -M -m read-model-spike   # rebuildable SQLite projection
```
