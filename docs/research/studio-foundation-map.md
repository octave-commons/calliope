# Studio foundation: current paths and first complete workflow

Date: 2026-10-25. License: GPL-3.0-or-later.
Status: observed repository map and proposed implementation sequence.

## Assignment and evidence

The [implementation handoff](https://docs.google.com/document/d/1x9v4v6_lxl3iu41mT06XhqX656Q4pqYgsNQtWA926AE/edit)
sets the immediate outcome: explicit arm, deterministic audio/MIDI, phrase mark,
stop, replay/select/annotate, provisional suggestion, separate human decision,
restart and source-aware recovery. Story/canon exploration is parked.
Read in order: [START_HERE](https://docs.google.com/document/d/17eUwQbFXl9AnfOLTSQDAyyabPbwbf-X9L7Q0F9U4Fxk/edit),
[vision](https://docs.google.com/document/d/1aR-yljt-wAZogXEXojts17AX-HN1YrJiemi_cMr-Bg0/edit),
[roadmap](https://docs.google.com/document/d/1KqhV6PBrZO0_loTBtteZYon2OZzg-UXDX6PatKqhZNw/edit),
[fixture kit](https://drive.google.com/file/d/1TCJfK3vH_3sHrNg4Vvf-xXGWM4CHeH8i/view),
[workflow plan](https://drive.google.com/file/d/19vFSfgKlOzU5z_pCe9DnHst-y0VnMUEI/view),
[sources](https://docs.google.com/document/d/1bmKW162FG-dNt3EnLIceBjCpowdXUdPijcCvk3u0XDI/edit).
The kit's six isolated tests pass locally; this supplies no product evidence.

## Pinned repository evidence

| Owner | Inspected head | Existing path and consequence |
| --- | --- | --- |
| Calliope main | a731093b878f600ee30648a01f1a6cd44f3439f0 | ADR-001, approved media-workbench-v1, media dataset/hash verification; no current production capture, playback, studio session or native shell namespace |
| Calliope original checkout | b473c21251d3281eae942c5bddfef829b37d1182 | device/stealth; preserved, clean; new work uses isolated codex/studio-foundation worktree |
| Calliope PR #7 | 194512b4ce7e9e706f555a4f933449c632bb8fb5 | Historical accepted native-runtime ADR, studio Malli laws and disposable spikes; closed, unmerged; never union-merge its stale board ledger |
| Clio upstream main | 9b14cda67d76639ee33aa90eff3caf10dd120d7c | src/clio/infra/runtime.cljc, ledger.cljc and extern/jvm/fs.clj already own schema snapshots, locking, durability and replay; consume this pinned upstream JVM boundary rather than copy laws/transport |
| Sol | 1276955c86ff46936cd1ce7d81fbc790f7068e1e | infra/agent/session_store.cljs and run_state.cljs expose persisted runs; no actual durable studio run was admitted or recovered |
| Muse | b4bdb0a7d019bb33c71aba1bd8daec5933e7ebde | Compatibility compiler; no persistent studio actor ownership inferred |
| Epiphany | ca3fd843b30ef8fd9ca2881aeb9758e58dac6b66 | Evidence/history owner; future source-reference integration, no new shared ontology in this increment |
| Truth | c7cc34a8248a615f39eeefd5ba8ac841b65e49fa | Read-only observation integration reserved; simulation and canon untouched |
| i3 devops | 7d3e609adef6241a8e12788dca80669fe911eebe | Existing config/lint tooling; no new WM config replacement |

Related heads identify inspection targets, not live integration or exhaustive audits.
Calliope governs work/render/clip/curation. Clio governs generic event law.
Sol/Knoxx may run future bounded jobs; dispatch is never durable completion.
Writing, GIMP, Blender, simulation and Fork Tales stay independent applications/assets.

## Existing paths to milestone

| Workflow step | Current authority/path | Implementation seam |
| --- | --- | --- |
| Layout | Existing i3 tools; approved native design honors desktop geometry | Explicit one-shot profile plan/apply; no background reconciliation or focus stealing |
| Arm / stop | Handoff requires visible deliberate consent | Local application command, no device opened by startup or recovery |
| Audio/MIDI | No production capture service on main | JVM synthetic adapter through versioned capture command; preserve raw WAV/MIDI and declared clocks |
| Mark / clip | Approved design: marker is annotation; clip spans one immutable render | Recover compatible studio laws, add bounded session commands; hashes verified before curation/play |
| Replay | Historical accepted JavaFX Media choice in PR #7; issue #12 tracks recovery | Re-run transport/window spike and reconcile decision provenance before downstream assumptions |
| Select / annotate | Approved scoped judgments; no production implementation | Append durable commands; replay projection; missing/stale sources fail explicitly |
| AI review | Classifier runtime emits provisional concepts | Explicit bounded proposal adapter; separate attributed human decision; no model in capture/monitor path |
| Restart | Proposed studio ledger; existing source/projection split | Locked, synced EDN session events + immutable media; SQLite disposable read model; recovery starts unarmed |

## Recovery and architecture reconciliation

PR #7's ADR-002 records Err's 2026-08-25 acceptance of cljfx 1.9.5,
JavaFX 21.0.3/Media, SQLite via JDBC and an in-process command/query boundary.
Main now uses ADR-002 for the external dataset. Preserve the historic source
byte-for-byte under research/recovered and refer to its immutable source SHA;
use a uniquely named reconciliation note rather than overwrite the current ADR.
The [current native-runtime address](../adrs/adr-003-reconciled-native-runtime.md)
preserves the recorded 2026-08-25 acceptance, unchanged choices and current
evidence limits. It does not claim new acceptance of the studio implementation.
Issue [#12](https://github.com/octave-commons/calliope/issues/12) requires fresh
window/audio/read-model evidence and Rheos reconciliation. Historic acceptance
does not prove today's playback. The approved design defers full DAW/MIDI editing;
this assignment adds capture/import adapters and source-linked phrases, not a DAW.

## Verification boundaries

- Host shell, repository read/write, GitHub and native dependencies are available.
- PipeWire/PulseAudio is reachable. No physical input was opened.
- Default i3 socket lookup failed; explicit discovery found the live socket.
  Read-only GET_VERSION and GET_TREE verified i3 4.23 and six windows.
  No layout commands or configuration changes were applied.
- Use a private Xvfb display and controlled playback sink for native automation.
  Such evidence proves the app path but not human listening or real workstation layout.
- Real guitar/mic/interface/MIDI, direct monitoring, hardware disconnects, latency
  and durable remote agents remain separate checks.
- Original media and derived clip identity survive unchanged through restart.
  Corrupt/truncated ledger, failed writes, stale bytes and missing sources must fail.
- Local disk has about 15 GiB free; fixtures remain small, no model downloads.

## Fresh native discovery

The disposable [native probe](../../spike/studio-runtime/README.md) executed on
Linux/OpenJDK 21.0.12.1/JavaFX 21.0.3 under Xvfb and an isolated PulseAudio null
sink. It asserted actual MP3 duration (187824 ms), play progress, paused position,
seek, resumed progress and stopped state, opened a cljfx window through a
command/query boundary, then deleted/rebuilt a toy SQLite index from EDN.
This is current runtime discovery, not the complete studio loop or hardware output.
The probe and [recovered ADR](recovered/pr-7-native-runtime-adr.md) are research
artifacts; production implementation still follows planning review and Rheos readiness.

Baseline main tests: 112 tests, 699 assertions, zero failures/errors with umask
0022. Under the shell's umask 0002 the existing dataset permission test has two
failures (expected group-write permission versus the replacement file's 0644).
Hosted Repository Contracts passes. Studio work has not changed that behavior.

## Delivery sequence

1. CS-001: reconcile native-runtime source and run fresh disposable evidence.
2. CS-002: [capture-specific boundary](../designs/studio-session-boundary.md),
   portable laws, deterministic synthetic adapters and lifecycle negatives.
3. CS-003: durable session commands, source verification and history recovery.
4. CS-005: phrase curation, provisional proposals and separate human decisions.
5. CS-004: native controls/playback and keyboard/size acceptance.
6. CS-006: one-shot desktop profile and independent i3 mock harness
   (depends on CS-001; may progress independently of session persistence).
7. CS-007: complete native journey and independent-process restart demonstration.

Planning review precedes production implementation; every story moves only through
Rheos. Existing FT cards retain their identity and acceptance gates. This increment
does not assert the entire daily-driver player, all FT gates, or real-device readiness.
CS-001 has an explicit FT-000D dependency. The existing owner decisions must be
reconciled through their actual lifecycle; prose or a standalone probe is not a
replacement readiness gate.
