# Harness adapters

Fork Tales process is harness-neutral. `PROCESS.md` defines obligations;
adapters describe how a particular environment can satisfy them.

## Selection rule

Use the adapter that matches the environment actually executing the work:

| Harness | Adapter |
|---|---|
| OpenCode on Err's development machine | `opencode-global.md` plus `.opencode/AGENTS.md` |
| ChatGPT with GitHub or web connectors | `chatgpt.md` |
| Perplexity web research | `perplexity.md` |
| Unknown or different harness | Declare capabilities and follow `PROCESS.md` directly |

## Preflight

Before substantive work, establish:

1. Which repository and branch are authoritative.
2. Whether repository reads and writes are available.
3. Whether a shell and local filesystem are available.
4. Whether local Ollama or llama.cpp can actually be reached.
5. Whether `receipt_river` or another append-only EDN tool exists.
6. Which verification can be executed rather than merely recommended.

A repository file describing a tool does not prove that the current harness has
that tool. A local path in historical metadata does not prove that the current
harness is on that machine.

## Common obligations

Every adapter must:

- preserve observed, derived, provisional, and accepted distinctions;
- avoid silently turning similarity into identity;
- preserve append-only ledgers and receipts;
- record the real origin and host class of repository work;
- state tests and commands that were not executed;
- avoid secrets and unnecessary personal information;
- append a receipt after substantive repository writes.

Harness adapters may add safeguards. They may not weaken `PROCESS.md`.
