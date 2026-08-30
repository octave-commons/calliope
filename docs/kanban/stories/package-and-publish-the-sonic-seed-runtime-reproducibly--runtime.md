---
uuid: "calliope-issue-14-package-sonic-seed-runtime"
title: "Package and publish the sonic-seed runtime reproducibly"
status: "incoming"
type: "task"
priority: "P2"
points: "5"
labels: "recovery, packaging, sonic-seed, reproducibility"
category: "stories"
write-id: "1788047389390-0.sia11foemfjg5cmutl"
created_at: "2026-08-29T23:49:49.390Z"
---

# Package and publish the sonic-seed runtime reproducibly

GitHub issue: https://github.com/octave-commons/calliope/issues/14

## Outcome

The sonic-seed runtime pack is reproducible from immutable inputs, independently
verified, and published through a durable versioned repository artifact record.

## Source evidence to preserve

Draft PR #8 head `8fde6f899f31bc222285bc0ca4c03f7c77200ca2`
contains a packaging script, Linux x86_64 runtime contract and Babashka license,
checksum-verifying launcher/cache, evidence for an external 18,794,512-byte ZIP,
and generated-media handoff conventions. Preserve useful evidence without treating
a Drive URL as durable build authority.

## Scope

- Keep reviewable source, package recipe, licenses, manifests, tests, and
  provenance in Git; keep generated session artifacts outside Git by hash.
- Verify the runtime archive, selected member, version/platform, license, hashes,
  path allowlist, staging limits, SBOM, and extracted runtime before publication.
- Normalize archive order, permissions, and timestamps for byte reproducibility.
- Make launcher cache initialization atomic and all unsupported/corrupt states fail
  before generator execution.

## Acceptance criteria

- Two clean package builds from immutable inputs yield the same ZIP hash.
- Archive paths, symlinks, members, sizes, and counts fail closed outside limits.
- Source and release-pack instructions distinguish development from bundled runtime.
- A clean extracted Linux x86_64 pack renders with network disabled.
- Unsupported platform, missing tools, corrupt xz, and hash mismatch have typed
  failures; concurrent cache setup cannot execute partial bytes.
- Runtime/generator identity changes unless byte compatibility is proved by the
  cross-runtime golden suite from issue #13.
- Generated seed artifacts are not bundled into the skill/runtime ZIP.
- Packages and manifests contain no credentials, private prompts, local absolute
  paths, or unnecessary personal data.
- A durable versioned GitHub release (or equivalent) records ZIP hash and provenance.
- A Receipt River entry pins source SHA, archive identity, two-build hashes,
  artifact URL, platform, and limitations.

## Dependency

Portable sonic-seed law, domain source, and golden tests are tracked by
https://github.com/octave-commons/calliope/issues/13. This card remains incoming
until that prerequisite is satisfied or explicitly revised through Rheos.

## Non-goals

Semantic music-law changes, corpus ingestion, provider renders, product review,
layout, release admission, and publication execution are out of scope.
