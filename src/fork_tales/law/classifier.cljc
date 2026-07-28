(ns fork-tales.law.classifier
  "Pure-data Malli contracts for describing LLM classifiers.

  The DSL separates source selection, context construction, prompting, model
  transport, output validation, feature extraction, and emitted ledger events.
  Runtime adapters interpret these records; definitions contain no executable
  code.")

(def schemas
  {"dsl/non-empty-string" [:string {:min 1}]
   "dsl/positive-int" [:int {:min 1}]
   "dsl/probability" [:double {:min 0.0 :max 1.0}]

   "object/type-v1"
   [:enum :work :lyric-document :production-brief :style-prompt
    :song-section :text-span :render :audio-segment :artwork
    :variant-family :project-event]

   "source/type-v1"
   [:enum :edn-projection :edn-ledger :directory :epiphany-query]

   "source/definition-v1"
   [:map {:closed true}
    [:source/id :keyword]
    [:source/type [:ref "source/type-v1"]]
    [:source/location [:ref "dsl/non-empty-string"]]
    [:source/object-type [:ref "object/type-v1"]]
    [:source/options {:optional true} :map]]

   "model/provider-v1" [:enum :ollama :llama-cpp]
   "model/modality-v1" [:enum :text :image :audio]

   "model/definition-v1"
   [:map {:closed true}
    [:model/id :keyword]
    [:model/provider [:ref "model/provider-v1"]]
    [:model/name [:ref "dsl/non-empty-string"]]
    [:model/endpoint [:ref "dsl/non-empty-string"]]
    [:model/modalities [:set {:min 1} [:ref "model/modality-v1"]]]
    [:model/options {:optional true} :map]]

   ;; Small, deliberately non-Turing-complete filter language. Runtime
   ;; interpreters may only implement these operations; no arbitrary eval.
   "filter/expression-v1"
   [:or
    [:tuple [:= :eq] :keyword :any]
    [:tuple [:= :not-eq] :keyword :any]
    [:tuple [:= :contains] :keyword :any]
    [:tuple [:= :present] :keyword]
    [:tuple [:= :not] [:ref "filter/expression-v1"]]
    [:tuple [:= :and]
     [:vector {:min 1} [:ref "filter/expression-v1"]]]
    [:tuple [:= :or]
     [:vector {:min 1} [:ref "filter/expression-v1"]]]]

   "selector/random-v1"
   [:map {:closed true}
    [:selector/id :keyword]
    [:selector/type [:= :random]]
    [:selector/source :keyword]
    [:selector/count [:ref "dsl/positive-int"]]
    [:selector/seed [:or :int [:= :run-seed]]]
    [:selector/where {:optional true} [:ref "filter/expression-v1"]]
    [:selector/distinct-by {:optional true} :keyword]]

   "selector/stratum-v1"
   [:map {:closed true}
    [:stratum/value :any]
    [:stratum/count [:ref "dsl/positive-int"]]]

   "selector/stratified-random-v1"
   [:map {:closed true}
    [:selector/id :keyword]
    [:selector/type [:= :stratified-random]]
    [:selector/source :keyword]
    [:selector/field :keyword]
    [:selector/strata
     [:vector {:min 1} [:ref "selector/stratum-v1"]]]
    [:selector/seed [:or :int [:= :run-seed]]]
    [:selector/where {:optional true} [:ref "filter/expression-v1"]]
    [:selector/distinct-by {:optional true} :keyword]]

   "selector/explicit-v1"
   [:map {:closed true}
    [:selector/id :keyword]
    [:selector/type [:= :explicit]]
    [:selector/source :keyword]
    [:selector/object-ids
     [:vector {:min 1} [:ref "dsl/non-empty-string"]]]]

   "selector/provided-v1"
   [:map {:closed true}
    [:selector/id :keyword]
    [:selector/type [:= :provided]]
    [:selector/source :keyword]
    [:selector/input-key :keyword]
    [:selector/max-count [:ref "dsl/positive-int"]]
    [:selector/where {:optional true} [:ref "filter/expression-v1"]]]

   "selector/anchor-neighbors-v1"
   [:map {:closed true}
    [:selector/id :keyword]
    [:selector/type [:= :anchor-neighbors]]
    [:selector/source :keyword]
    [:selector/anchor-id [:ref "dsl/non-empty-string"]]
    [:selector/neighbor-count [:ref "dsl/positive-int"]]
    [:selector/random-count {:optional true} [:int {:min 0}]]
    [:selector/modes
     [:set {:min 1} [:enum :lexical :semantic :metadata]]]
    [:selector/where {:optional true} [:ref "filter/expression-v1"]]]

   "selector/definition-v1"
   [:multi {:dispatch :selector/type}
    [:random [:ref "selector/random-v1"]]
    [:stratified-random [:ref "selector/stratified-random-v1"]]
    [:explicit [:ref "selector/explicit-v1"]]
    [:provided [:ref "selector/provided-v1"]]
    [:anchor-neighbors [:ref "selector/anchor-neighbors-v1"]]]

   "context/step-hydrate-v1"
   [:map {:closed true}
    [:step/op [:= :hydrate]]
    [:step/resolver :keyword]
    [:step/modalities [:set {:min 1} [:ref "model/modality-v1"]]]
    [:step/as :keyword]]

   "context/step-segment-v1"
   [:map {:closed true}
    [:step/op [:= :segment]]
    [:step/input :keyword]
    [:step/strategy
     [:enum :explicit-song-sections :fixed-text-windows :audio-sections]]
    [:step/as :keyword]]

   "context/step-limit-v1"
   [:map {:closed true}
    [:step/op [:= :limit]]
    [:step/input :keyword]
    [:step/max-chars-per-object [:ref "dsl/positive-int"]]
    [:step/overflow [:enum :truncate-tail :truncate-middle :drop-object]]
    [:step/as :keyword]]

   "context/step-render-v1"
   [:map {:closed true}
    [:step/op [:= :render]]
    [:step/input :keyword]
    [:step/template [:ref "dsl/non-empty-string"]]
    [:step/as :keyword]]

   "context/step-v1"
   [:multi {:dispatch :step/op}
    [:hydrate [:ref "context/step-hydrate-v1"]]
    [:segment [:ref "context/step-segment-v1"]]
    [:limit [:ref "context/step-limit-v1"]]
    [:attach-features [:ref "context/step-attach-features-v1"]]
    [:render [:ref "context/step-render-v1"]]]

   "context/token-budget-v1"
   [:map {:closed true}
    [:max-tokens [:ref "dsl/positive-int"]]
    [:overflow
     [:enum :drop-largest-object :drop-lowest-priority :truncate :fail]]]

   "context/definition-v1"
   [:map {:closed true}
    [:context/id :keyword]
    [:context/selector :keyword]
    [:context/steps [:vector {:min 1} [:ref "context/step-v1"]]]
    [:context/output-key :keyword]
    [:context/token-budget [:ref "context/token-budget-v1"]]]

   "prompt/message-v1"
   [:map {:closed true}
    [:message/role [:enum :system :user :assistant]]
    [:message/template [:ref "dsl/non-empty-string"]]]

   "prompt/definition-v1"
   [:map {:closed true}
    [:prompt/id :keyword]
    [:prompt/version [:ref "dsl/positive-int"]]
    [:prompt/messages [:vector {:min 1} [:ref "prompt/message-v1"]]]
    [:prompt/variables [:set :keyword]]
    ;; :inline-schema appends the exact output Malli form to the prompt;
    ;; :provider-native constrains decoding with a provider JSON Schema;
    ;; :tool-call makes the result a function call the model must emit.
    ;; The last two are translated from this program's Malli contract and
    ;; decoded back into EDN types, so the ledger stays EDN either way.
    [:prompt/output-contract
     [:enum :inline-schema :provider-native :tool-call :none]]
    [:prompt/response-format [:enum :edn :json]]]

   "output/definition-v1"
   [:map {:closed true}
    [:output/id :keyword]
    [:output/format [:enum :edn :json]]
    ;; Malli schemas are EDN data, so an output contract can carry its exact
    ;; validator without requiring runtime code generation.
    [:output/schema :any]
    [:output/on-invalid [:enum :reject :repair-once :repair-until-limit]]
    [:output/max-repair-attempts {:optional true} [:int {:min 1}]]]

   "classifier/emission-v1"
   [:map {:closed true}
    [:event/type :keyword]
    [:event/ledger [:ref "dsl/non-empty-string"]]
    [:event/status [:enum :observed :derived :provisional]]]

   "classifier/runtime-v1"
   [:map {:closed true}
    [:timeout-ms [:ref "dsl/positive-int"]]
    [:max-attempts [:ref "dsl/positive-int"]]
    [:temperature [:double {:min 0.0 :max 2.0}]]]

   "classifier/kind-v1"
   [:enum :concept-discovery :object-classification
    :relationship-proposal :production-analysis]

   "classifier/definition-v1"
   [:map {:closed true}
    [:classifier/id :keyword]
    [:classifier/version [:ref "dsl/positive-int"]]
    [:classifier/kind [:ref "classifier/kind-v1"]]
    [:classifier/description [:ref "dsl/non-empty-string"]]
    [:classifier/model :keyword]
    [:classifier/fallback-models {:optional true} [:vector :keyword]]
    [:classifier/context :keyword]
    [:classifier/prompt :keyword]
    [:classifier/output :keyword]
    [:classifier/requires-features {:optional true} [:set :keyword]]
    [:classifier/emits [:ref "classifier/emission-v1"]]
    [:classifier/runtime [:ref "classifier/runtime-v1"]]]

   "classifier/program-v1"
   [:map {:closed true}
    [:classifier-dsl/version [:= 1]]
    [:program/id :keyword]
    [:program/description {:optional true} [:ref "dsl/non-empty-string"]]
    [:sources [:map-of :keyword [:ref "source/definition-v1"]]]
    [:models [:map-of :keyword [:ref "model/definition-v1"]]]
    [:features [:map-of :keyword [:ref "feature/definition-v1"]]]
    [:selectors [:map-of :keyword [:ref "selector/definition-v1"]]]
    [:contexts [:map-of :keyword [:ref "context/definition-v1"]]]
    [:prompts [:map-of :keyword [:ref "prompt/definition-v1"]]]
    [:outputs [:map-of :keyword [:ref "output/definition-v1"]]]
    [:extractors [:map-of :keyword [:ref "extractor/definition-v1"]]]
    [:classifiers [:map-of :keyword [:ref "classifier/definition-v1"]]]]})
