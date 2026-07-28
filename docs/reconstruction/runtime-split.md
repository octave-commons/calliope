# Runtime split — why bb hosts most lanes and the JVM hosts DSP

The reconstruction pipeline runs in two runtimes. This document records why, so
the boundary does not get re-litigated or accidentally crossed.

## The short answer

**libpython-clj cannot run in babashka.** It needs JNA and dtype-next native
interop, and bb's GraalVM native image has neither — it cannot load jars or
native bindings at runtime. Measured, not assumed:

```
$ bb -e "(require '[libpython-clj2.python :as py])"
Could not locate libpython_clj2/python.bb, libpython_clj2/python.clj … on classpath

$ bb -e "(Class/forName \"com.sun.jna.Native\")"
absent: ClassNotFoundException
```

**You do not need a pod.** `babashka.pods` does load, so a pod is technically
possible — but a libpython-clj pod would itself be a JVM process. You would pay
the same JVM startup, gain an extra bencode hop, and maintain a pod wrapper. This
repo already has a JVM (`deps.edn`, `clojure -M:test`), so the DSP lane is just
another alias.

`libpython3.12.so` is present system-wide (`LIBDIR /usr/lib/x86_64-linux-gnu`),
so libpython-clj works fine on the JVM side.

## The boundary

| Lane | Runtime | Why |
|---|---|---|
| Handoff validation (μ1-μ6) | bb | pure data; instant startup |
| Rubric grading | bb | pure arithmetic over JSON |
| Judge-output parsing | bb | regex and string work |
| Event ledger | both | plain EDN append; no runtime-specific deps |
| Spectral metrics, spectrograms | **JVM** | librosa + matplotlib via libpython-clj |

```bash
bb scripts/reconstruction/validate.clj PACKET...   # bb lane
clojure -M:metrics ...                             # JVM + libpython-clj lane
```

## The ledger is the IPC

The two runtimes never call each other. They both append events to
`ledgers/reconstruction.edn`, and every derived view is a projection rebuilt from
that file. `reconstruction.ledger` deliberately requires nothing bb-only and
nothing JVM-only, so it loads identically in both.

This is what makes the pipeline native to the event-sourcing process rather than
merely scripted by it: a stage's output is an immutable event, not a return
value, so a lane can be re-run, replaced, or reimplemented in another runtime
without any other lane knowing.

## Startup cost, and why batching beats a resident pod

The JVM lane pays roughly: JVM boot (~1s) + CPython init + `import librosa`
(~2-3s). That is why the DSP lane takes a **batch** — a manifest of candidates,
one process per run, many measurements — instead of one invocation per file.

A resident pod would amortise that cost too, and is the right answer *only* if
you end up needing many small interactive DSP calls from a bb-rooted loop. Until
that is a measured problem, batching is strictly simpler.

## Current blocker

The metrics venv referenced by the old workflow —
`references/mir-workbench/.venv` — **does not exist**. `audio_metrics.py` is
therefore unrunnable as written, independently of any port. Whichever route is
taken, a Python environment with librosa and matplotlib has to be rebuilt first,
and libpython-clj must be pointed at that interpreter.

## Port status

| Tool | Status |
|---|---|
| `handoff_validate.py` | **ported** → `fork-tales.reconstruction.handoff` + `scripts/reconstruction/validate.clj`. Python file retired. |
| `audio_grade.py` | not yet ported. Pure stdlib + `difflib.SequenceMatcher`; needs a similarity-ratio equivalent. |
| `spectrogram_image_judge.py` | not yet ported. Pure stdlib + `re`; direct translation. |
| `audio_metrics.py` | **not portable to bb.** librosa/matplotlib. Belongs behind `-M:metrics` via libpython-clj. |
| `audio_agent.cljs` | already ClojureScript (nbb). Retarget to bb, or keep nbb and let it append events. |
| 25 `.py` + 4 `.mjs` generators under `references/` | historical provenance, not live pipeline. Leave as records. |
