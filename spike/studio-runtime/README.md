# Native runtime recovery probe

License: GPL-3.0-or-later.
Status: disposable discovery, no production studio implementation.

This probe independently rechecks the native choices recorded in closed
[PR #7](https://github.com/octave-commons/calliope/pull/7), immutable head
194512b4ce7e9e706f555a4f933449c632bb8fb5. Its historical ADR is preserved
byte-for-byte in [the recovered artifact](../../docs/research/recovered/pr-7-native-runtime-adr.md),
SHA-256 a27eff747e8b0c832cc6c37d3e262748dff8a3389da14208614c40ad2120ee25.
The current ADR-002 still owns the external media dataset. This file neither
reassigns that identity nor declares new architectural acceptance.

Run from this directory with JDK 21, Clojure CLI, Xvfb, PulseAudio/PipeWire and a
locally available MP3 longer than 6250 ms (seek/resume needs a safety margin). The example creates a private null sink and leaves the
default sink and all physical inputs unchanged:

```bash
module_id=$(pactl load-module module-null-sink sink_name=calliope_studio_probe)
trap 'pactl unload-module "$module_id"' EXIT
PULSE_SINK=calliope_studio_probe xvfb-run -a clojure -M -m native-probe \
  /absolute/path/to/corpus.mp3 /absolute/scratch/probe.db \
  "$PWD/phrases.edn" /absolute/scratch/window.png
```

The database path must be fresh; the probe refuses any existing path. It deletes
only the database it created to prove rebuilding. The screenshot argument is optional.

The newly authored probe fails nonzero on timeout, decode error, missing input,
failed state/position assertions, failed database deletion or mismatched rebuild.
It runs JavaFX commands on the FX thread. The native window calls command/query
functions; its controls exercise local operational state, not production playback.
The MediaPlayer transport is tested separately against actual MP3 bytes.
All media operations and transport polls check both Media and MediaPlayer error
properties on the FX thread and preserve a reported error as the failure cause.
HALTED without an error property is an explicit failure, not a timeout with an
invented cause. Cleanup still disposes a failed player. To verify that boundary
with synthetic error getters (no audio playback), run:

```bash
xvfb-run -a clojure -M -m ready-probe
```

This separate check verifies UNKNOWN before READY and READY before a transport
wait, followed by HALTED with an error on poll two for each channel. It checks
original-cause identity, explicit unreported HALTED, and READY without errors. It uses fake
getter objects through real FX-thread dispatch; it does not demonstrate an actual
codec error. A delayed invalid HTTP fixture timed out without setting either
error property, so it is not counted as error-channel verification.

The two-row EDN/SQLite fixture proves the chosen index can be discarded/rebuilt;
it does not establish production event admission or full session recovery.

Observed on 2026-10-25:

```text
WINDOW: actual cljfx native stage shown; in-process command/query exercised
DURATION: 187824.0
AUDIO: play/progress/pause-held/seek/resume-progress/stop asserted
SQLITE: deleted and rebuilt; identical query rows [[phrase-1 human observation] [phrase-2 provisional suggestion]]
ENVIRONMENT: Linux 21.0.12.1 JavaFX 21.0.3
NATIVE DISCOVERY PASS — private Xvfb/null sink; no hardware or human listening claim
```

Asset: Aquila Regina corpus MP3, 4389702 bytes, SHA-256
211cce48219584744f7f9b23768e55f7ca7dace4d7d52fd28b477049b86eac0e.
The source lived in the original checkout's dataset; no corpus bytes are copied
into this spike or committed. Dependencies match the historical selected versions.

Unverified: audible physical output, current desktop window journey, system media
keys, real recording, MIDI instruments, direct monitoring, capture latency,
cross-process production recovery and durable remote agent execution.
