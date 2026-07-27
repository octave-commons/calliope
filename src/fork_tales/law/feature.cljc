(ns fork-tales.law.feature
  "Malli contracts for reusable feature definitions and extraction programs.

  Features are typed observations about an object or a bounded part of one.
  Extractors produce feature observations with independent provenance and cache
  policy. Classifiers may consume cached features without re-running the
  extractor that produced them.")

(def schemas
  {"feature/family-v1"
   [:enum :structure :section :style :production :language :rhetoric
    :theme-signal :motif :entity :emotion :audio :visual :metadata]

   "feature/aggregation-v1"
   [:enum :none :any :all :ordered :majority :max-confidence]

   "feature/definition-v1"
   [:map {:closed true}
    [:feature/id :keyword]
    [:feature/version [:ref "dsl/positive-int"]]
    [:feature/name [:ref "dsl/non-empty-string"]]
    [:feature/description [:ref "dsl/non-empty-string"]]
    [:feature/family [:ref "feature/family-v1"]]
    [:feature/scopes [:set {:min 1} [:ref "object/type-v1"]]]
    ;; The exact value contract for one extracted feature observation.
    [:feature/value-schema :any]
    [:feature/aggregation [:ref "feature/aggregation-v1"]]]

   "feature/status-policy-v1"
   [:enum :observed-only :accepted-only :derived-or-better
    :provisional-or-better :latest-any]

   "feature/missing-policy-v1"
   [:enum :omit :null :fail :extract]

   "context/step-attach-features-v1"
   [:map {:closed true}
    [:step/op [:= :attach-features]]
    [:step/input :keyword]
    [:step/features [:set {:min 1} :keyword]]
    [:step/status-policy [:ref "feature/status-policy-v1"]]
    [:step/missing [:ref "feature/missing-policy-v1"]]
    [:step/as :keyword]]

   "extractor/cache-key-part-v1"
   [:enum :object-content-sha256 :extractor-version
    :model-digest :prompt-version :context-version]

   "extractor/cache-v1"
   [:map {:closed true}
    [:cache/reuse [:enum :exact-only :never]]
    [:cache/key
     [:set {:min 1} [:ref "extractor/cache-key-part-v1"]]]]

   "extractor/common-v1"
   [:map {:closed true}
    [:extractor/id :keyword]
    [:extractor/version [:ref "dsl/positive-int"]]
    [:extractor/description [:ref "dsl/non-empty-string"]]
    [:extractor/input-object-types
     [:set {:min 1} [:ref "object/type-v1"]]]
    [:extractor/modalities
     [:set {:min 1} [:ref "model/modality-v1"]]]
    [:extractor/produces [:set {:min 1} :keyword]]
    [:extractor/output :keyword]
    [:extractor/cache [:ref "extractor/cache-v1"]]
    [:extractor/emits [:ref "classifier/emission-v1"]]]

   "extractor/deterministic-v1"
   [:map {:closed true}
    [:extractor/id :keyword]
    [:extractor/type [:= :deterministic]]
    [:extractor/version [:ref "dsl/positive-int"]]
    [:extractor/description [:ref "dsl/non-empty-string"]]
    [:extractor/resolver :keyword]
    [:extractor/input-object-types
     [:set {:min 1} [:ref "object/type-v1"]]]
    [:extractor/modalities
     [:set {:min 1} [:ref "model/modality-v1"]]]
    [:extractor/produces [:set {:min 1} :keyword]]
    [:extractor/output :keyword]
    [:extractor/cache [:ref "extractor/cache-v1"]]
    [:extractor/emits [:ref "classifier/emission-v1"]]]

   "extractor/llm-v1"
   [:map {:closed true}
    [:extractor/id :keyword]
    [:extractor/type [:= :llm]]
    [:extractor/version [:ref "dsl/positive-int"]]
    [:extractor/description [:ref "dsl/non-empty-string"]]
    [:extractor/input-object-types
     [:set {:min 1} [:ref "object/type-v1"]]]
    [:extractor/modalities
     [:set {:min 1} [:ref "model/modality-v1"]]]
    [:extractor/produces [:set {:min 1} :keyword]]
    [:extractor/model :keyword]
    [:extractor/fallback-models {:optional true} [:vector :keyword]]
    [:extractor/context :keyword]
    [:extractor/prompt :keyword]
    [:extractor/output :keyword]
    [:extractor/cache [:ref "extractor/cache-v1"]]
    [:extractor/emits [:ref "classifier/emission-v1"]]
    [:extractor/runtime [:ref "classifier/runtime-v1"]]]

   "extractor/definition-v1"
   [:multi {:dispatch :extractor/type}
    [:deterministic [:ref "extractor/deterministic-v1"]]
    [:llm [:ref "extractor/llm-v1"]]]})
