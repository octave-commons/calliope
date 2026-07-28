---
status: incubating
created: 2026-07-28T01:08:27.840075100Z
source-session: octave-commons/fork_tales_v2
source-task: Prove audio_grade.py and audio_metrics.py still reproduce committed outputs
p-efficiency: 0.4
p-friction: 0.75
p-skill-candidate: 0.8
promoted-to: ""
rejected-reason: ""
---

## Problem
Two fresh runs disagreed with committed artifacts and both looked like tool regressions. Neither was. The grader was reading evidence.json whose 12-of-13 referenced paths were dead, so it silently scored those features null instead of erroring. The metrics run used a different --original file than the committed run had recorded. I also mis-invoked two CLIs by assuming flag names, and once read the FIRST ledger event when checking the LAST.

## Pattern
When a reproduction differs, the tool is the least likely cause and the loudest suspect. Inputs drift silently: stale paths inside preserved evidence, unrecorded flags, defaults that changed, or reading the wrong element of an append-only file. Tools that degrade instead of failing make this invisible.

## Candidate skill outline
- Name suggestion
- Trigger phrases
- Key steps or rules
- Anti-patterns to avoid

## Better path
Before concluding a tool changed: read --help rather than assuming flags; recover the exact inputs from the committed artifact itself (it usually records path, sha256, and settings) and feed those back; compare input hashes first and only then compare outputs; normalize known-cosmetic fields (absolute paths, ids, timestamps) before diffing so real deltas stand out; and treat a float delta near 1e-9 as agreement, not failure. Prefer tools that error on an unresolvable input over ones that score it null -- silent degradation is the actual defect worth filing.

## Receipt refs
- 2026-07-27 fork_tales_v2 receipts.edn :note evidence-paths-degrade
