# Synthetic capture implementation — CS-002

License: GPL-3.0-or-later. This is implementation evidence, not acceptance of
the complete studio workflow. Parent planning converged at `c0d39fa` with the
canonical gate reporting current-head CodeRabbit and MiMo approval, all 19
conversations settled and required checks passing. CS-001 completed its bounded
native-decision reconciliation/probe scope through Rheos; CS-002 then passed
through ready before its RED commit `1db248f`.

## Implemented boundaries

`calliope.law.capture` defines closed, named, plain-data Malli clocks, inputs,
normalized events/notes/interruptions, ingest commands and take/source shapes.
Every registered type is compiled in the tests; schema data round-trips through
EDN. Cross-field interpretation is separate from structural validation.

`calliope.studio.midi` interprets one declared device over a bounded constant-tempo
PPQ source. It preserves source packet order, raw tick/sequence/epoch and normalized
sample offsets. Velocity-zero onset means off; same-pitch overlap pairs FIFO;
channels and sustain pedals remain independent. Physical key and sounding spans
are separate. Disconnect, stop and end-of-take interruptions are derived data,
never fabricated source MIDI messages. Failed interpretation returns no partial
events/notes. Samples are exact integers within 0..9007199254740991. Intermediate
products use exact arithmetic, and sample-to-ms views round outward. JVM execution
is verified; the CLJS branch has not been executed.

`calliope.studio.synthetic` explicitly generates four seconds of stereo, 48 kHz,
16-bit little-endian PCM WAV plus an SMF containing six notes. Seed42 is the
demonstration source. The two channel roles are `:guitar-fixture` and
`:voice-fixture`; they are test tones, not recorded guitar/voice or fidelity
evidence. Staged fixture writes never overwrite different existing bytes.

`calliope.studio.ingress` snapshots staged WAV/SMF sources. The native arm boundary
opens an opaque, closeable grant bound to configured user, session, process, clock,
origin and anchored directory identity. The UI-user entrypoint validates authority,
active grant, closed command, session revision/configuration and direct leaf names
before source access. Separate provider/system entrypoints always refuse, even
when passed a nominal user actor. Body fields cannot choose actor or entrypoint.

Both files must be direct regular entries and each must fit the explicit 8 MiB
v1 snapshot bound. Symlinks, directories, FIFOs, absolute/nested/traversal/URL/drive
names and replaced roots refuse. Source payloads are never read through a
check-then-open leaf pathname: fixed trusted Linux helper code uses `O_PATH` and
`fstat`, retains each regular inode, then opens that exact inode through procfs.
The helper verifies SHA-256 over actual bytes and detects mutation; the JVM
validates complete PCM WAV, actual PPQ/constant tempo and supported MIDI messages.
Unknown MIDI message classes are explicit unsupported outcomes. Original staged
bytes are unchanged; returned byte arrays are transient adapter results.

The helper requires Linux secure directory handles, procfs and
`/usr/bin/python3` with standard library only. It runs with isolated Python flags,
fixed classpath source, cleared environment, bounded input/output and a ten-second
process deadline. Host inability to enforce confinement returns non-success.
No model, network, device or canonical-storage operation runs in that helper.

## Verification and limits

RED checkpoints: missing pure interpreter (`1db248f`); missing ingress adapter
(`1198dcb`). A later large-tick regression exposed rejecting a valid exact bigint
before conversion to the stored bounded integer. The named-schema compilation
test also guards the materialized Malli utility registry used by `:merge`.

The JVM suite includes real WAV/SMF generation and decoding, deterministic hashes
and normalized values, unchanged source bytes, and negative authorization/arming/
revision/configuration/path/type/root/hash/format/tempo/message cases. Spies prove
the preflight rejection paths never call the payload-snapshot seam. Separate
Python tests execute actual descriptor behavior, including replacing the leaf
with an outside symlink between metadata inspection and payload open; only the
retained original inode is read. FIFO/directory/symlink inputs open zero
read-capable source descriptors. The Python suite is invoked by the JVM suite
and has a ten-second alarm against hanging regressions.

A controlled pending provider worker does not prevent actual source ingress;
failed/unavailable provider stubs are never called by ingress. This is a mocked
separation check, not live model/session integration or a real-time audio latency
measurement. The live provider discovery remains separately labelled evidence.

This adapter does **not** append events, publish canonical immutable source files,
track durable request idempotency, rebuild SQLite, or recover a studio session.
Those are CS-003 application-service responsibilities and must precede native
views. It must be called only after that service prepares a new request against
its current admitted session projection. Generic event identity, locking,
durability, uncertainty/retry and replay stay upstream in Clio. There is no copied
generic ledger implementation here.

Still required: CS-003 durable source publication/session transactions and failure
recovery; CS-005 curation/proposal/human-decision admission; CS-004 usable native
controls/playback; CS-006 desktop profile; CS-007 complete actual restart demo.
No real input device, physical monitoring, human listening, human creative
acceptance or durable remote agent execution is claimed by this evidence.
