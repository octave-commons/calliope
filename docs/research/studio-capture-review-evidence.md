# Capture review evidence and remaining disposition

License: GPL-3.0-or-later. Recorded 2026-10-25. This distinguishes verified
defects from an API dispute; it supplies no native Bot settlement or PR approval.

## Verified defects fixed

- CodeRabbit review5643385596/root4370998517: `snapshot-sources!` is private;
  zero-payload tests rebind its private Var and the public namespace excludes it.
  Privacy maintains the declared namespace API; arbitrary code already running
  inside the JVM is not a separate security principal.
- The separate after-stop note in MiMo review5643381784 was reproduced by an
  independent local assessor. A state with both `:capture/stopped? true` and
  `:capture/armed? true` reached payload access. RED13bd8ce captures that failure;
  the active-grant check now refuses stopped state independently, before payload
  access. The old test had stopped=true and armed=false, conflating both paths.
- CodeRabbit root4370998520: existing fixtures are compared through one
  `NOFOLLOW_LINKS` channel, after checking exact expected size. Mismatches read
  zero payload bytes in the injected actual-channel regression. Existing byte
  comparison is bounded by the generated fixture size. Concurrent CREATE_NEW
  collisions produce `:capture/staging-collision` and preserve the competing file.
- CodeRabbit root4370998510: execution logs and generated outputs named in the
  earlier receipts are **local to this Codex workspace**, not tracked repository
  paths. An append-only receipt clarifies their location/availability. The
  implementation, tests, generator and descriptor test source are tracked and
  reproducible; prior receipt rows remain unchanged.

## Disputed child-attribute overload

MiMo review5643381784, native root4370995043 / thread
`PRRT_kwDOTkaDCs6wBorZ`, claims the child-path overload is nonexistent and every
authorized ingest must therefore error. It binds original head
`2f4f3606fb72d4ff5b3ae45b06bfd5047f0aa7bd`.

The [official Java SE21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/SecureDirectoryStream.html)
declares both a directory-only `getFileAttributeView(Class<V>)` method and the
child-path `getFileAttributeView(T, Class<V>, LinkOption...)` method. Local
`javap java.nio.file.SecureDirectoryStream` independently lists both signatures.
The current `leaf-check!` supplies a relative Path, BasicFileAttributeView and
typed LinkOption array to the latter method. No overload replacement was needed.

A separate read-only local agent independently checked the exact source head,
actual PR/review bindings, JDK21 API and runtime behavior. Its targeted ingress
run on that original head passed **7 tests / 55 assertions**, including repeated
successful authorized ingestion. The exact-head hosted Repository Contracts
[job121175408178](https://github.com/octave-commons/calliope/actions/runs/40195522830/job/121175408178)
also passed. The local assessor separately reproduced the stopped-state defect;
the valid concern was fixed rather than included in the API dispute.

On committed217d30d, the local full suite passed **142 tests / 1001 assertions**, zero
failures/errors, with zero-warning source/test/spike lint. Tests execute actual
authorized ingress through the disputed call. The review workflow now consumes
its pinned upstream JVM toolchain and runs the full JVM suite, lint and `javap`
as deterministic evidence gates. The exact217d30d hosted evidence artifact12601770382 was subsequently downloaded.
Its summary binds expected/executed/completion217d30d, a clean checkout, and all
four attempted gates exiting0. The deterministic output records142tests /
1001assertions, zero failures/errors, zero-warning lint, and both Java methods.
This qualifies that historical exact head; a later source head needs fresh review.

The independent local agent is not an allowlisted native GitHub Bot. Its real
assessment does not supply native rejection credit, a full review, approval or
completed-round credit. The original inline finding remains open pending a
supported native disposition or an explicit, recorded user exception. No native
Bot response is fabricated and no canonical gate pass is claimed from local
agreement alone.

## Subsequent exact-head assessment and native review

The same independent local assessor rechecked217d30d against the actual native
root and current code:11ingress tests/64assertions, zero failures/errors, clean
checkout before and after. Current native review5643547933 APPROVED217d30d and
explicitly states that hosted javap contradicts the older overload claim. The
writer's counterproposal is discussion4371067250 and summary response is
issuecomment6398542105. These are actual native sources, not Bot transcripts.
The fresh opener review is not an in-thread withdrawal or a different Bot's
rejection agreement. The original root remains open; a scoped user exception
question is pending. No answer, settlement, native gate pass or CS-003 readiness
is presumed. Exact-head findings/approvals remain attached to their real heads.

## Content-parent synchronization

After the217d30d review completed, capture incorporated content parent a3a2faa,
including its three reviewer-requested coverage cases. The combined suite now
passes145tests/1011assertions with zero failures/errors; full src/test/spike lint
has zero errors/warnings. Capture production code is unchanged by this merge.
Receipt history retains the full first-parent bytes plus the incoming append tail.
This source-head change requires fresh hosted checks and exact-head review; the
217d30d approval and assessment are not relabeled as current-head qualification.
