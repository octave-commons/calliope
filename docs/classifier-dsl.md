# Fork Tales classifier DSL

The classifier DSL describes model-assisted corpus work as data. A program says:

1. which corpus objects may be selected;
2. which reusable features may be extracted from those objects;
3. how objects and cached features become bounded multimodal context;
4. which prompt and model profile to use;
5. which Malli contract the response must satisfy; and
6. which append-only event the accepted result emits.

It does **not** contain executable functions and it does not depend on Ollama or
llama.cpp transport code. Runtime adapters interpret the same plan for either
provider.

## Files

- `src/fork_tales/law/classifier.cljc` — classifier, context, model, and prompt contracts.
- `src/fork_tales/law/feature.cljc` — feature definitions, extractor contracts, and cache policy.
- `src/fork_tales/classifier/dsl.cljc` — registry, semantic lint, and plan resolution.
- `resources/classifiers/theme-discovery-v1.edn` — first complete feature-plus-classifier program.
- `test/fork_tales/classifier/dsl_test.clj` — contract, reference, feature, and dataflow tests.

## Program registries

A program contains named registries:

```clojure
{:sources {...}
 :models {...}
 :features {...}
 :selectors {...}
 :contexts {...}
 :prompts {...}
 :outputs {...}
 :extractors {...}
 :classifiers {...}}
```

Registry keys must equal each definition's declared ID. References are checked
by `fork-tales.classifier.dsl/lint`; local data shape is checked by Malli.

## Objects, features, and classifications

These are distinct layers:

```text
object
  -> feature observations
  -> classifications and relationship proposals
```

An object is the thing being examined: a work, lyric document, song section,
text span, render, audio segment, artwork, production brief, or project event.

A feature is a typed observation that may be reused by many later classifiers:

```clojure
{:feature/id :fork-tales/production-style-v1
 :feature/scopes #{:work :production-brief :style-prompt}
 :feature/family :production
 :feature/value-schema [:map ...]}
```

Features remain scoped. A bridge may carry a rhetorical feature that does not
belong to the whole work. A render may audibly contain distortion even when the
production brief did not request it. Production intent and audible observation
are therefore separate feature IDs.

The example program defines feature contracts for:

- ordered song sections;
- normalized production style and negative instructions;
- section-level rhetorical devices;
- audible production traits; and
- visual motifs in artwork.

These are starting contracts, not a closed ontology. New feature definitions
remain data and carry their own Malli value schemas.

## Feature extractors

An extractor produces one or more feature observations. There are two forms.

### Deterministic extractor

```clojure
{:extractor/id :fork-tales/song-sections-v1
 :extractor/type :deterministic
 :extractor/resolver :fork-tales/explicit-song-sections-v1
 :extractor/input-object-types #{:work :lyric-document}
 :extractor/modalities #{:text}
 :extractor/produces #{:fork-tales/song-sections-v1}
 :extractor/output :fork-tales/song-sections-result-v1
 :extractor/cache {:cache/reuse :exact-only
                   :cache/key #{:object-content-sha256 :extractor-version}}}
```

The resolver is a named runtime capability, not an executable function stored
in the DSL.

### LLM extractor

```clojure
{:extractor/id :fork-tales/production-style-v1
 :extractor/type :llm
 :extractor/model :fork-tales/gemma4-e2b-ollama
 :extractor/context :fork-tales/one-work-production-context-v1
 :extractor/prompt :fork-tales/production-style-extraction-v1
 :extractor/output :fork-tales/production-style-result-v1
 :extractor/produces #{:fork-tales/production-style-v1}}
```

The E2B extractor handles a bounded, narrow task: normalize explicit style and
production language while preserving evidence, negative prompts, and unusual
phrasing. It does not decide whether the resulting song belongs to a theme.

## Feature value schemas are program data

A feature ID also acts as a schema name within its program. Output contracts may
reference it directly:

```clojure
[:map {:closed true}
 [:object/id :string]
 [:feature/id [:= :fork-tales/song-sections-v1]]
 [:feature/value [:ref :fork-tales/song-sections-v1]]]
```

`program-registry` extends the core DSL registry with each feature's
`:feature/value-schema`. This keeps corpus-specific feature vocabularies in EDN
rather than requiring a Clojure source edit for every new feature.

## Cache semantics

Feature cache keys are explicit. An exact reusable observation may depend on:

- object content hash;
- extractor version;
- model digest;
- prompt version; and
- context-generator version.

A changed lyric, extractor, model, prompt, or context policy therefore produces
a new derived observation instead of silently reusing stale features.

## Selection is not context construction

Selectors identify objects. Context generators hydrate and transform them.
This distinction allows the same reproducible sample to be examined as:

- whole lyrics;
- section-level text;
- lyrics plus production features;
- lyrics plus artwork;
- aligned lyrics and audio segments; or
- historical evidence returned by Epiphany.

The DSL supports random, stratified-random, explicit, anchor-neighbor, and
runtime-provided selectors. The last form is useful for per-object feature jobs.

## Context operations

Version 1 defines five non-executable operations:

- `:hydrate` — call a named runtime resolver for selected modalities;
- `:segment` — derive sections or fixed windows;
- `:limit` — enforce per-object bounds and name the bounded result;
- `:attach-features` — join cached features under an explicit status and missing-value policy;
- `:render` — turn structured context into a prompt variable.

Every transform names its output with `:step/as`. Semantic lint verifies that
each `:step/input` refers to an earlier binding and that the final
`:context/output-key` was actually produced.

An attachment step can declare:

```clojure
{:step/op :attach-features
 :step/input :bounded-sections
 :step/features #{:fork-tales/production-style-v1}
 :step/status-policy :derived-or-better
 :step/missing :extract
 :step/as :enriched-sections}
```

`:step/missing :extract` asks the runtime to resolve a registered producer when
a valid cache entry is absent. `:omit`, `:null`, and `:fail` support other
workflows.

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

A classifier or LLM extractor selects one primary model and optional fallback
profiles. The example uses Ollama first and an OpenAI-compatible llama.cpp
endpoint as the E4B classifier fallback.

## Output contracts

Malli schemas are EDN data, so each output definition carries its exact
response contract. Invalid model output is never appended directly. The
runtime must apply the declared policy (`:reject`, `:repair-once`, or
`:repair-until-limit`) and record each attempt in its run evidence.

`:prompt/output-contract :inline-schema` directs the adapter to append the
resolved output schema to the prompt. `:provider-native` allows a model adapter
to use native structured-output facilities. The response is still validated by
Malli afterward.

Themes and motifs are values produced by classifiers, not enums in the DSL.
Only broad concept families are constrained by the output contract. This keeps
concept discovery open while preserving a stable machine-readable envelope.

## Execution plans

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

(def style-plan
  (dsl/compile-extractor-plan
   program
   :fork-tales/production-style-v1))

(def discovery-plan
  (dsl/compile-plan
   program
   :fork-tales/random-ten-theme-discovery-v1))
```

The random-ten classifier declares
`:classifier/requires-features #{:fork-tales/production-style-v1}`. Its compiled
plan includes both the feature definition and the registered producer IDs.

An adapter executes feature and classifier work in this order:

```text
select objects
  -> resolve exact cached features
  -> run missing deterministic or LLM extractors
  -> validate feature values
  -> append :feature/extracted observations
  -> hydrate and bound classifier context
  -> attach selected feature observations
  -> render prompt variables
  -> call the selected classifier model
  -> validate classifier output
  -> append a provenance-bearing provisional event
```

Selection seed, exact object IDs, content hashes, extractor version, prompt
version, model profile and digest, cache disposition, and response validation
outcome belong in run evidence. A `:concept/discovered` event remains
provisional; similarity or repeated model agreement never promotes it to
accepted knowledge by itself.

## Verification

```bash
clojure -M:test
```

The tests validate the complete example program, closed records, registry-key
identity, cross-reference completeness, context dataflow, feature-value schema
resolution, deterministic and LLM extractor compilation, fallback resolution,
and classifier feature dependencies.
