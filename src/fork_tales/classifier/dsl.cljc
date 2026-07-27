(ns fork-tales.classifier.dsl
  "Validation and pure-data resolution for classifier programs.

  This namespace deliberately does not call Ollama, llama.cpp, the filesystem,
  or an event ledger. It validates a program and resolves classifiers and
  feature extractors into pure plans for adapter layers to interpret."
  (:require [fork-tales.law.classifier :as classifier-law]
            [fork-tales.law.feature :as feature-law]
            [malli.core :as m]
            [malli.registry :as mr]))

(def schemas
  (merge classifier-law/schemas feature-law/schemas))

(def registry
  (mr/composite-registry m/default-registry schemas))

(defn program-registry
  "Extend the DSL registry with this program's feature value contracts.

  A feature id is also a schema name inside its program. Output contracts may
  therefore use `[:ref :feature/id]` without moving discovered feature
  vocabularies into Clojure source."
  [program]
  (let [feature-schemas
        (into {}
              (keep (fn [[feature-id feature]]
                      (when-let [value-schema (:feature/value-schema feature)]
                        [feature-id value-schema])))
              (:features program))]
    (mr/composite-registry registry feature-schemas)))

(defn schema
  "Resolve a named core DSL contract."
  [schema-name]
  (m/schema [:ref schema-name] {:registry registry}))

(def validator
  "Core schema name -> memoized predicate."
  (memoize
   (fn [schema-name]
     (m/validator (schema schema-name)))))

(defn valid?
  "Does value satisfy a named core DSL contract?"
  ([value]
   (valid? "classifier/program-v1" value))
  ([schema-name value]
   ((validator schema-name) value)))

(defn explain
  "Malli explanation for invalid data, nil when valid."
  ([value]
   (explain "classifier/program-v1" value))
  ([schema-name value]
   (m/explain (schema schema-name) value)))

(defn feature-value-schema
  "Compile the value contract for one program-defined feature."
  [program feature-id]
  (m/schema [:ref feature-id] {:registry (program-registry program)}))

(defn output-schema
  "Compile one output contract, including refs to program-defined features."
  [program output-id]
  (let [schema-data (get-in program [:outputs output-id :output/schema])]
    (m/schema schema-data {:registry (program-registry program)})))

(defn- issue
  ([code path message]
   (issue code path message nil))
  ([code path message data]
   (cond-> {:issue/code code
            :issue/path path
            :issue/message message}
     data (assoc :issue/data data))))

(defn- keyed-id-issues
  [program section id-key]
  (for [[registry-id definition] (get program section {})
        :let [declared-id (get definition id-key)]
        :when (not= registry-id declared-id)]
    (issue :registry/id-mismatch
           [section registry-id id-key]
           "Registry key must equal the definition's declared id."
           {:registry-id registry-id
            :declared-id declared-id})))

(defn- missing-ref
  [program section ref-id path]
  (when-not (contains? (get program section {}) ref-id)
    (issue :reference/missing
           path
           "Referenced definition does not exist."
           {:section section :reference ref-id})))

(defn- selector-reference-issues
  [program]
  (keep
   (fn [[selector-id selector]]
     (missing-ref program
                  :sources
                  (:selector/source selector)
                  [:selectors selector-id :selector/source]))
   (:selectors program)))

(defn- context-reference-issues
  [program]
  (keep
   (fn [[context-id context]]
     (missing-ref program
                  :selectors
                  (:context/selector context)
                  [:contexts context-id :context/selector]))
   (:contexts program)))

(defn- context-dataflow-issues
  [program]
  (mapcat
   (fn [[context-id context]]
     (let [{:keys [issues bindings]}
           (reduce
            (fn [{:keys [issues bindings]} [index step]]
              (let [input (:step/input step)
                    output (:step/as step)
                    input-issue
                    (when (and input (not (contains? bindings input)))
                      (issue :context/input-unbound
                             [:contexts context-id :context/steps index :step/input]
                             "Context step input must name a prior step output."
                             {:input input :known-bindings bindings}))]
                {:issues (cond-> issues input-issue (conj input-issue))
                 :bindings (cond-> bindings output (conj output))}))
            {:issues [] :bindings #{:selected}}
            (map-indexed vector (:context/steps context)))
           output-key (:context/output-key context)
           output-issue
           (when-not (contains? bindings output-key)
             (issue :context/output-unbound
                    [:contexts context-id :context/output-key]
                    "Context output key must be produced by a context step."
                    {:output-key output-key :known-bindings bindings}))]
       (cond-> issues output-issue (conj output-issue))))
   (:contexts program)))

(defn- context-feature-reference-issues
  [program]
  (mapcat
   (fn [[context-id context]]
     (mapcat
      (fn [[index step]]
        (when (= :attach-features (:step/op step))
          (keep
           (fn [feature-id]
             (missing-ref program
                          :features
                          feature-id
                          [:contexts context-id :context/steps index
                           :step/features feature-id]))
           (:step/features step))))
      (map-indexed vector (:context/steps context))))
   (:contexts program)))

(defn- classifier-reference-issues
  [program]
  (mapcat
   (fn [[classifier-id classifier]]
     (remove
      nil?
      (concat
       [(missing-ref program :models (:classifier/model classifier)
                     [:classifiers classifier-id :classifier/model])
        (missing-ref program :contexts (:classifier/context classifier)
                     [:classifiers classifier-id :classifier/context])
        (missing-ref program :prompts (:classifier/prompt classifier)
                     [:classifiers classifier-id :classifier/prompt])
        (missing-ref program :outputs (:classifier/output classifier)
                     [:classifiers classifier-id :classifier/output])]
       (map-indexed
        (fn [index model-id]
          (missing-ref program :models model-id
                       [:classifiers classifier-id
                        :classifier/fallback-models index]))
        (:classifier/fallback-models classifier))
       (map
        (fn [feature-id]
          (missing-ref program :features feature-id
                       [:classifiers classifier-id
                        :classifier/requires-features feature-id]))
        (:classifier/requires-features classifier)))))
   (:classifiers program)))

(defn- extractor-reference-issues
  [program]
  (mapcat
   (fn [[extractor-id extractor]]
     (remove
      nil?
      (concat
       [(missing-ref program :outputs (:extractor/output extractor)
                     [:extractors extractor-id :extractor/output])]
       (map
        (fn [feature-id]
          (missing-ref program :features feature-id
                       [:extractors extractor-id :extractor/produces feature-id]))
        (:extractor/produces extractor))
       (when (= :llm (:extractor/type extractor))
         (concat
          [(missing-ref program :models (:extractor/model extractor)
                        [:extractors extractor-id :extractor/model])
           (missing-ref program :contexts (:extractor/context extractor)
                        [:extractors extractor-id :extractor/context])
           (missing-ref program :prompts (:extractor/prompt extractor)
                        [:extractors extractor-id :extractor/prompt])]
          (map-indexed
           (fn [index model-id]
             (missing-ref program :models model-id
                          [:extractors extractor-id
                           :extractor/fallback-models index]))
           (:extractor/fallback-models extractor)))))))
   (:extractors program)))

(defn- prompt-context-issue
  [program owner-path context-id prompt-id]
  (let [context (get-in program [:contexts context-id])
        prompt (get-in program [:prompts prompt-id])
        output-key (:context/output-key context)
        variables (:prompt/variables prompt)]
    (when (and context prompt output-key
               (not (contains? variables output-key)))
      (issue :prompt/context-variable-missing
             owner-path
             "Prompt variables must include the context generator output key."
             {:context-output-key output-key
              :prompt-id (:prompt/id prompt)}))))

(defn- prompt-context-issues
  [program]
  (remove
   nil?
   (concat
    (for [[classifier-id classifier] (:classifiers program)]
      (prompt-context-issue
       program
       [:classifiers classifier-id]
       (:classifier/context classifier)
       (:classifier/prompt classifier)))
    (for [[extractor-id extractor] (:extractors program)
          :when (= :llm (:extractor/type extractor))]
      (prompt-context-issue
       program
       [:extractors extractor-id]
       (:extractor/context extractor)
       (:extractor/prompt extractor))))))

(defn- schema-data-issue
  [registry* path schema-data message]
  (try
    (m/schema schema-data {:registry registry*})
    nil
    (catch #?(:clj Exception :cljs :default) error
      (issue :schema/invalid
             path
             message
             {:error #?(:clj (.getMessage ^Exception error)
                        :cljs (.-message error))}))))

(defn- output-schema-issues
  [program]
  (let [registry* (program-registry program)]
    (keep
     (fn [[output-id output]]
       (schema-data-issue
        registry*
        [:outputs output-id :output/schema]
        (:output/schema output)
        "Output contract does not contain a compilable Malli schema."))
     (:outputs program))))

(defn- feature-schema-issues
  [program]
  (let [registry* (program-registry program)]
    (keep
     (fn [[feature-id _]]
       (schema-data-issue
        registry*
        [:features feature-id :feature/value-schema]
        [:ref feature-id]
        "Feature definition does not contain a compilable Malli value schema."))
     (:features program))))

(defn lint
  "Return semantic issues not expressible as local Malli shape constraints.

  An empty vector means registry keys, cross-references, dataflow, and embedded
  Malli schemas are coherent. Call `valid?` as well: lint assumes the top-level
  data is close enough to the schema for keys to be inspected."
  [program]
  (vec
   (concat
    (keyed-id-issues program :sources :source/id)
    (keyed-id-issues program :models :model/id)
    (keyed-id-issues program :features :feature/id)
    (keyed-id-issues program :selectors :selector/id)
    (keyed-id-issues program :contexts :context/id)
    (keyed-id-issues program :prompts :prompt/id)
    (keyed-id-issues program :outputs :output/id)
    (keyed-id-issues program :extractors :extractor/id)
    (keyed-id-issues program :classifiers :classifier/id)
    (selector-reference-issues program)
    (context-reference-issues program)
    (context-dataflow-issues program)
    (context-feature-reference-issues program)
    (classifier-reference-issues program)
    (extractor-reference-issues program)
    (prompt-context-issues program)
    (output-schema-issues program)
    (feature-schema-issues program))))

(defn runnable?
  "True when the program satisfies its schema and has no semantic lint issues."
  [program]
  (and (valid? program)
       (empty? (lint program))))

(defn- assert-runnable!
  [program owner-id]
  (let [schema-explanation (explain program)
        issues (lint program)]
    (when schema-explanation
      (throw (ex-info "Invalid classifier program schema."
                      {:owner/id owner-id
                       :schema/explanation schema-explanation})))
    (when (seq issues)
      (throw (ex-info "Classifier program has unresolved semantic issues."
                      {:owner/id owner-id
                       :issues issues})))))

(defn- producer-index
  [program]
  (reduce-kv
   (fn [index extractor-id extractor]
     (reduce
      (fn [m feature-id]
        (update m feature-id (fnil conj []) extractor-id))
      index
      (:extractor/produces extractor)))
   {}
   (:extractors program)))

(defn compile-extractor-plan
  "Resolve one feature extractor into a pure execution plan."
  [program extractor-id]
  (assert-runnable! program extractor-id)
  (let [extractor (get-in program [:extractors extractor-id])]
    (when-not extractor
      (throw (ex-info "Extractor id is not defined."
                      {:extractor/id extractor-id})))
    (cond->
     {:extractor extractor
      :features
      (select-keys (:features program) (:extractor/produces extractor))
      :output (get-in program [:outputs (:extractor/output extractor)])
      :output-schema (output-schema program (:extractor/output extractor))}
      (= :llm (:extractor/type extractor))
      (assoc
       :model (get-in program [:models (:extractor/model extractor)])
       :fallback-models
       (mapv #(get-in program [:models %])
             (:extractor/fallback-models extractor))
       :context (get-in program [:contexts (:extractor/context extractor)])
       :prompt (get-in program [:prompts (:extractor/prompt extractor)])))))

(defn compile-plan
  "Resolve one classifier definition into a pure execution plan.

  Adapters consume the returned map to perform selection, hydration, feature
  attachment, prompt interpolation, model invocation, result parsing, output
  validation, and append-only event emission."
  [program classifier-id]
  (assert-runnable! program classifier-id)
  (let [classifier (get-in program [:classifiers classifier-id])]
    (when-not classifier
      (throw (ex-info "Classifier id is not defined."
                      {:classifier/id classifier-id})))
    (let [context (get-in program [:contexts (:classifier/context classifier)])
          selector (get-in program [:selectors (:context/selector context)])
          required-features (:classifier/requires-features classifier)
          producers (producer-index program)]
      {:classifier classifier
       :model (get-in program [:models (:classifier/model classifier)])
       :fallback-models
       (mapv #(get-in program [:models %])
             (:classifier/fallback-models classifier))
       :source (get-in program [:sources (:selector/source selector)])
       :selector selector
       :context context
       :prompt (get-in program [:prompts (:classifier/prompt classifier)])
       :output (get-in program [:outputs (:classifier/output classifier)])
       :output-schema (output-schema program (:classifier/output classifier))
       :features (select-keys (:features program) required-features)
       :feature-producers (select-keys producers required-features)})))
