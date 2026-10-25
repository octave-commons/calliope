# Clio JVM consumer discovery

License: GPL-3.0-or-later. This is an integration probe of the upstream library,
not production studio capture or session recovery.

Run two separate JVMs from this directory, using a fresh scratch path:

```bash
clojure -M -m clio-probe append /absolute/scratch/clio-probe
clojure -M -m clio-probe recover /absolute/scratch/clio-probe
```

The first process uses Clio to publish a content-derived schema snapshot and
durably append one event. The second loads the schemas/history, verifies the same
event and source-hash metadata, and explicitly retries the exact candidate.
Clio must report already-present and leave the canonical history unchanged.
Failures throw and exit nonzero; an existing ledger is never overwritten.

Dependency: open-hax/clio
9b14cda67d76639ee33aa90eff3caf10dd120d7c, consumed as a Git dependency through
its owning deps.edn. No event-admission, filesystem-locking or replay algorithm
is copied into Calliope.

This probe carries declared hash metadata. It does not read/hash/capture media or
establish the studio's source-validation and command-idempotency semantics.
Those remain CS-002/003 work. It runs on the upstream JVM adapter's supported
Linux/POSIX filesystem and does not claim cross-platform durability.
