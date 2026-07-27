# Fork Tales classifier DSL

The classifier DSL describes LLM work as data. A definition says:

1. which corpus objects may be selected;
2. how those objects become bounded multimodal context;
3. which prompt and model profile to use;
4. which Malli contract the response must satisfy; and
5. which append-only event the accepted result emits.

It does **not** contain executable functions and it does not depend on Ollama or
llama.cpp transport code. Runtime adapters interpret the same plan for either
provider.

## Files

- `src/fork_tales/law/classifier.cljc` — closed Malli contracts.
- `src/fork_tales/classifier/dsl.cljc` — registry, semantic lint, plan resolution.
- `resources/classifiers/theme-discovery-v1.edn` — first complete program.
- `test/fork_tales/classifier/dsl_test.clj` — contract and reference tests.

## Program registries

A program contains named registries:

```clojure
{:sources {...}
 :models {...}
 :selectors {...}
 :contexts {...}
 :prompts {...}
 :outputs {...}
 :classifiers {...}}
```

Registry keys must equal each definition's declared ID. References are checked
by `fork-tales.classifier.dsl/lint`; local data shape is checked by Malli.

## Selection is not context construction

Selectors identify objects. Context generators hydrate and transform them.
This distinction allows the same reproducible sample to be examined as:

- whole lyrics;
- section-level text;
- lyrics plus artwork;
- aligned lyrics and audio segments; or
- historical evidence returned by Epiphany.

The first selector is a seeded random sample of ten works. Its context generator
hydrates canonical lyrics, segments explicit song sections, bounds each work,
and renders one prompt variable named `:context/songs`.

## Context operations

Version 1 defines four non-executable operations:

- `:hydrate` — call a named runtime resolver for selected modalities;
- `:segment` — derive sections or fixed windows;
- `:limit` — enforce per-object bounds;
- `:render` — turn structured context into a prompt variable.

Runtime resolvers are named capabilities, not functions stored in classifier
data. For example, `:fork-tales/canonical-lyric-v1` can be implemented by a JVM,
Babashka, ClojureScript, or service adapter while preserving the definition.

## Filters

The selector filter language is intentionally small and non-Turing-complete:

```clojure
[:eq :classification :suno-lyric]
[:contains :flags :pasted-artifact]
[:not [:contains :flags :pasted-artifact]]
[:and [[:present :body-sha256]
       [:not-eq :classification :other]]]
```

There is no arbitrary evaluation boundary in classifier data.

## Model profiles

Model profiles carry provider-specific connection and option data:

```clojure
{:model/id :fork-tales/gemma4-e4b-ollama
 :model/provider :ollama
 :model/name "gemma4:e4b"
 :model/endpoint "http://127.0.0.1:11434"
 :model/modalities #{:text :image :audio}
 :model/options {:num_ctx 32768}}
```

A classifier selects one primary model and optional fallback profiles. The
example uses Ollama first and an OpenAI-compatible llama.cpp endpoint second.

## Output contracts

Malli schemas are EDN data, so each output definition carries its exact
response contract. Invalid model output is never appended directly. The
runtime must apply the declared policy (`:reject`, `:repair-once`, or
`:repair-until-limit`) and record each attempt in its run evidence.

Themes and motifs are values produced by classifiers, not enums in the DSL.
Only broad concept families are constrained by the output contract. This keeps
concept discovery open while preserving a stable machine-readable envelope.

## Execution plan

```clojure
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[fork-tales.classifier.dsl :as dsl])

(def program
  (-> "classifiers/theme-discovery-v1.edn"
      io/resource
      slurp
      edn/read-string))

(dsl/runnable? program)

(def plan
  (dsl/compile-plan
   program
   :fork-tales/random-ten-theme-discovery-v1))
```

An adapter executes the resolved plan in this order:

```text
select objects
  -> hydrate modalities
  -> segment and bound context
  -> render prompt variables
  -> interpolate prompt messages
  -> call the selected model adapter
  -> parse EDN or JSON
  -> validate the output Malli schema
  -> append a provenance-bearing event
```

Selection seed, exact object IDs, content hashes, prompt version, model profile,
and response validation outcome belong in the emitted run evidence. A
`:concept/discovered` event remains provisional; similarity or repeated model
agreement never promotes it to accepted knowledge by itself.

## Verification

```bash
clojure -M:test
```

The initial tests validate the example program, closed records, registry-key
identity, cross-reference completeness, prompt/context compatibility, fallback
resolution, and plan compilation.
