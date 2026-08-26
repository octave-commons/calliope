# FT-000D spike — native runtime evidence

Date: 2026-08-02. Host: Linux (X11, DISPLAY=:0, PulseAudio), OpenJDK 21.0.11.

Evidence for ADR-002. Three spikes, each runnable from this directory:

```bash
clojure -P                     # fetch deps (one time)
clojure -M -m audio-spike      # A: JavaFX Media vs real corpus MP3
clojure -M -m window-spike     # B: cljfx native window + in-process boundary
clojure -M -m read-model-spike # C: rebuildable SQLite read model
```

## A — audio (JavaFX Media)

Target: `tracks/aquila-regina/211cce48.mp3` (4,389,702 bytes), a real Suno
render. Volume 0.25, ~5 s of audible playback.

Observed output:

```
media: .../tracks/aquila-regina/211cce48.mp3 (4389702 bytes)
duration: 187824 ms (187.8 s)
play 2.0 s at volume 0.25 ...
seek to 56347 ms (30%) ...
position after seek: 56846 ms
pause 1.0 s ...
status while paused: PAUSED (position 56846 ms)
resume 1.5 s ...
status while playing: PLAYING (position 58361 ms)
stop+dispose: clean
AUDIO SPIKE: PASS
```

## B — window (cljfx/JavaFX, no browser)

Minimal native window listing liked clips fetched through the query boundary;
transport state mutated only through the command boundary. Auto-exits.

```
query boundary -> :library/liked-titles returned 12 items
window shown on Linux via JavaFX 21.0.3
command boundary -> :transport/play
command boundary -> :transport/pause
final transport status: :paused
commands served through boundary: 2
WINDOW SPIKE: PASS
```

## C — read model (SQLite, rebuildable)

Built `target/studio/spike-index.db` from `ledgers/projections/suno-meta-v1.edn`,
queried, deleted the file, rebuilt, compared:

```
first build:  {:clip-count 825, :liked-count 107,
               :by-model [{:clips/model v5, :count 204}
                          {:clips/model v4.5-all, :count 49}], ...}
deleted spike-index.db — rebuilding from durable EDN ...
rebuild: identical
READ-MODEL SPIKE: PASS (rebuild is deterministic)
spike db removed; durable sources untouched
```

## Notes

- `cljfx` pulls `javafx-web` onto the classpath transitively; no web component
  is instantiated. The no-browser constraint of ADR-001 is honored.
- Headless environments need Monocle (`-Dprism.order=sw
  -Dglass.platform=Monocle`) for window/media tests.
- The "Unsupported JavaFX configuration: classes were loaded from unnamed
  module" warning is cosmetic for a spike; packaging (jlink) will use proper
  modules.
