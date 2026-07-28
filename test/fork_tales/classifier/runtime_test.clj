(ns fork-tales.classifier.runtime-test
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [fork-tales.classifier.dsl :as dsl]
            [fork-tales.classifier.runtime :as runtime]))

(def program
  (-> "classifiers/theme-discovery-v1.edn"
      io/resource
      slurp
      edn/read-string))

(deftest filter-language-is-data-only
  (let [object {:classification :suno-lyric
                :flags [:draft]
                :title "A Fork in the Static"}]
    (is (runtime/matches-filter? object [:eq :classification :suno-lyric]))
    (is (runtime/matches-filter? object [:contains :flags :draft]))
    (is (runtime/matches-filter?
         object
         [:and [[:present :title]
                [:not [:contains :flags :pasted-artifact]]]]))
    (is (false? (runtime/matches-filter?
                 object
                 [:contains :flags :pasted-artifact])))))

(deftest random-selection-is-reproducible
  (let [objects (mapv (fn [index]
                        {:object/id (str index)
                         :body-sha256 (str "hash-" index)
                         :flags []})
                      (range 20))
        selector {:selector/type :random
                  :selector/count 10
                  :selector/seed :run-seed
                  :selector/distinct-by :body-sha256
                  :selector/where [:not [:contains :flags :pasted-artifact]]}
        first-run (runtime/execute-selector objects selector {:run-seed 42})
        second-run (runtime/execute-selector objects selector {:run-seed 42})
        other-run (runtime/execute-selector objects selector {:run-seed 43})]
    (is (= first-run second-run))
    (is (not= (mapv :object/id first-run)
              (mapv :object/id other-run)))
    (is (= 10 (count first-run)))))

(deftest section-extraction-preserves-local-scope
  (let [sections
        (runtime/explicit-sections
         (str "Title: Test\n\n"
              "**[Verse 1]**\n"
              "The system forgets my name.\n\n"
              "**[Bridge - silence]**\n"
              "One checksum remains.\n"))]
    (is (= [:verse :bridge] (mapv :section/type sections)))
    (is (= [0 1] (mapv :section/ordinal sections)))
    (is (= "One checksum remains."
           (:section/text (second sections))))))

(defn- projection
  []
  (into {}
        (map (fn [index]
               [(str "song-" index)
                {:title (str "Song " index)
                 :classification :suno-lyric
                 :body-sha256 (format "%064x" index)
                 :file (str "lyrics/song-" index ".txt")
                 :flags []}]))
        (range 12)))

(def lyric
  (str "Title: Synthetic Song\n"
       "Prompt:\n"
       "Glitch choir, 84 BPM, no heroic uplift.\n\n"
       "**[Verse 1]**\n"
       "A branch becomes a promise.\n\n"
       "**[Chorus]**\n"
       "Pay the fork tax in proof.\n"))

(defn- as-json-text
  "Serialize an EDN result the way a JSON-constrained endpoint would."
  [value]
  (json/write-str (runtime/json-safe value)))

(defn- as-tool-arguments
  "Simulate the argument map Ollama hands back from a tool call."
  [value]
  (json/read-str (as-json-text value) :key-fn keyword))

(defn- shape-for
  "Render a canonical EDN result into the wire shape a contract produces."
  [contract value]
  (case contract
    :inline-schema (pr-str value)
    :provider-native (as-json-text value)
    :tool-call (as-tool-arguments value)))

(defn- production-value
  [messages]
  (let [content (:content (last messages))
        object-id (second (re-find #"Object ID: ([^\n]+)" content))]
    {:object/id object-id
     :feature/id :fork-tales/production-style-v1
     :feature/value
     {:tempo {:bpm-min 84 :bpm-max 84 :feel :straight}
      :genres ["glitch choir"]
      :moods []
      :rhythm []
      :texture []
      :vocals ["choir"]
      :structure []
      :negative-constraints ["no heroic uplift"]
      :production-notes []
      :evidence [{:source :style-prompt
                  :quote-or-description "Glitch choir, 84 BPM, no heroic uplift."}]}}))

(defn- discovery-value
  [messages]
  (let [content (:content (last messages))
        ids (->> (re-seq #"Object ID: ([^\n]+)" content)
                 (map second)
                 distinct
                 (take 2)
                 vec)]
    {:batch-summary "Two works frame branching as an obligation."
      :concepts
      [{:concept/name "branching as obligation"
        :concept/family :theme
        :concept/definition
        "Creating another path is represented as creating a responsibility."
        :concept/scope :song-section
        :concept/members
        (mapv (fn [id]
                {:object/id id
                 :section/id "1-chorus"
                 :evidence/quote "Pay the fork tax in proof."
                 :evidence/explanation
                 "The chorus states that divergence requires evidence."})
              ids)
        :concept/shared-basis
        "Both choruses make evidence the cost of branching."
        :concept/important-difference
        "The surrounding verses use different technical metaphors."
        :concept/confidence 0.9}]}))

(defn- with-contract
  [program contract]
  (update program :prompts update-vals
          #(assoc % :prompt/output-contract contract)))

(defn- run-with-contract
  "Drive a full classifier run whose fake endpoint answers in the wire shape
  the given output contract implies."
  [contract]
  (let [events (atom [])
        ids (atom 0)
        fake-runtime
        {:read-edn (fn [_] (projection))
         :read-text (fn [_] lyric)
         :read-ledger (fn [_] [])
         :append-event! (fn [path event]
                          (swap! events conj [path event]))
         :invoke-model!
         (fn [{:keys [model messages]}]
           (shape-for contract
                      (if (= "gemma4:e2b" (:model/name model))
                        (production-value messages)
                        (discovery-value messages))))
         :now (constantly "2026-07-27T00:00:00Z")
         :uuid #(str "event-" (swap! ids inc))}
        result (runtime/run-classifier!
                fake-runtime
                (with-contract program contract)
                :fork-tales/random-ten-theme-discovery-v1
                {:run-seed 3721599729})]
    {:result result :events @events}))

(deftest complete-run-extracts-caches-classifies-and-appends
  (doseq [contract [:inline-schema :provider-native :tool-call]]
    (testing (str "output contract " contract)
      (let [{:keys [result events]} (run-with-contract contract)
            appended (mapv second events)]
        (testing "ten missing production features are extracted before classification"
          (is (= 10 (count (filter #(= :feature/extracted (:event/type %))
                                   appended)))))
        (testing "the final result remains provisional and evidence-bound"
          (is (= :concept/discovered (get-in result [:event :event/type])))
          (is (= :provisional (get-in result [:event :event/status])))
          (is (= 10 (count (get-in result [:event :selection/object-ids]))))
          (is (= "branching as obligation"
                 (get-in result [:result :concepts 0 :concept/name]))))
        (testing "JSON wire shapes are decoded back into EDN types"
          (is (= :theme (get-in result [:result :concepts 0 :concept/family])))
          (is (= 0.9 (get-in result [:result :concepts 0 :concept/confidence])))
          (is (= :straight
                 (->> appended
                      (filter #(= :feature/extracted (:event/type %)))
                      first
                      :feature/value
                      :tempo
                      :feel))))
        (testing "all writes target the declared append-only ledger"
          (is (= #{"ledgers/classification.edn"}
                 (set (map first events)))))))))

(defn- json-round-trip
  "Encode then decode, so assertions test meaning rather than escaping.
  data.json writes \"/\" as \"\\/\", which is valid JSON and decodes the same."
  [value]
  (json/read-str (json/write-str value)))

(deftest json-encoding-preserves-keyword-namespaces
  (testing "clojure.data.json would otherwise drop the namespace"
    (is (= {"id" 1} (json-round-trip {:object/id 1})))
    (is (= {"object/id" 1}
           (json-round-trip (runtime/json-safe {:object/id 1})))))
  (testing "keyword values are stringified in full too"
    (is (= {"feature/id" "fork-tales/production-style-v1"}
           (runtime/json-safe {:feature/id :fork-tales/production-style-v1})))))

(deftest malli-contracts-translate-to-json-schema
  (let [schema (runtime/->json-schema
                (dsl/output-schema program
                                   :fork-tales/production-style-result-v1))]
    (testing "namespaced properties survive translation"
      (is (contains? (get schema "properties") "object/id"))
      (is (contains? (get schema "properties") "feature/value")))
    (testing "the feature id is pinned as a constant the model cannot vary"
      (is (= "fork-tales/production-style-v1"
             (get-in schema ["properties" "feature/id" "const"]))))
    (testing "the schema survives a JSON round trip with namespaces intact"
      (is (contains? (get (json-round-trip schema) "properties") "object/id")))))

(deftest json-values-decode-into-declared-edn-types
  (let [schema (dsl/output-schema program
                                  :fork-tales/production-style-result-v1)
        decoded (runtime/decode-json-value
                 schema
                 (as-tool-arguments (production-value
                                     [{:content "Object ID: song-1"}])))]
    (is (= :straight (get-in decoded [:feature/value :tempo :feel])))
    (is (= :fork-tales/production-style-v1 (:feature/id decoded)))
    (is (= :style-prompt
           (get-in decoded [:feature/value :evidence 0 :source])))))

(deftest prompt-instruction-matches-the-output-contract
  (let [output {:output/id :fork-tales/production-style-result-v1
                :output/schema [:map]}]
    (testing "inline-schema embeds the Malli form"
      (is (str/includes? (runtime/contract-instruction :inline-schema output)
                         "Output Malli schema")))
    (testing "provider-native asks for JSON, per Ollama's guidance"
      (is (str/includes? (runtime/contract-instruction :provider-native output)
                         "JSON")))
    (testing "tool-call names the function instead of demanding a literal"
      (let [instruction (runtime/contract-instruction :tool-call output)]
        (is (str/includes? instruction "emit_production_style_result_v1"))
        (is (not (str/includes? instruction "EDN")))))
    (testing ":none leaves the prompt alone"
      (is (nil? (runtime/contract-instruction :none output))))))

(deftest explicit-selector-rejects-unresolved-ids
  (let [selector {:selector/id :test/explicit
                  :selector/type :explicit
                  :selector/object-ids ["present" "missing"]}
        objects [{:object/id "present"}]
        error (try
                (runtime/execute-selector objects selector {})
                nil
                (catch clojure.lang.ExceptionInfo error error))]
    (is (some? error))
    (is (= :test/explicit (:selector/id (ex-data error))))
    (is (= "missing" (:object/id (ex-data error))))))

(deftest hydrate-consumes-the-declared-input-binding
  (let [state {:runtime
               {:resolvers
                {:test/first (fn [_ object] (assoc object :stage 1))
                 :test/second (fn [_ object] (update object :stage inc))}}}
        context {:context/steps
                 [{:step/op :hydrate
                   :step/input :selected
                   :step/resolver :test/first
                   :step/as :first}
                  {:step/op :hydrate
                   :step/input :first
                   :step/resolver :test/second
                   :step/as :second}]
                 :context/output-key :second
                 :context/token-budget {:max-tokens 100 :overflow :fail}}
        result (runtime/execute-context state context [{:object/id "song"}])]
    (is (= 2 (get-in result [:second 0 :stage])))))

(deftest missing-requested-feature-fails-at-the-extractor-boundary
  (let [state {:runtime {}
               :program program
               :producer-index
               {:fork-tales/production-style-v1
                [:fork-tales/production-style-v1]}
               :cache (atom {})
               :dry-run? false}
        context {:context/steps
                 [{:step/op :attach-features
                   :step/input :selected
                   :step/features #{:fork-tales/production-style-v1}
                   :step/status-policy :derived-or-better
                   :step/missing :extract
                   :step/as :enriched}]
                 :context/output-key :enriched
                 :context/token-budget {:max-tokens 100 :overflow :fail}}
        object {:object/id "song"
                :object/type :work
                :object/content-sha256 (apply str (repeat 64 "a"))}
        error
        (with-redefs [runtime/run-extractor!
                      (fn [_ _ _]
                        [{:feature/id :fork-tales/unrelated-feature-v1}])]
          (try
            (runtime/execute-context state context [object])
            nil
            (catch clojure.lang.ExceptionInfo error error)))]
    (is (some? error))
    (is (= :fork-tales/production-style-v1
           (:feature/id (ex-data error))))
    (is (= :fork-tales/production-style-v1
           (:extractor/id (ex-data error))))
    (is (= "song" (:object/id (ex-data error))))))
