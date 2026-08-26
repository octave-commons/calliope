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
| Evidence path preflight | bb | pure data; must run before any grader |
| Handoff validation (μ1-μ6) | bb | pure data; instant startup |
| Rubric grading | bb (planned) / Python today | pure arithmetic over JSON; still `audio_grade.py` |
| Judge-output parsing | bb (planned) / Python today | regex and string work; still `spectrogram_image_judge.py` |
| Event ledger | both | plain EDN append; no runtime-specific deps |
| Spectral metrics, spectrograms | **JVM** (declared) / venv Python today | librosa + matplotlib; see the caveat below |

```bash
bb scripts/reconstruction/validate.clj PACKET...   # bb lane, runnable now
clojure -M:metrics ...                             # DECLARED, NOT YET RUNNABLE
```

`clojure -M:metrics` fails today — measured 2026-07-28:

```
$ clojure -M:metrics --help
Could not locate reconstruction/metrics__init.class, reconstruction/metrics.clj
  or reconstruction/metrics.cljc on classpath
```

The alias exists and its deps resolve (libpython-clj 2.025 downloads and
`libpython-clj2.python` loads on the JVM), but nobody has written
`reconstruction.metrics` yet. Until then the DSP lane is `audio_metrics.py` run
under the venv below. The boundary in this table is the design; the runnable
surface is the venv.

## The ledger is the IPC

The two runtimes never call each other. They both append events to
`ledgers/reconstruction.edn`, and every derived view is a projection rebuilt from
that file. `reconstruction.ledger` deliberately requires nothing bb-only and
nothing JVM-only, so it loads identically in both.

One classpath caveat, measured 2026-07-28: the namespace is *portable*, but on
the JVM it is not *reachable* by default. `bb.edn` puts `scripts` on the bb
classpath; `deps.edn` `:paths` is `["src" "resources"]`, so a bare
`clojure -M -e "(require 'reconstruction.ledger)"` raises
`FileNotFoundException`. The `:metrics` alias supplies
`:extra-paths ["scripts"]`, which is the intended JVM route — and with that path
added, the namespace loads and appends and re-reads events identically to bb
(verified both directions on the same ledger file).

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

## Metrics environment — rebuilt and verified 2026-07-27

The venv at `~/Music/fork-tales/references/mir-workbench/.venv` had been deleted
and was rebuilt with `--system-site-packages` plus two missing packages.

The failure mode is worth recording, because a naive check reports success:
librosa 0.11 uses `lazy_loader`, so `import librosa` succeeds even when its
dependencies are absent. Only real use fails.

```
$ python3 -c "import librosa; print(librosa.__version__)"   ->  0.11.0
$ python3 -c "import librosa; librosa.note_to_hz('C4')"     ->  No module named 'joblib'
```

`joblib` and `platformdirs` were missing. A user-level `pip install` is blocked by
PEP 668 on this host, so the venv is the supported route:

```bash
python3 -m venv --system-site-packages ~/Music/fork-tales/references/mir-workbench/.venv
~/Music/fork-tales/references/mir-workbench/.venv/bin/pip install joblib platformdirs
```

`audio_agent.cljs` defaults `--metrics-python` to that interpreter, so the lane
works once the venv exists. Verified end-to-end: the agent's `metrics` subcommand
reproduced the committed `metrics.json` for the v16 opening audit — 6 of 7 values
bit-identical, the seventh differing by 2e-9 float noise, with both input
`sha256` matching.

libpython-clj must be pointed at this same interpreter.

## Port status

| Tool | Status |
|---|---|
| `handoff_validate.py` | **ported** → `fork-tales.reconstruction.handoff` + `scripts/reconstruction/validate.clj`. Python file retired. One reported field deliberately diverges — see below. |
| `audio_grade.py` | not yet ported. Pure stdlib + `difflib.SequenceMatcher`; needs a similarity-ratio equivalent. |
| `spectrogram_image_judge.py` | not yet ported. Pure stdlib + `re`; direct translation. |
| `audio_metrics.py` | **not portable to bb.** librosa/matplotlib. Belongs behind `-M:metrics` via libpython-clj. |
| `audio_agent.cljs` | already ClojureScript (nbb). Retarget to bb, or keep nbb and let it append events. |
| 25 `.py` + 4 `.mjs` generators under `references/` | historical provenance, not live pipeline. Leave as records. |

## The one place the handoff port does not match the retired tool

Differentially measured 2026-07-28. The retired `handoff_validate.py` was
recovered from the pre-retirement blob into a scratch directory and run as an
oracle against the Clojure interpreter over 365 packets — every
`handoff_kind` × `verdict` × `mode` × `role` combination in the schema
vocabulary, plus five structured fixtures.

Result: `ok`, `error_count`, `warning_count`, and every error `path` and
`message` agree on **all 365 cases**. The verdict the pipeline actually consumes,
and the exit code, are faithful.

`checked_specs` differs on 364 of 365, in exactly two ways:

- the Python tool called `checked("μ1")` unconditionally, before testing
  applicability, so μ1 appears in every report it ever wrote;
- it marked μ2/μ3 checked on `handoff_kind` alone, while the Clojure
  interpreter's `:applies-when` also requires a rejecting `verdict`.

The Clojure reading is the intended one — a spec is reported checked only if it
was actually evaluated — and `handoff_test.clj` pins it ("an accepting review
does not trigger μ2 at all"). The consequence to know about: **committed
`*.validation.json` artifacts replay with a wider `checked_specs` than the
current tool produces.** `heresy-v17-ending-tail-planner-assignment.validation.json`
records `["μ1"]`; re-running `validate.clj` on the same packet yields `[]` with
identical `ok`/`error_count`/`warning_count`. Neither is wrong; do not treat the
difference as evidence drift. `handoff_test.clj` locks both readings so the next
change to `:applies-when` has to be deliberate.
