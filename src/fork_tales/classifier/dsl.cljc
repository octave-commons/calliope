(ns fork-tales.classifier.dsl
  "Validation and pure-data resolution for classifier programs.

  This namespace deliberately does not call Ollama, llama.cpp, the filesystem,
  or an event ledger. It validates a program and resolves one classifier into
  an executable plan for an adapter layer to interpret."
  (:require [fork-tales.law.classifier :as law]
            [malli.core :as m]
            [malli.registry :as mr]))

(def registry
  (mr/composite-registry m/default-registry law/schemas))

(defn schema
  "Resolve a named classifier contract."
  [schema-name]
  (m/schema [:ref schema-name] {:registry registry}))

(def validator
  "Schema name -> memoized predicate."
  (memoize
   (fn [schema-name]
     (m/validator (schema schema-name)))))

(defn valid?
  "Does value satisfy a named classifier contract?"
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
        (:classifier/fallback-models classifier)))))
   (:classifiers program)))

(defn- prompt-context-issues
  [program]
  (keep
   (fn [[classifier-id classifier]]
     (let [context (get-in program [:contexts (:classifier/context classifier)])
           prompt (get-in program [:prompts (:classifier/prompt classifier)])
           output-key (:context/output-key context)
           variables (:prompt/variables prompt)]
       (when (and context prompt output-key
                  (not (contains? variables output-key)))
         (issue :prompt/context-variable-missing
                [:classifiers classifier-id]
                "Prompt variables must include the context generator output key."
                {:context-output-key output-key
                 :prompt-id (:prompt/id prompt)}))))
   (:classifiers program)))

(defn- output-schema-issues
  [program]
  (keep
   (fn [[output-id output]]
     (try
       (m/schema (:output/schema output) {:registry registry})
       nil
       (catch #?(:clj Exception :cljs :default) error
         (issue :output/invalid-schema
                [:outputs output-id :output/schema]
                "Output contract does not contain a compilable Malli schema."
                {:error #?(:clj (.getMessage ^Exception error)
                           :cljs (.-message error))}))))
   (:outputs program)))

(defn lint
  "Return semantic issues not expressible as local Malli shape constraints.

  An empty vector means all registry keys and cross-references are coherent.
  Call `valid?` as well: lint assumes the top-level data is close enough to the
  schema for keys to be inspected."
  [program]
  (vec
   (concat
    (keyed-id-issues program :sources :source/id)
    (keyed-id-issues program :models :model/id)
    (keyed-id-issues program :selectors :selector/id)
    (keyed-id-issues program :contexts :context/id)
    (keyed-id-issues program :prompts :prompt/id)
    (keyed-id-issues program :outputs :output/id)
    (keyed-id-issues program :classifiers :classifier/id)
    (selector-reference-issues program)
    (context-reference-issues program)
    (classifier-reference-issues program)
    (prompt-context-issues program)
    (output-schema-issues program))))

(defn runnable?
  "True when the program satisfies its schema and has no semantic lint issues."
  [program]
  (and (valid? program)
       (empty? (lint program))))

(defn compile-plan
  "Resolve one classifier definition into a pure execution plan.

  Adapters consume the returned map to perform selection, hydration, prompt
  interpolation, model invocation, result parsing, output validation, and
  append-only event emission."
  [program classifier-id]
  (let [schema-explanation (explain program)
        issues (lint program)]
    (when schema-explanation
      (throw (ex-info "Invalid classifier program schema."
                      {:classifier/id classifier-id
                       :schema/explanation schema-explanation})))
    (when (seq issues)
      (throw (ex-info "Classifier program has unresolved semantic issues."
                      {:classifier/id classifier-id
                       :issues issues})))
    (let [classifier (get-in program [:classifiers classifier-id])]
      (when-not classifier
        (throw (ex-info "Classifier id is not defined."
                        {:classifier/id classifier-id})))
      (let [context (get-in program [:contexts (:classifier/context classifier)])
            selector (get-in program [:selectors (:context/selector context)])]
        {:classifier classifier
         :model (get-in program [:models (:classifier/model classifier)])
         :fallback-models
         (mapv #(get-in program [:models %])
               (:classifier/fallback-models classifier))
         :source (get-in program [:sources (:selector/source selector)])
         :selector selector
         :context context
         :prompt (get-in program [:prompts (:classifier/prompt classifier)])
         :output (get-in program [:outputs (:classifier/output classifier)])}))))
