# ChatGPT harness instructions

Use this adapter when ChatGPT is working through GitHub, web, file, or similar
connectors rather than running inside Err's local development environment.

## Capability boundary

Unless a tool explicitly proves otherwise, ChatGPT must assume it does not have:

- Err's local filesystem or working tree;
- a shell in the repository;
- local Ollama or llama.cpp access;
- OpenCode global plugins or Muse extensions;
- the `receipt_river` or `edn_ledger` tools;
- uncommitted local files.

GitHub connector access is repository access, not local-machine access.

## Repository workflow

1. Read `PROCESS.md`, root `AGENTS.md`, recent `receipts.edn`, and relevant
   branch files before consequential changes.
2. Use a branch and pull request for multi-file or reviewable work.
3. Fetch the current file and blob SHA before replacing it.
4. Preserve append-only files exactly; never reconstruct or reorder old events.
5. Use GitHub Actions or other visible checks as verification evidence.
6. State explicitly when local commands, models, audio analysis, or services were
   not exercised.

## Receipt River fallback

After substantive GitHub writes, append a receipt to the touched repository's
root `receipts.edn`.

Because the native tool may be absent:

1. Fetch the current branch version of `receipts.edn`.
2. Preserve all existing content and line order.
3. Append exactly one valid EDN map and a trailing newline.
4. Update the file through the GitHub contents API.

Use truthful environment fields:

```clojure
{:origin "chatgpt"
 :host "chatgpt-github-connector"
 :repo "https://github.com/octave-commons/calliope_v2"}
```

Use `:drift` to compensate for any earlier missed receipt rather than rewriting
history.

## Evidence language

- “CI passed” means the cited workflow passed for the cited commit.
- “The runtime supports Ollama” means the code path exists and relevant tests
  passed; it does not mean Err's local Ollama instance was contacted.
- “Written to the PR branch” is distinct from “merged” and “accepted.”
- Connector output and committed files are evidence; unstated local state is not.

## Web research

Use current web sources when facts may have changed. Prefer primary technical
sources, preserve source dates and limitations, and keep research findings
separate from repository authority and human acceptance.
