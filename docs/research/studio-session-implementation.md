# CS-003 session persistence implementation

License: GPL-3.0-or-later.

The production in-process service is `calliope.studio.session`. It supports fresh
store creation, session creation, explicit arm, deterministic WAV/MIDI ingestion,
stop, inspection, close/open and explicit exact-candidate retry. The trusted native
UI channel binds a configured local user; provider and system command channels
refuse capture commands. Request IDs deduplicate successful commands and conflict
when reused with different content. Commands carry the expected session revision.

Clio at `9b14cda67d76639ee33aa90eff3caf10dd120d7c` owns event envelopes,
schema revisions, admission, canonicalization, locks and durability fences. The
Calliope domain owns session/take semantics. Named Malli schemas compose the
existing capture/studio laws and Clio envelope; Malli expands utility forms before
the portable schema catalog reaches Clio. No generic ledger implementation is
introduced here.

Each fresh private store contains:

| Artifact | Role |
|---|---|
| `events.edn`, `schemas/` | Admitted history and its historical schema revisions |
| `sources/<sha256>` | Hash-checked original WAV/MIDI bytes; exclusive hard-link publication |
| `pending.edn` | Exact interrupted/uncertain candidate, persisted before source publication and event append |
| `index.sqlite` | Disposable sessions/events/sources/takes projection |
| `commands.lock` | Clio-managed application mutex covering source publication and command admission |
| `staging/` | Descriptor-confined capture import input |

Source admission is bounded to 8 MiB per raw source by capture. Recovery metadata
is bounded to 16 MiB. Source publication forces its temporary inode, publishes
without replacing a digest path and forces the directory. Originals are preserved.
The separate application mutex does not replace Clio's event-ledger lock.

Failures before persistence leave admitted history, projection and source inventory
unchanged. Once publication starts, uncertainty can leave unreferenced objects or
visible event bytes. It returns explicit non-success and retains the exact
candidate rather than claiming rollback. Open/reload exposes interrupted or
committed-unacknowledged intent; it never automatically submits it. Explicit retry
uses Clio's fence and retains the original event UUID. An arbitrary recovery-shaped
value is not permission to append a new request. Pending metadata is local command
intent, not a persistent agent execution receipt.

Opening validates historical schemas/history and source hashes before replacing
the cache. Empty existing history without interrupted creation is refused.
Interrupted initial creation can be explicitly retried. Restart always clears the
process-local capture grant; historical arm events cannot reopen capture. Missing
sources, changed source bytes, partial EDN, missing schemas/history and corrupt
pending metadata fail explicitly and preserve originals. This does not establish
tamper-proof detection of a coordinated rewrite into another internally valid
history. No independent external history witness is claimed.

## Verification

`umask 0022; clojure -M:test`: 157 tests, 1,225 assertions, zero failures/errors.
`clj-kondo --lint src test spike`: zero errors/warnings (one pre-existing informational
message). Independent JVMs compare exact event/session/source identities and
selected ordered SQLite columns after deleting/rebuilding the cache. Two writers
both recover revision 1 before a barrier releases them; one succeeds and one
conflicts, and both replay the same history. Fault injection covers refusal before
persistence, a real event write followed by a failed directory force, and an
interruption before event append; retry runs in another JVM with the exact ID.

Reproduce the smaller service demonstration in two separate JVM invocations:

```bash
mkdir -p /path/to/demo-parent
clojure -Sdeps '{:paths ["src" "resources" "spike"]}' -M -m studio-session-demo create /path/to/demo-parent/session /path/to/demo-parent/before.edn
rm /path/to/demo-parent/session/index.sqlite
clojure -Sdeps '{:paths ["src" "resources" "spike"]}' -M -m studio-session-demo recover /path/to/demo-parent/session /path/to/demo-parent/before.edn
```

The second invocation records all five equality/restart checks and recovered
history in `before.edn.recovered.edn`. The labelled operator is simulated; it is
not Err's creative acceptance. The valid ingest is refused after restart; the full suite also verifies the
refusal happens before opening payloads.

This story does not implement phrase curation, proposals/decisions or the native
studio view. CS-005, CS-004 and CS-007 own those and the combined workflow. Hardware,
real-time capture/monitoring, audio listening, durable model execution and the full
native milestone remain unverified. Existing writing/drawing/animation/Truth and
Fork Tales ownership is unchanged.
