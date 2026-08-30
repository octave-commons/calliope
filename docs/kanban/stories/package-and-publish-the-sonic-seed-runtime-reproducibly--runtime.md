---
category: "stories"
labels: "recovery, packaging, sonic-seed, reproducibility"
type: "task"
write-id: "1788049616725-0.735bp2146g0re54cwy"
points: "5"
title: "Package and publish the sonic-seed runtime reproducibly"
priority: "P2"
status: "incoming"
uuid: "calliope-issue-14-package-sonic-seed-runtime"
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

---
Review finding confirmed: this card depends on calliope-issue-13-portable-sonic-seed-skill, but Rheos at source d3937a2 does not expose dependency on create or its guarded frontmatter surface. Direct frontmatter edits are prohibited for this reconciliation, so the enforceable field is authority-blocked by open-hax/eta-mu#306. Keep this card in incoming and do not advance it until Rheos can record the dependency canonically or authorized board policy explicitly revises the prerequisite.

Review clarification: publishing the reproducible ZIP plus hashes/provenance to a durable repository release (GitHub release or equivalent) is in scope. The publication-execution non-goal excludes external media/provider/distributor publication governed by issue #10; it does not exclude repository artifact publication required by this card.
---