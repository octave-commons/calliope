# Reconstruction integration scope

Status: active integration note  
Date: 2026-07-28

## Purpose

Reconcile the audio-reconstruction lineage that landed on
`design/classifier-dsl-v1` after PR #1 had already merged, without flattening its
commit provenance into the media-workbench design branch.

## Integration boundary

Included:

- reconstruction laws, programs, contracts, documentation, and committed evidence;
- `references/heresy-between/` non-render evidence and render hash manifest;
- reconstruction ledger and path-translation rules;
- one Rheos story connecting reconstruction evidence to the media workbench;
- append-only union of both branch receipt histories.

Excluded:

- committed `.cpcache/` products;
- `.ημ/session-mycology/` run-local artifacts;
- the untracked 4.6G local render tree;
- any claim that local reconstruction tools or model endpoints were exercised by
  this remote integration.

## Merge policy

The integration uses an explicit merge commit with the media-workbench head as
first parent and the reconstruction lineage as second parent. Shared authority
files are reconciled deliberately rather than selected wholesale:

- preserve the accepted media-workbench `AGENTS.md` and add reconstruction facts;
- preserve and append-union both `receipts.edn` histories;
- accept reconstruction dependency/runtime additions;
- fail on any unexpected textual conflict.

The stacked PR targets `design/media-workbench-v1` so PR #3 remains readable and
its design review does not silently absorb hundreds of evidence files.
