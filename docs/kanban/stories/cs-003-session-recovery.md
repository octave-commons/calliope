---
labels: "studio, foundation, native, recovery"
dependency: ["cs-002-capture-laws"]
adr: "docs/adrs/adr-001-local-first-media-workbench.md"
write-id: "1792946060748-0.jv95vh2wkw989svu6sa"
points: "3"
title: "CS-003: Persist immutable sources and recover session history"
priority: "P0"
status: "review"
design: "docs/designs/media-workbench-v1.md"
uuid: "cs-003-session-recovery"
research: "docs/research/studio-foundation-map.md"
---

# CS-003: Persist immutable sources and recover session history

## Context

The 2026-10-25 studio handoff requests the first complete synthetic music session.
See [current path map](../../research/studio-foundation-map.md) and its source links.

## Outcome

Persist immutable sources and recover session history within Calliope's existing
native media ownership. Phrase curation/proposals/decisions move to CS-005.

## Scope

Add in-process session commands/queries, immutable source storage and recoverable
history using upstream Clio's JVM adapters. Follow the durable outcomes in the
[studio boundary](../../designs/studio-session-boundary.md); do not copy generic
ledger transport or identity law. SQLite remains disposable; admitted history
and immutable sources are the recovery authority. This three-point story is
bounded to source/session persistence; CS-005 owns phrase and decision semantics.

## Non-goals

A new DAW, browser client, changed simulation/canon, publication, hardware purchasing,
or a new persistent agent runtime. No declaration that the full FT milestone is done.

## Acceptance criteria

- Two independent JVMs recover identical session/event IDs, causal order, source
  identities and hashes. Delete/rebuild the read model; selected query columns
  and row values/order remain equal.
- A pre-write rejection/injected failure leaves ledger/projection/source inventory
  unchanged. A post-write/fsync failure returns explicit non-success, preserves
  Clio's exact recovery candidate and does not advance the successful projection;
  it must not claim rollback of bytes the filesystem may already expose.
- Retry the exact candidate through Clio's durability fence. A successful request
  retry produces one durable command and one projected result; changed content
  under the same request ID conflicts.
- Competing writers on one session revision yield one admitted slot and one
  explicit conflict/reload. Both JVMs replay the same resulting history; operating
  system scheduling is not presented as deterministic intent order.
- Missing/stale sources, partial/corrupt ledger and unavailable schemas report
  explicit non-success, preserve originals and never return an empty success.
- Restart never opens devices or rearms capture; interrupted local requests are
  visible as interrupted/unknown, not durable execution.
- CS-005 verifies persisted phrase/selection/annotation and separate accept/reject
  records; CS-007 binds their combined restart evidence to canonical identity.

## Verification

Write failing contract/behavior tests before production implementation, then run
focused tests, full Clojure suite, zero-warning lint and Rheos reads. Preserve
commands, hashes and execution environment in Receipt River and evidence docs.
Planning review precedes ready; code review and current-head gates precede merge.

## Risks

Historical native artifacts are not current integration. Recover sources explicitly;
keep original bytes, independent sources, decisions and failed/unavailable outcomes
inspectable. Device, display, inference and i3 availability have separate claims.

---
Planning split for PR #22 review 5642202249: this 3-point story now owns only immutable source/session persistence. CS-005 owns phrase/proposal/human-decision curation; CS-007 owns combined restart evidence. Failure acceptance distinguishes unchanged pre-write rejection from Clio post-write uncertainty and exact-candidate retry.

Foundation planning gate freshly PASS on unchangedc0d39fa, unanimous current-headMiMo/CodeRabbit and19settledthreads. This card is byte-unchanged from that reviewed plan. Canonical Rheos permitted incoming/accepted/breakdown/ready/todo/in_progress while CS002 remainsreview; starting stackedimplementation does not close its finding, mark itdone or supplymergeapproval. Prior coordinator statement that allCS003workneededCS002done was overlystrict. New namedMalli/Clio-composedcontracts compile; behavior tests nowfail onmissingCalliope session implementation. Next implementdomain then durableadapters.

CS003 local GREEN: 157 tests / 1225 assertions, zero failures/errors, full src/test/spike lint zero warnings/errors; actual Rheos review build gates passed. Independent JVMs preserve history/source identities and rebuild SQLite identically; exact interrupted/postwrite recovery candidate retry, competing writers and negative corruption/authority tests pass. Production-service two-JVM demo has five passing identity/cache/restart checks. See docs/research/studio-session-implementation.md. Simulated operator and synthetic media; no curation/proposal/native UI/device/full-milestone claim. Stacked parent capture review remains open; no merge credit.

---