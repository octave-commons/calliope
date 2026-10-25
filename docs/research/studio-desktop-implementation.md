# One-shot music desktop profile — CS-006

License: GPL-3.0-or-later. Planning converged at foundation head `c0d39fa` before
CS-006 passed through Rheos ready. Dependency CS-001 is complete for its bounded
native-decision/probe scope. RED commit `30d4240` fails on the missing desktop
implementation namespace; production code follows that contract checkpoint.

## Application boundary

`calliope.law.desktop` declares closed, named plain-data snapshot, profile,
operation and plan shapes. `calliope.studio.desktop/plan` is pure. The music-v1
profile accepts explicitly selected studio/optional DAW container IDs and opt-outs,
not titles, regex criteria or commands. Eligible windows must be on the current
workspace, non-fullscreen and at least 640x480. Missing, opted-out, fullscreen,
other-workspace and undersized windows have explicit skip reasons. With enough
space, two windows receive half the workspace each; otherwise the studio retains
the full suitable area and the second window is reported undersized. Existing
writing/drawing/animation/Truth/Fork Tales clients remain outside the profile.

The compiler produces only integer-ID criteria with fixed `floating enable`,
`resize set` and `move absolute position` syntax from the
[official i3 command reference](https://i3wm.org/docs/userguide.html#command_criteria).
There is no `exec`, focus command, workspace replacement, caller command string,
config write or background enforcement.

`calliope.studio.i3/controller` owns the actual read/execute capabilities. Native
code supplies these capabilities; model payloads do not. `preview!` reads a fresh
snapshot and stores one immutable canonical plan behind an opaque random ID.
Only the latest preview remains active. `apply!` accepts that ID, not editable plan
data; it consumes it once. Exact snapshot equality checks both the raw-tree revision
hash and normalized geometry/focus/window identity. A changed preview executes
zero commands. Each subsequent operation checks the latest observed snapshot;
changes observed between commands stop further effects. A successful native
acknowledgement also verifies that operation's declared geometry/floating fields;
a mismatched result stops instead of applying the remaining operations.

Success verifies exact target rectangles and unchanged focus. Failure after some
commands returns a partial result; a lost effect acknowledgement preserves the
attempted operation with `:uncertain? true`, never an empty success or rollback
claim. Disconnect clears pending plans. `reconnect!` creates a fresh generation
and reads a snapshot; it never reapplies anything. A fresh preview is required.
The i3 IPC API offers no atomic compare-and-apply transaction: a change concurrent
with an individual command can produce a partial/uncertain result. This is not a
continuous controller that overrides subsequent human actions.

Native calls use the explicitly configured i3 socket, literal process arguments
and a three-second coreutils timeout (plus one-second kill grace). Neither window
titles nor model text enter executable command strings. Snapshot parsing rejects
missing/duplicate focus and duplicate window IDs instead of guessing identity.
An unavailable or malformed IPC reply is explicit non-success, not an empty
desktop. The application owns lifecycle/shutdown; this library does not stop
global Clojure executors.

## Evidence and limits

- Full JVM suite: 150 tests / 1068 assertions, zero failures/errors under umask0022.
  Tests cover fixed operation admission, selected-only effects, exact layout,
  unchanged focus/unselected apps, duplicate/old-preview refusal, independent
  revision/geometry/focus staleness, skip reasons, lost acknowledgement,
  mid-apply focus change, reconnect and native tree reduction.
- Full src/test/spike lint: zero errors/warnings after explicit probe imports.
- Read-only production discovery against the current workstation's explicit
  socket succeeds with seven windows and zero layout effects. Saved evidence is
  separately labelled `real-workstation-read-only`.
- The [isolated real i3 probe](../../spike/studio-desktop/README.md) executes six
  actual fixed commands on two disposable xmessage windows under private Xvfb,
  verifies exact rectangles and unchanged focus, and checks duplicate/stale
  application produces zero commands. This is separate from mocked effects.

No current workstation window has been moved by this development. Actual
user-invoked workstation effects, a real DAW, the integrated native studio UI and
the full capture-to-restart workflow remain separate verification. The controller
must be wired to explicit native preview/apply actions in CS-004; model completion
must not acquire that entrypoint. Story/canon exploration remains parked.
