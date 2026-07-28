---
status: incubating
created: 2026-07-28T01:08:27.828991132Z
source-session: octave-commons/fork_tales_v2
source-task: Repair audio_agent.cljs, which could not start under nbb
p-efficiency: 0.7
p-friction: 0.8
p-skill-candidate: 0.9
promoted-to: ""
rejected-reason: ""
---

## Problem
A historical secret purge had replaced the plain literals 'node' and 'root' with REDACTED_SECRET, producing "REDACTED_SECRET:fs" requires and artifact-REDACTED_SECRET bindings. The canonical runner was dead on require. Worse, the same placeholder stood for several different original words, so no single substitution could be inferred from the placeholder alone.

## Pattern
Secret scrubbers match substrings, not identifiers, so ordinary words that happen to appear in a credential get replaced everywhere -- in source, schemas, lyrics, and ledgers. Because one placeholder can cover several distinct originals, guessing from context is unsound even when one case looks obvious.

## Candidate skill outline
- Name suggestion
- Trigger phrases
- Key steps or rules
- Anti-patterns to avoid

## Better path
Never guess a scrubbed literal. Find a pre-purge blob with 'git log --all --oneline -S<token> -- <path>' or by walking fork-tax snapshot commits, extract it, and diff it against the current file to scope the damage before editing. The diff also proves scrub damage is the ONLY delta, which a targeted sed cannot. Restore per-file from the blob, then re-apply intentional changes on top. Grep the whole repo for the placeholder afterwards and classify each hit: live code gets repaired, append-only ledgers and derived corpus projections are left alone.

## Receipt refs
- 2026-07-27 fork_tales_v2 receipts.edn :drift secret-purge; commit 43024c7
