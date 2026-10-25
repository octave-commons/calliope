---
title: Studio session boundary — first synthetic music workflow
status: proposed
governing-adr: docs/adrs/adr-001-local-first-media-workbench.md
governing-design: docs/designs/media-workbench-v1.md
assignment: https://docs.google.com/document/d/1x9v4v6_lxl3iu41mT06XhqX656Q4pqYgsNQtWA926AE/edit
---

# Studio session boundary

License: GPL-3.0-or-later. Proposed capture-specific contract for planning review.
The assignment explicitly adds synthetic audio/MIDI capture to the approved media
workbench. This is a bounded adapter extension, not full DAW editing or a new
shared event schema. CS-002 produces named Malli laws and negative tests from this
contract before capture/storage/UI adapters are implemented. Existing accepted
Calliope media identity and Clio event/admission/replay law remain governing.

## Commands and trusted context

All application commands are plain data, with `:command/type`,
`:command/version 1`, a UUID-string `:request/id` and `:session/id`.
The local application entrypoint supplies trusted
`{:actor/id "...", :actor/kind :user|:agent|:system}` separately; a model payload
cannot choose or override this context. UI user commands and provider-completion
commands use separate entrypoints. User identity is the configured local operator;
this increment does not introduce remote multi-user authorization.

| Command | Input | Resulting durable fact |
| --- | --- | --- |
| `:studio/session-create` | Title, configured work identity and project/source-reference links | New session, initially unarmed |
| `:studio/arm` | Explicit local user action and declared capture configuration | Armed capture grant for this session/process only |
| `:studio/ingest` | Staged WAV/MIDI source paths and expected SHA-256 digests; declared clock below | Immutable source identities and normalized bounded take |
| `:studio/mark` | Recent `:start-sample`/`:end-sample` within the current take | Source-linked marker; no automatic accepted edit |
| `:studio/stop` | Explicit local user action | Capture stopped; subsequent ingest refused |
| `:studio/select-clip` | Audio render ref, source hash and nonempty sample range | Non-destructive clip selection; source remains immutable |
| `:studio/annotate` | Selected clip UUID, text and optional scoped labels | Attributed annotation on this clip |
| `:studio/request-proposal` | Selected clip UUID and bounded analysis request | Inspectable request; provider work runs outside capture |
| `:studio/proposal-result` | Request UUID, validated text, provider/model/prompt identity and evidence refs | Provisional proposal only; agent/system entrypoint |
| `:studio/decide` | Proposal UUID, `:accept` or `:reject` | Separate attributed human decision and source-event reference |

Studio selections retain exact `:start-sample`/`:end-sample` and the take clock's
`:sample-rate` alongside the owning workbench clip. To project this half-open
sample range into integer-millisecond `:clip/range`, use outward rounding:
`:start-ms = floor(1000 * start-sample / sample-rate)` and
`:end-ms = ceil(1000 * end-sample / sample-rate)`. The end uses the same outward
rounding policy, toward the end of the source rather than toward its beginning.
Compute by integer quotient/remainder; never store raw sample counts in ms fields.
The render's millisecond duration is likewise `ceil(1000 * total-samples /
sample-rate)`, so a selection ending at the source end remains bounded. At 48 kHz,
`[0,24000)` projects to `[0,500)` ms; `[1,2)` projects to `[0,1)` ms. The ms view
can include less than one extra millisecond at either boundary; precise studio
replay and recovery use the retained sample offsets, not a conversion back from ms.

Read/query/view/transport commands never create decision facts. A proposal never
modifies source bytes, clip selection or accepted labels. Unknown proposal, stale
source identity, agent self-promotion and malformed output return explicit failure.

Command results use a closed result envelope: :result/status is :ok, :error or
:uncertain; :request/id echoes the request. Success contains :result/value and
the admitted :event/id for durable commands. Error contains :error/code and
:error/message, with no success event ID. The unarmed rejection code is
:capture/not-armed. Uncertain append contains an opaque adapter-owned
:append/recovery candidate and error information, never a success result.
Queries return declared projection values without appending an event.

## Capture ingress and immutable artifacts

A JVM edge adapter accepts staged source files from synthetic generators, DAW
exports or future device adapters. It does not open recording devices on startup.
The command contains paths and expected hashes, never audio bytes in a ledger.
Paths are host-local ingress references, not canonical source identity.

Before opening/copying any input the service validates the active arm grant,
command shape, request identity and session revision. Both unarmed and after-stop
ingest return `:capture/not-armed` and leave session state, ledger bytes,
existing media bytes, canonical source inventory and hashes unchanged. The rejected
input is never published as a source.

On valid input, verify exact raw bytes and WAV/SMF metadata, then publish immutable
content-addressed copies with forced file/directory durability before referencing
them from an event. Never replace an existing digest path with different bytes.
A take has separate source objects: WAV is an immutable audio render; SMF MIDI is
a linked MIDI source, never mislabeled as an audio render. A phrase/clip spans
one audio render and links the corresponding MIDI range and original take.

The normalized take result is plain Clojure data:

```clojure
{:take/id "uuid"
 :take/session-id "uuid"
 :take/audio {:render/id "uuid" :sha256 "64 lowercase hex"
              :format :wav :sample-rate 48000 :channels 2 :frames 192000}
 :take/midi {:source/id "uuid" :sha256 "64 lowercase hex" :format :smf}
 :take/clock {:sample-rate 48000 :ppq 480 :tempo-us-per-quarter 500000
              :sample-origin 0 :rounding :floor :mapping/id :ppq-samples-v1}
 :take/events [{:packet/seq 0 :device/id "synthetic-midi"
                :device/epoch 0 :raw/tick 0 :sample/offset 0
                :kind :note-on :channel 0 :pitch 60 :velocity 96}]
 :take/origin {:kind :synthetic :generator/id :studio-fixture-v1
               :seed 42 :channel-roles [:guitar-fixture :voice-fixture]}}
```

IDs and raw hashes stay stable after successful ingestion and restart. Independent
replay of the same bytes and configuration must produce identical event
interpretation, clocks and source hashes; request/session identity may differ
between independently created sessions. Generated channel names identify test
signals, not real instrument performance or measured guitar/voice fidelity.

## MIDI and clock laws

The first mapping uses constant declared tempo and PPQ. Variable-tempo files are
explicitly unsupported until a separate mapping is implemented; no guessed timing.
Use integer arithmetic:

`sample = sample-origin + floor(tick * sample-rate * tempo-us-per-quarter / (ppq * 1000000))`.

At 48000 Hz, PPQ 480, tempo 500000: tick 0 -> sample 0,
tick 240 -> 12000, tick 480 -> 24000. Preserve raw tick, device epoch,
packet sequence and normalized sample. Arrival/wall-clock times do not reorder
the source or masquerade as the sample clock. Within one epoch require
nondecreasing raw ticks and strictly increasing packet sequence; equal-tick events
retain source order. Reject invalid/overflowing configurations and out-of-range samples.

Concrete lifecycle acceptance cases:

- Note-on C4/channel 0 at tick 0, note-off at 480 yields one [0,24000) note span.
  Note-on velocity 0 is interpreted as note-off while original bytes remain intact.
- Note-on C4 on channels 0 and 1 creates independent voices. Two same-pitch
  channel-0 onsets are paired FIFO with two offs; an orphan off is rejected.
- CC64 value 127 sustains released channel notes; note-off closes the physical
  key span but the sounding span closes only when CC64 falls below 64.
  Other channels' pedal states stay independent. Preserve all raw CC messages.
- End-of-take active notes/pedals are reported as interrupted; synthetic closing
  observations are separately derived and never written into original MIDI.
- Disconnect at sample N ends that device epoch and reports active voices as
  interrupted at N. No new packet in the disconnected epoch is accepted.
  Reconnect with the same declared device creates epoch+1 and an explicit
  sample-origin; raw ticks may reset without making session samples go backward.
- A different undeclared device, missing reconnect, duplicate packet sequence,
  backward tick/sample, or packet after stop returns failure with no admitted take.
  Reconnect never silently opens a device or rearms a recovered session.

## Durable command and recovery outcomes

Use upstream Clio 9b14cda67d76639ee33aa90eff3caf10dd120d7c for generic schemas,
event identity, locking, append admission, fsync, uncertainty/retry and replay.
Calliope owns session transitions and request idempotency, not generic ledger law.

- Pre-write rejection/injected failure: ledger/projection/source inventory unchanged.
- Write or fsync failure after bytes may be visible: return explicit non-success,
  preserve Clio's exact append-recovery candidate, and do not advance the successful
  application projection. Never promise rollback of bytes the filesystem may have
  written. Validate/reflush and retry the exact candidate through Clio.
- Retrying a successful request/id with identical content yields one durable command
  and one projected result. Same request/id with different content is a conflict.
- Two writers based on the same session stream revision yield one admitted slot
  and one explicit conflict/reload outcome; they do not silently merge intent.
  Both JVMs canonicalize the resulting valid history to the same event identities,
  causal order and source hashes. OS scheduling is not claimed deterministic.
- Missing/stale sources, corrupt/partial history or unsupported schema never become
  an empty successful session. Return explicit non-success and preserve originals.
- Recovery retains committed history and unfinished proposal requests. Capture
  always starts unarmed; a prior arm is historical evidence, never a new grant.
  Interrupted in-process provider work is reported interrupted/unknown, not durable
  execution or automatic resubmission.

The SQLite read model is disposable and built from admitted events. Selected query
columns and row order are declared by each query. Losing the index cannot lose
source identity, clip UUID/range, annotations, proposal identity or decisions.

## Human decisions and provider separation

Accept and reject are tested independently. Each decision persists proposal UUID,
trusted human actor ID, decision value, affected clip/source identity and causal
source event. Reading/viewing a proposal, model completion and replay create zero
decision events. An unknown proposal cannot be accepted or rejected.

A real model invocation records provider/model and prompt/version or digest, input
evidence and actual completion/failure. Synthetic proposal fixtures are labeled
fixtures. Requests run on a bounded worker outside capture and the FX thread;
a stalled, unavailable or failed provider does not hold capture/monitoring locks.
This is inspectable local asynchronous work, not a claim of durable Sol/Knoxx
execution. Actual durable agent admission/recovery remains a later integration check.

## Native workflow and desktop adapter

Native views invoke the application boundary only. At 640x480 and 960x720:

1. Startup shows UNARMED and disables ingest/mark.
2. User arms; ARMED is visible and ingest becomes available.
3. User ingests the synthetic take; source hashes/duration are visible.
4. Mark creates a source-linked range; stop shows STOPPED and disables ingest.
5. Replay advances JavaFX transport over the selected phrase; selection/annotation
   preserve source bytes and produce the declared clip UUID/range/annotation.
6. Request shows pending, then provisional or explicit failed/unavailable state.
7. Viewing leaves decision count unchanged; accept/reject creates one attributed
   decision for the selected proposal and is reflected after command success.
8. Closing/reopening recovers the same phrase UUID, source/derivative hashes,
   event identities, annotation, selected range and attributed decision records;
   capture is UNARMED.

All actions have keyboard focus/activation and nonempty accessible names.
Tab order reaches every enabled control; Space/Enter activates the focused action.
Text/range fields are editable by keyboard. At both sizes the transport and capture
state are visible; overflow areas scroll so every action can be reached without
resizing. A narrower window retains scrolling and never auto-writes a new layout.

The i3 adapter is one-shot, explicit and inspectable. Plans use user-selected
container IDs from a fresh tree; command text is generated from fixed operations
and validated integer IDs/sizes, never window titles or caller command strings.
Only owned/explicitly selected containers are targeted. The profile retains other
creative applications; missing apps are reported without changing them.

Mock tests assert allowlisted operations execute once, unexpected commands execute
zero times, focus ID stays unchanged, undersized/opted-out windows are skipped,
stale tree or manual geometry change cancels apply with zero commands, and reconnect
requires a fresh tree/plan and reports its result. No background reconciliation
reasserts the profile. Actual workstation layout effects remain separate evidence.
