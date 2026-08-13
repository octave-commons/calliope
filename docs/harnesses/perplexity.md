# Perplexity web harness instructions

Use this adapter when Perplexity is acting primarily as a web-research harness.

## Default authority

Perplexity should be treated as research-only unless repository read or write
access is explicitly available and demonstrated. Web access does not imply:

- access to Err's local filesystem;
- access to uncommitted repository state;
- shell, Ollama, llama.cpp, OpenCode, or eta-mu tools;
- authority to modify or accept repository artifacts.

## Research packet

A useful research result should identify:

- the question or uncertainty;
- search scope and date;
- primary sources and direct evidence;
- material disagreements or missing evidence;
- derived findings with confidence and limitations;
- proposed repository objects, concepts, or relationships;
- a disposition: adopt, test, compare, defer, or reject.

Distinguish source claims from interpretation. Prefer official documentation,
research papers, standards, and first-party project material for technical
claims. Current facts require current sources.

## Calliope use

Perplexity is well suited for:

- researching music-production terminology and historical techniques;
- comparing open-source audio and multimodal-model capabilities;
- locating primary model documentation;
- proposing candidate production vocabularies with citations;
- checking whether a supposedly novel theme or phrase has external precedent.

Research must not silently classify corpus objects. It may propose provisional
concepts or feature definitions with evidence for later evaluation.

## Repository writes and receipts

When no repository write capability exists, return a ready-to-commit research
artifact and a ready-to-append receipt, clearly marked as unpersisted.

When repository writes are available, follow `PROCESS.md`, write through a
reviewable branch, and append a truthful Receipt River record. Use
`:origin "perplexity"` and a host label that names the actual connector or web
harness. Never claim local tests, model execution, or acceptance unless those
acts occurred outside the web-research harness and their evidence is supplied.
