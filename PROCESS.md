# Fork Tales Process Charter

## Purpose

Fork Tales is a living creative corpus and an experimental system for recovering
relationships among lyrics, production language, renders, artwork, project
history, and the work that caused a song to exist.

This charter defines durable obligations for people, agents, and automation. It
is deliberately independent of OpenCode, ChatGPT, Perplexity, a local shell, a
particular model provider, or any one machine.

## Scope and non-goals

This charter governs work represented as work on this repository, including:

- corpus ingestion and projection;
- classifier and feature-extractor design;
- research and thematic synthesis;
- implementation, testing, review, and process change;
- repository writes performed through local tools or remote connectors.

It does not require one board, agent harness, LLM, operating system, or storage
adapter. Harness-specific capabilities belong in `docs/harnesses/` and may not
silently weaken this charter.

## Authority

Apply guidance in this order:

1. This `PROCESS.md` governs provenance, evidence, receipts, completion claims,
   and harness portability.
2. Accepted ADRs and explicit durable decisions govern their declared scope.
3. Data contracts in `src/fork_tales/law/` govern represented data shapes.
4. Root `AGENTS.md` describes current repository structure and engineering
   practice.
5. Harness adapters under `docs/harnesses/` and `.opencode/` describe how a
   specific environment performs the work.
6. Task instructions govern only their bounded task.

A lower-order document must not silently override a higher-order obligation.

## Epistemic tiers

Never silently promote one tier into another:

```text
observed -> derived -> provisional -> accepted
```

Examples:

- A lyric file and its content hash are observed from a canonical source.
- Parsed sections, extracted production notes, and embeddings are derived.
- A proposed shared theme or relationship is provisional.
- A human-scoped decision may accept a concept or relationship for a declared
  purpose.

Similarity is never identity. Repetition is never acceptance. Passing CI is
never human acceptance.

## Canonical and derived data

- `ledgers/ingest.edn` is the append-only ingestion source of truth.
- `docs/lyrics/` and `ledgers/projections/` are rebuildable projections.
- `tracks/` contains corpus-linked media assets and metadata; derived audio
  analysis remains reproducible and separately identified.
- `resources/classifiers/` contains pure-data programs.
- `src/fork_tales/law/` contains contracts.
- `receipts.edn` records accountable multi-step repository work.

Never hand-edit a projection to create new truth. Change a source, event,
contract, or projection process and rebuild.

## Responsible work lifecycle

Material work follows the smallest adequate version of this lifecycle:

```text
orient
  -> inspect current authority and recent receipts
  -> declare harness capabilities and limitations
  -> bound the intended change
  -> act
  -> verify proportionately
  -> append a receipt
  -> report evidence and remaining limitations
```

The lifecycle may be lightweight for trivial work. Repository writes,
architectural changes, classifier-contract changes, migrations, and completion
claims are material.

## Harness capability declaration

Before acting, determine what the current harness actually provides. At minimum,
separate these capabilities:

- repository read access;
- repository write access;
- local filesystem access;
- shell or process execution;
- local Ollama or llama.cpp access;
- web research;
- GitHub pull-request and CI access;
- Receipt River or generic EDN-ledger tools.

Do not infer a capability from repository prose. Do not represent a remote
connector as the user's local machine. Do not claim a command, model invocation,
or test ran unless the active harness executed it and preserved the result.

Use the applicable adapter in `docs/harnesses/`. When no adapter matches, state
the available capabilities and follow this charter directly.

## Receipt River protocol

### Activation

Any substantive repository write activates the receipt obligation for that
repository. Multi-repository work requires a receipt in every substantively
touched repository.

Examples of substantive work include:

- creating or changing code, schemas, process documents, prompts, or workflows;
- opening or materially updating a pull request;
- running and relying on tests or builds;
- making a durable design or process decision;
- performing multi-step research that is committed to the repository.

### Recall before consequential work

Before a major decision or multi-step continuation, inspect at least the last
three records in `receipts.edn`. Treat them as context, not unquestionable truth.
Never edit or delete prior receipt lines.

### Append after substantive work

Append exactly one EDN map per line. Every standard Receipt River record must
contain:

```clojure
{:ts "ISO-8601"
 :kind :build
 :repo "repository identity"
 :origin "actual harness"
 :owner "responsible actor or protocol"
 :dod "what this receipt establishes"
 :pi "agent/runtime identity"
 :host "actual execution host class"
 :manifest "artifact or contract reference"
 :refs "paths, commits, PRs, reports, or checks"}
```

Useful optional keys are `:note`, `:tests`, `:decisions`, and `:drift`.
Do not record secrets, credentials, private prompts, or unnecessary personal
information.

Use a known kind when applicable, including `:observation`, `:decision`,
`:test-run`, `:build`, `:drift`, `:catalog`, `:truth`, `:refutation`, or
`:adjudication`.

### Tool and fallback behavior

- When the harness exposes `receipt_river`, use it.
- When it exposes a safe append-only EDN-ledger tool, use that tool.
- When neither exists but repository writes are available, fetch the current
  `receipts.edn`, preserve every existing byte and line, append one new line,
  and update the file through the repository API.
- When no write capability exists, produce a ready-to-append receipt in the
  work artifact and explicitly state that it was not persisted.

A remote harness uses the canonical repository URL or `owner/repo` as `:repo`.
A local harness may use the resolved Git root. `:origin` and `:host` must name
the real environment, such as `opencode`/`local` or
`chatgpt`/`chatgpt-github-connector`.

### Missed receipts

When substantive work was performed without a receipt, compensate before
continuing with a `:drift` record that identifies the omitted scope and cause.
Do not rewrite history to pretend the protocol was followed at the time.

## Classifier and feature work

Classifier programs remain data. Runtime adapters may interpret declared
operations but must not introduce hidden arbitrary evaluation.

Feature observations must preserve:

- object and scope;
- extractor and schema version;
- model and prompt identity when model-derived;
- exact cache identity;
- evidence or observable basis;
- epistemic status.

A section-level feature does not automatically belong to its song. Production
intent does not automatically describe audible output. Artwork, lyrics, prompt,
and render observations remain separate until a typed relation is proposed.

Concept discovery remains provisional. Concept acceptance is explicit and
scoped.

## Research and web evidence

Research must distinguish source evidence, interpretation, and recommendation.
A web harness should prefer primary sources for technical claims and preserve
URLs, publication dates, access dates when material, and limitations.

Research output is derived evidence. It does not authorize repository changes or
promote a concept by itself.

## Verification and completion

Verification must be proportional to the claim:

- Schema changes require contract and negative tests.
- Runtime changes require tests that execute the relevant dataflow.
- Provider adapters require either live verification or an explicit statement
  that only mocked/provider-independent behavior was tested.
- Documentation changes require link, authority, and contradiction review.

A completion report must state:

- what changed;
- what evidence was produced;
- what was not executed or verified;
- the receipt reference;
- whether acceptance remains pending.

Mergeability, a green check, or an agent statement does not equal acceptance.

## Process changes and exceptions

Process friction and escaped mistakes are evidence. Change this charter or a
harness adapter explicitly; do not silently normalize repeated exceptions.

A bounded exception records the waived rule, scope, rationale, risk, owner,
expiry or review condition, and remediation. An exception does not create a
hidden precedent.

## Harness adapters

| Document | Scope |
|---|---|
| `docs/harnesses/README.md` | Adapter selection and capability rules |
| `docs/harnesses/opencode-global.md` | Machine-global OpenCode/eta-mu assumptions |
| `.opencode/AGENTS.md` | Repository-local OpenCode behavior |
| `docs/harnesses/chatgpt.md` | ChatGPT with GitHub/web connectors |
| `docs/harnesses/perplexity.md` | Perplexity web-research harness |

## License

Process and software documentation is GPL-3.0-or-later. Creative corpus material
retains the repository's CC-BY-SA-4.0 terms where applicable.
