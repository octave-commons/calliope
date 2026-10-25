---
id: ADR-003
title: Reconciled native runtime decision from historical ADR-002
status: accepted
accepted: "2026-08-25"
reconciled: "2026-10-25"
deciders: [Err]
historical-source: "194512b4ce7e9e706f555a4f933449c632bb8fb5"
historical-id: "ADR-002 at closed PR #7"
card: FT-000D
---

# Reconcile the accepted native runtime without changing its choices

License: GPL-3.0-or-later.

## Provenance and scope of acceptance

The immutable [historical ADR](https://github.com/octave-commons/calliope/blob/194512b4ce7e9e706f555a4f933449c632bb8fb5/docs/adrs/adr-002-native-runtime-architecture.md)
records Err's acceptance on 2026-08-25. The
[decision receipt](https://github.com/octave-commons/calliope/blob/194512b4ce7e9e706f555a4f933449c632bb8fb5/receipts.edn#L37)
records that acceptance separately from the earlier proposed spike and pending
acceptance receipts. This reconciliation carries that recorded disposition
forward; it does not claim a new human acceptance in this run.

The original document is preserved [byte-for-byte](../research/recovered/pr-7-native-runtime-adr.md),
SHA-256 a27eff747e8b0c832cc6c37d3e262748dff8a3389da14208614c40ad2120ee25.
Current ADR-002 governs the external media dataset. ADR-003 is the current
address for this recovered runtime decision, not an overwrite of either original.
No historic board events or status snapshots were imported.

## Unchanged decision

- Native Clojure/JVM client: cljfx 1.9.5 over JavaFX 21.0.3, no embedded browser.
- Playback: JavaFX Media/MediaPlayer, replaceable behind the application boundary.
- Read model: disposable SQLite via xerial sqlite-jdbc 3.46.1.3 and JDBC;
  next.jdbc 1.3.1002 remains the selected convenience adapter.
- Topology: in-process, versioned plain-Clojure commands/queries; no HTTP server
  prerequisite. Views do not write ledgers, invoke FFmpeg or call providers.
- Packaging: JVM/jpackage direction; GraalVM native-image and system media-key
  integration remain unverified, not implicit consequences of the stack choice.

The affected [approved media-workbench design](../designs/media-workbench-v1.md)
remains the current product contract. The historical
[rejected alternatives](../research/recovered/pr-7-native-runtime-adr.md#rejected-alternatives)
explain the Swing/AWT, GPU-canvas, embedded-browser, alternative audio, transport
subprocess and external/read-model tradeoffs. Its
[acceptance conditions](../research/recovered/pr-7-native-runtime-adr.md#acceptance-conditions)
require Err's disposition and reproducible native window, corpus-audio transport
and disposable-index evidence. Those sections are historical references; ADR-003
is the current decision address. The acceptance receipt above records the prior
human disposition, and current verification below states what was rerun.

ADR-001 and the approved media-workbench design still govern domain semantics.
ADR-002 dataset roots/manifests remain compatible with this read model.
Generic event storage/admission/replay belongs to Clio; SQLite is a disposable
projection and does not replace source/event authority.

## Current verification and its limits

The [reproducible native discovery](../../spike/studio-runtime/README.md) ran on
Linux/OpenJDK 21.0.12.1/JavaFX 21.0.3 under a private Xvfb display and null sink.
It opened a real cljfx stage through an in-process command/query boundary and
asserted real corpus MP3 open/duration, play/progress, seek, pause-held,
resume/progress and stop. Duration was 187824 ms; the 4389702-byte MP3 hash is
211cce48219584744f7f9b23768e55f7ca7dace4d7d52fd28b477049b86eac0e.

The disposable two-row SQLite fixture was deleted and rebuilt from durable EDN;
selected id/note columns ordered by id matched exactly. It is current evidence
for the chosen rebuild mechanism, not a production library index or studio
session recovery. The historical source separately records its larger corpus
index spike; that historic result was not silently reattributed to this run.

Physical output/human listening, pro-audio capture/direct monitoring, device
latency, MPRIS, full native studio interaction and cross-process production
recovery remain separate evidence requirements. JavaFX Media is not selected
as a hard real-time capture/monitoring engine.

## Reconciliation and downstream gate

FT-000A is reconciled through its existing human disposition, current accepted
documents and the supported Rheos lifecycle. FT-000D owns this recovered decision
and current spike evidence. CS-001 explicitly depends on FT-000D through Rheos;
no production studio card may assume this stack by bypassing that owner.

Human acceptance of the new studio implementation remains pending its own
working-code, test, native-workflow and restart/recovery evidence. This accepted
historical stack decision does not accept unimplemented product behavior.
