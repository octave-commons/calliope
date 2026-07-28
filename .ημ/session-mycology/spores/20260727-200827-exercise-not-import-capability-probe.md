---
status: incubating
created: 2026-07-28T01:08:27.815256190Z
source-session: octave-commons/fork_tales_v2
source-task: Verify the librosa DSP lane of the Fork Tales reconstruction pipeline
p-efficiency: 0.35
p-friction: 0.85
p-skill-candidate: 0.85
promoted-to: ""
rejected-reason: ""
---

## Problem
I concluded the DSP lane was unavailable, then that it WAS available, then that it was available-but-broken. Three statements, two wrong. The cause: librosa 0.11 uses lazy_loader, so 'import librosa' returns a version string while any real call dies on a missing joblib. An import probe reported a capability the runtime did not have.

## Pattern
Any lazily-loaded or plugin-style module answers 'is it installed?' with yes and 'does it work?' with no. Same shape as: a CLI whose --help parses before its deps load; a service whose /health returns 200 while its dependency is down; a binary present but not executable. Availability probes that touch only the entry point systematically over-report.

## Candidate skill outline
- Name suggestion
- Trigger phrases
- Key steps or rules
- Anti-patterns to avoid

## Better path
Probe a capability by exercising the cheapest real operation, never by importing or --help. For librosa call note_to_hz; for a DSP tool run it on a tiny real input; for a service call the endpoint the work needs, not /health. When a probe and an observed failure disagree, the failure wins. Record the exercised call in the availability note so the next agent can re-run the same check instead of inventing a weaker one.

## Receipt refs
- 2026-07-27 fork_tales_v2 receipts.edn :kind :test-run all-five-scripts
