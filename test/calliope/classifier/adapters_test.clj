(ns calliope.classifier.adapters-test
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [calliope.classifier.dsl :as dsl]
            [calliope.classifier.runtime :as runtime]
            [malli.core :as m])
  (:import [com.sun.net.httpserver HttpHandler HttpServer]
           [java.net InetSocketAddress]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files]))

(def program
  (-> "classifiers/theme-discovery-v1.edn"
      io/resource
      slurp
      edn/read-string))

(deftest sha256-matches-known-digests
  (is (= "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
         (runtime/sha256 "")))
  (is (= (runtime/sha256 "calliope")
         (runtime/sha256 (.getBytes "calliope" StandardCharsets/UTF_8)))))

(defn- temp-dir
  []
  (str (Files/createTempDirectory
        "calliope-runtime"
        (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest default-runtime-adapters-read-and-append
  (let [base (temp-dir)
        runtime (runtime/default-runtime base)]
    (spit (io/file base "projection.edn") {:song-1 {:title "One"}})
    (spit (io/file base "lyric.txt") "Title: One\n")
    (spit (io/file base "ledger.edn") "{:event/n 1}\n\n{:event/n 2}\n")
    (testing "relative paths resolve against the base directory"
      (is (= {:song-1 {:title "One"}} ((:read-edn runtime) "projection.edn")))
      (is (= "Title: One\n" ((:read-text runtime) "lyric.txt"))))
    (testing "ledger reads skip blank lines"
      (is (= [{:event/n 1} {:event/n 2}] ((:read-ledger runtime) "ledger.edn"))))
    (testing "a missing ledger reads as empty rather than failing"
      (is (= [] ((:read-ledger runtime) "absent.edn"))))
    (testing "append creates parent directories and round-trips events"
      ((:append-event! runtime) "nested/events.edn" {:event/n 3})
      ((:append-event! runtime) "nested/events.edn" {:event/n 4})
      (is (= [{:event/n 3} {:event/n 4}]
             ((:read-ledger runtime) "nested/events.edn"))))
    (testing "absolute paths bypass the base directory"
      (is (= {:song-1 {:title "One"}}
             ((:read-edn runtime) (str (io/file base "projection.edn"))))))
    (testing "now and uuid supply provenance strings"
      (is (string? ((:now runtime))))
      (is (string? ((:uuid runtime)))))))

(deftest missing-runtime-adapter-fails-loudly
  (is (thrown? clojure.lang.ExceptionInfo
               (runtime/load-source {} {:source/type :edn-ledger
                                        :source/location "x.edn"
                                        :source/object-type :work}))))

(deftest load-source-handles-projections-ledgers-and-rejects-unknown-types
  (let [runtime {:read-edn (fn [_] {"song-1" {:title "One"}})
                 :read-ledger (fn [_] [{:event/id "e1"}])}]
    (is (= [{:object/id "song-1" :object/type :work :title "One"}]
           (runtime/load-source runtime {:source/type :edn-projection
                                         :source/location "songs.edn"
                                         :source/object-type :work})))
    (is (= [{:object/type :feature-observation :event/id "e1"}]
           (runtime/load-source runtime {:source/type :edn-ledger
                                         :source/location "features.edn"
                                         :source/object-type :feature-observation})))
    (is (thrown? clojure.lang.ExceptionInfo
                 (runtime/load-source runtime {:source/type :sqlite-table
                                               :source/id :test/source})))))

(deftest filter-language-covers-every-operator
  (let [object {:classification :suno-lyric
                :flags #{:draft}
                :meta {:provenance :ingest}
                :title "A Fork in the Static"}]
    (is (runtime/matches-filter? object nil))
    (is (runtime/matches-filter? object [:not-eq :classification :prose]))
    (is (not (runtime/matches-filter? object [:not-eq :classification :suno-lyric])))
    (testing ":contains dispatches on the value's type"
      (is (runtime/matches-filter? object [:contains :meta :provenance]))
      (is (runtime/matches-filter? object [:contains :flags :draft]))
      (is (runtime/matches-filter? object [:contains :title "Static"]))
      (is (not (runtime/matches-filter? object [:contains :flags :final])))
      (is (not (runtime/matches-filter? object [:contains :missing :anything]))))
    (is (runtime/matches-filter? object [:or [[:eq :classification :prose]
                                              [:present :title]]]))
    (is (not (runtime/matches-filter? object [:or [[:eq :classification :prose]]])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (runtime/matches-filter? object [:near :title "Static"])))))

(deftest seeded-shuffle-is-a-deterministic-permutation
  (let [values (vec (range 30))
        shuffled (runtime/seeded-shuffle values 99)]
    (is (= shuffled (runtime/seeded-shuffle values 99)))
    (is (not= shuffled (runtime/seeded-shuffle values 100)))
    (is (= (sort values) (sort shuffled)))))

(deftest stratified-selector-samples-each-stratum-reproducibly
  (let [objects (mapv (fn [index]
                        {:object/id (str "object-" index)
                         :kind (if (even? index) :even :odd)})
                      (range 20))
        selector {:selector/type :stratified-random
                  :selector/field :kind
                  :selector/strata [{:stratum/value :even :stratum/count 2}
                                    {:stratum/value :odd :stratum/count 1}]
                  :selector/seed 7}
        selection (runtime/execute-selector objects selector {})]
    (is (= selection (runtime/execute-selector objects selector {})))
    (is (= 3 (count selection)))
    (is (= 2 (count (filter #(= :even (:kind %)) selection))))))

(deftest provided-selector-consumes-run-inputs
  (let [selector {:selector/type :provided
                  :selector/input-key :batch/objects
                  :selector/max-count 2
                  :selector/where [:present :keep?]}
        inputs {:batch/objects [{:object/id "a" :keep? true}
                                {:object/id "b"}
                                {:object/id "c" :keep? true}
                                {:object/id "d" :keep? true}]}]
    (is (= ["a" "c"]
           (mapv :object/id
                 (runtime/execute-selector [] selector {:inputs inputs}))))))

(deftest unsupported-and-unknown-selectors-fail-loudly
  (is (thrown? clojure.lang.ExceptionInfo
               (runtime/execute-selector [] {:selector/type :anchor-neighbors
                                             :selector/id :test/anchor} {})))
  (is (thrown? clojure.lang.ExceptionInfo
               (runtime/execute-selector [] {:selector/type :telepathic} {}))))

(deftest hydrate-canonical-lyric-parses-headers-and-hashes-content
  (let [text (str "Title: Checksum Choir\n"
                  "Prompt:\n"
                  "Glitch choir, 84 BPM.\n\n"
                  "Lyrics:\n\n"
                  "**[Verse 1]**\n"
                  "A branch becomes a promise.\n")
        runtime {:read-text (fn [_] text)}
        hydrated (runtime/hydrate-canonical-lyric runtime {:object/id "song-1"})]
    (is (= "Checksum Choir" (:header/title hydrated)))
    (is (= "Checksum Choir" (:title hydrated)))
    (is (str/includes? (:style-and-metadata hydrated) "Glitch choir"))
    (is (not (str/includes? (:style-and-metadata hydrated) "promise")))
    (is (= (runtime/sha256 text) (:object/content-sha256 hydrated)))
    (testing "an existing body hash wins and a missing title falls back to the id"
      (let [reused (runtime/hydrate-canonical-lyric
                    runtime {:object/id "song-2" :body-sha256 "abc"})]
        (is (= "abc" (:object/content-sha256 reused)))
        (is (= "song-2" (:title (runtime/hydrate-canonical-lyric
                                 {:read-text (fn [_] "no header here")}
                                 {:object/id "song-2"}))))))))

(deftest section-parser-covers-the-full-vocabulary
  (let [labels ["Pre-Chorus" "Hook" "Refrain" "Intro" "Outro" "Interlude"
                "Instrumental Break" "Spoken Word" "Coda" "Mysterious Bit"]
        text (str (str/join "\n" (map #(str "**[" % "]**\nline\n") labels)))
        sections (runtime/explicit-sections text)]
    (is (= [:pre-chorus :hook :refrain :intro :outro :interlude
            :instrumental :spoken :coda :unknown]
           (mapv :section/type sections)))
    (testing "text without markers becomes one unsegmented section"
      (is (= [{:section/id "0-unknown" :section/type :unknown}]
             (mapv #(select-keys % [:section/id :section/type])
                   (runtime/explicit-sections "just some words")))))))

(deftest song-sections-result-carries-the-declared-feature-id
  (let [result (runtime/song-sections-result {:object/id "song-1"
                                              :lyrics "**[Verse]**\nhi\n"})]
    (is (= "song-1" (:object/id result)))
    (is (= :calliope/song-sections-v1 (:feature/id result)))
    (is (= :verse (get-in result [:feature/value 0 :section/type])))))

(deftest unregistered-resolvers-and-unknown-context-ops-fail-loudly
  (let [state {:runtime {}}]
    (is (thrown? clojure.lang.ExceptionInfo
                 (runtime/execute-context
                  state
                  {:context/steps [{:step/op :hydrate
                                    :step/input :selected
                                    :step/resolver :test/unregistered
                                    :step/as :out}]
                   :context/output-key :out
                   :context/token-budget {:max-tokens 100 :overflow :fail}}
                  [{:object/id "song"}])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (runtime/execute-context
                  state
                  {:context/steps [{:step/op :teleport
                                    :step/input :selected
                                    :step/as :out}]
                   :context/output-key :out
                   :context/token-budget {:max-tokens 100 :overflow :fail}}
                  [])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (runtime/execute-context
                  state
                  {:context/steps [{:step/op :segment
                                    :step/input :selected
                                    :step/strategy :vibes
                                    :step/as :out}]
                   :context/output-key :out
                   :context/token-budget {:max-tokens 100 :overflow :fail}}
                  [])))))

(deftest limit-step-supports-every-overflow-policy
  (let [long-lyrics (apply str (repeat 200 "x"))
        object {:object/id "song" :lyrics long-lyrics}
        run (fn [overflow]
              (runtime/execute-context
               {:runtime {}}
               {:context/steps [{:step/op :limit
                                 :step/input :selected
                                 :step/max-chars-per-object 100
                                 :step/overflow overflow
                                 :step/as :limited}]
                :context/output-key :limited
                :context/token-budget {:max-tokens 10000 :overflow :fail}}
               [object]))]
    (testing ":truncate-tail keeps the beginning"
      (is (= 100 (count (:lyrics (first (:limited (run :truncate-tail))))))))
    (testing ":truncate-middle keeps both ends with an elision marker"
      (let [truncated (:lyrics (first (:limited (run :truncate-middle))))]
        (is (str/includes? truncated "…"))
        (is (< (count truncated) (count long-lyrics)))))
    (testing ":drop-object removes the whole oversized object"
      (is (= [] (:limited (run :drop-object)))))
    (testing ":truncate-middle also bounds section text and metadata"
      (let [sectioned {:object/id "song"
                       :lyrics long-lyrics
                       :style-and-metadata long-lyrics
                       :sections [{:section/text long-lyrics}]}
            result (runtime/execute-context
                    {:runtime {}}
                    {:context/steps [{:step/op :limit
                                      :step/input :selected
                                      :step/max-chars-per-object 50
                                      :step/overflow :truncate-tail
                                      :step/as :limited}]
                     :context/output-key :limited
                     :context/token-budget {:max-tokens 10000 :overflow :fail}}
                    [sectioned])]
        (is (= 50 (count (:style-and-metadata (first (:limited result))))))
        (is (= 50 (count (get-in result [:limited 0 :sections 0 :section/text]))))))))

(deftest render-step-and-template-language-cover-all-value-shapes
  (let [object {:object/id "song-1"
                :title "Checksum Choir"
                :sections [{:section/label "Verse" :section/text "first line"}]
                :nothing nil
                :payload {:complex [:value]}}
        result (runtime/execute-context
                {:runtime {}}
                {:context/steps [{:step/op :render
                                  :step/input :selected
                                  :step/template
                                  "{{title}}|{{sections}}|{{nothing}}|{{payload}}"
                                  :step/as :rendered}]
                 :context/output-key :rendered
                 :context/token-budget {:max-tokens 10000 :overflow :fail}}
                [object])
        rendered (:rendered result)]
    (is (str/includes? rendered "Checksum Choir"))
    (is (str/includes? rendered "[Verse]\nfirst line"))
    (is (str/includes? rendered "{:complex [:value]}"))))

(deftest token-budget-fails-or-truncates-the-final-output
  (let [long-text (apply str (repeat 100 "fork "))
        run (fn [budget]
              (runtime/execute-context
               {:runtime {}}
               {:context/steps [{:step/op :render
                                 :step/input :selected
                                 :step/template "{{lyrics}}"
                                 :step/as :rendered}]
                :context/output-key :rendered
                :context/token-budget budget}
               [{:lyrics long-text}]))]
    (is (thrown? clojure.lang.ExceptionInfo
                 (run {:max-tokens 10 :overflow :fail})))
    (is (= 40 (count (:rendered (run {:max-tokens 10 :overflow :truncate})))))))

(deftest extractor-cache-key-is-deterministic-and-content-sensitive
  (let [plan (dsl/compile-extractor-plan program :calliope/song-sections-v1)
        extractor (:extractor plan)
        object {:object/id "song-1" :object/content-sha256 "aaa"}
        key (runtime/extractor-cache-key object extractor plan)]
    (is (= key (runtime/extractor-cache-key object extractor plan)))
    (is (not= key (runtime/extractor-cache-key
                   (assoc object :object/content-sha256 "bbb") extractor plan)))
    (testing "a body hash stands in when the content hash is absent"
      (is (= key (runtime/extractor-cache-key
                  {:object/id "song-1" :body-sha256 "aaa"} extractor plan))))))

(deftest parse-model-output-handles-fences-formats-and-errors
  (is (= {:ok true} (runtime/parse-model-output :edn "{:ok true}")))
  (is (= {:ok true} (runtime/parse-model-output :edn "```edn\n{:ok true}\n```")))
  (is (= {:ok true} (runtime/parse-model-output :json "{\"ok\": true}")))
  (is (thrown? clojure.lang.ExceptionInfo
               (runtime/parse-model-output :yaml "ok: true"))))

(deftest tool-names-are-stable-function-identifiers
  (is (= "emit_production_style_result_v1"
         (runtime/tool-name :calliope/production-style-result-v1))))

(defn- respond-json
  [exchange status body]
  (let [bytes (.getBytes ^String (json/write-str body) StandardCharsets/UTF_8)]
    (.sendResponseHeaders exchange status (count bytes))
    (with-open [out (.getResponseBody exchange)]
      (.write out bytes))))

(defn- with-http-server
  "Run f with [port captured-requests] for a stub JSON endpoint."
  [responder f]
  (let [captured (atom [])
        server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext
     server "/"
     (reify HttpHandler
       (handle [_ exchange]
         (let [body (slurp (.getRequestBody exchange))]
           (swap! captured conj {:uri (str (.getRequestURI exchange))
                                 :body (json/read-str body :key-fn keyword)})
           (respond-json exchange 200 (responder body))))))
    (.start server)
    (try
      (f (.getPort (.getAddress server)) captured)
      (finally (.stop server 0)))))

(def bool-schema (m/schema [:map [:ok :boolean]]))

(deftest ollama-structured-mode-posts-schema-and-returns-content
  (with-http-server
   (fn [_] {:message {:content "{\"ok\": true}"}})
   (fn [port captured]
     (let [model {:model/provider :ollama
                  :model/endpoint (str "http://127.0.0.1:" port)
                  :model/name "gemma4:e2b"
                  :model/options {:temperature 0.2}}
           text (runtime/invoke-model-http
                 {:model model
                  :messages [{:role "user" :content "hi"}]
                  :runtime-options {}
                  :output-schema bool-schema
                  :tools? false})
           request (:body (first @captured))]
       (is (= "{\"ok\": true}" text))
       (is (= "/api/chat" (:uri (first @captured))))
       (is (= "gemma4:e2b" (:model request)))
       (is (some? (:format request)))
       (is (= 0.2 (get-in request [:options :temperature])))))))

(deftest ollama-tool-mode-posts-tools-and-returns-arguments
  (with-http-server
   (fn [_] {:message {:tool_calls [{:function {:arguments {:ok true}}}]}})
   (fn [port captured]
     (let [model {:model/provider :ollama
                  :model/endpoint (str "http://127.0.0.1:" port)
                  :model/name "gemma4:e4b"}
           result (runtime/invoke-model-http
                   {:model model
                    :messages []
                    :runtime-options {:tool-name "emit_bool_result_v1"}
                    :output-schema bool-schema
                    :tools? true})
           request (:body (first @captured))]
       (is (= {:ok true} result))
       (is (nil? (:format request)))
       (is (= "emit_bool_result_v1"
              (get-in request [:tools 0 :function :name])))))))

(deftest tool-mode-without-a-tool-call-explains-the-failure
  (with-http-server
   (fn [_] {:message {:content "I would rather chat."}})
   (fn [port _]
     (let [error (try
                   (runtime/invoke-model-http
                    {:model {:model/provider :ollama
                             :model/endpoint (str "http://127.0.0.1:" port)
                             :model/name "gemma4:e4b"}
                     :messages []
                     :runtime-options {:tool-name "emit_bool_result_v1"}
                     :output-schema bool-schema
                     :tools? true})
                   nil
                   (catch clojure.lang.ExceptionInfo error error))]
       (is (some? error))
       (is (= "I would rather chat."
              (:message/content (ex-data error))))))))

(defn- llama-model
  [port]
  {:model/provider :llama-cpp
   :model/endpoint (str "http://127.0.0.1:" port "/v1/chat/completions")
   :model/name "gemma4:e4b"})

(deftest llama-cpp-structured-content-comes-from-the-first-choice
  (with-http-server
   (fn [_] {:choices [{:message {:content "{\"ok\": true}"}}]})
   (fn [port captured]
     (let [text (runtime/invoke-model-http
                 {:model (llama-model port)
                  :messages []
                  :runtime-options {}
                  :output-schema bool-schema
                  :tools? false})]
       (is (= "{\"ok\": true}" text))
       (is (= "output"
              (get-in (:body (first @captured))
                      [:response_format :json_schema :name])))))))

(deftest llama-cpp-tool-arguments-arrive-as-a-json-string
  (with-http-server
   (fn [_] {:choices [{:message {:tool_calls [{:function {:arguments "{\"ok\": true}"}}]}}]})
   (fn [port _]
     (is (= {:ok true}
            (runtime/invoke-model-http
             {:model (llama-model port)
              :messages []
              :runtime-options {:tool-name "emit_bool_result_v1"}
              :output-schema bool-schema
              :tools? true}))))))

(deftest llama-cpp-missing-tool-call-is-an-explained-failure
  (with-http-server
   (fn [_] {:choices [{:message {:content "no call"}}]})
   (fn [port _]
     (is (thrown? clojure.lang.ExceptionInfo
                  (runtime/invoke-model-http
                   {:model (llama-model port)
                    :messages []
                    :runtime-options {:tool-name "emit_bool_result_v1"}
                    :output-schema bool-schema
                    :tools? true}))))))

(deftest non-success-http-status-and-unknown-providers-fail-loudly
  (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext
     server "/"
     (reify HttpHandler
       (handle [_ exchange]
         (respond-json exchange 500 {:error "boom"}))))
    (.start server)
    (try
      (let [error (try
                    (runtime/invoke-model-http
                     {:model {:model/provider :ollama
                              :model/endpoint (str "http://127.0.0.1:"
                                                   (.getPort (.getAddress server)))
                              :model/name "m"}
                      :messages []
                      :runtime-options {}
                      :output-schema nil
                      :tools? false})
                    nil
                    (catch clojure.lang.ExceptionInfo error error))]
        (is (= 500 (:http/status (ex-data error)))))
      (finally (.stop server 0))))
  (is (thrown? clojure.lang.ExceptionInfo
               (runtime/invoke-model-http
                {:model {:model/provider :carrier-pigeon
                         :model/endpoint "http://127.0.0.1:1"
                         :model/name "m"}
                 :messages []
                 :runtime-options {}
                 :output-schema nil
                 :tools? false}))))

(def bool-plan
  {:output {:output/id :test/bool-result-v1
            :output/format :edn
            :output/on-invalid :repair-once
            :output/schema [:map [:ok :boolean]]}
   :output-schema bool-schema
   :prompt {:prompt/output-contract :inline-schema}
   :model {:model/id :test/model :model/name "test-model"}})

(deftest model-retries-after-a-transport-failure
  (let [calls (atom 0)
        runtime {:invoke-model!
                 (fn [_]
                   (if (zero? @calls)
                     (do (swap! calls inc)
                         (throw (java.net.ConnectException. "refused")))
                     "{:ok true}"))}
        result (runtime/call-model-and-validate runtime bool-plan [])]
    (is (= {:ok true} (:value result)))
    (is (= 2 (:attempts result)))))

(deftest malformed-output-triggers-a-repair-with-feedback
  (let [seen (atom [])
        runtime {:invoke-model!
                 (fn [{:keys [messages]}]
                   (swap! seen conj messages)
                   (if (= 1 (count @seen)) "{:ok " "{:ok true}"))}
        result (runtime/call-model-and-validate runtime bool-plan [])]
    (is (= {:ok true} (:value result)))
    (is (= 1 (:repairs result)))
    (testing "the repair turn explains what broke instead of repeating the prompt"
      (is (str/includes? (get-in @seen [1 (dec (count (second @seen))) :content])
                         "could not be parsed")))))

(deftest invalid-output-is-repaired-with-the-validation-explanation
  (let [calls (atom 0)
        runtime {:invoke-model!
                 (fn [{:keys [messages]}]
                   (swap! calls inc)
                   (if (str/includes? (or (:content (last messages)) "")
                                      "failed validation")
                     "{:ok true}"
                     "{:ok \"yes\"}"))}
        result (runtime/call-model-and-validate runtime bool-plan [])]
    (is (= {:ok true} (:value result)))
    (is (= 2 (:attempts result)))))

(deftest rejecting-contracts-and-exhausted-attempts-fail-with-history
  (let [rejecting (assoc-in bool-plan [:output :output/on-invalid] :reject)
        runtime {:invoke-model! (fn [_] "{:ok \"yes\"}")}
        error (try
                (runtime/call-model-and-validate runtime rejecting [])
                nil
                (catch clojure.lang.ExceptionInfo error error))]
    (is (some? error))
    (is (some :validation (:failures (ex-data error)))))
  (let [runtime {:invoke-model! (fn [_] (throw (java.net.ConnectException.)))}
        error (try
                (runtime/call-model-and-validate runtime bool-plan [])
                nil
                (catch clojure.lang.ExceptionInfo error error))]
    (is (some? error))
    (testing "a message-less exception falls back to the class name"
      (is (some #(str/includes? (:error %) "ConnectException")
                (:failures (ex-data error)))))))

(deftest ensure-feature-covers-hit-dry-run-and-missing-producer
  (let [plan (dsl/compile-extractor-plan program :calliope/song-sections-v1)
        object {:object/id "song-1" :object/content-sha256 "aaa"}
        key (runtime/extractor-cache-key object (:extractor plan) plan)
        ensure! #'runtime/ensure-feature!]
    (testing "a cached event is a hit and skips extraction entirely"
      (let [state {:runtime {}
                   :program program
                   :producer-index {:calliope/song-sections-v1
                                    [:calliope/song-sections-v1]}
                   :cache (atom {["song-1" :calliope/song-sections-v1 key]
                                 {:feature/value [:cached]
                                  :event/status :derived}})
                   :dry-run? false}
            feature (ensure! state object :calliope/song-sections-v1)]
        (is (= :hit (:cache/disposition feature)))
        (is (= [:cached] (:feature/value feature)))
        (is (= :derived (:feature/status feature)))))
    (testing "a dry run reports the miss without extracting"
      (let [state {:runtime {}
                   :program program
                   :producer-index {:calliope/song-sections-v1
                                    [:calliope/song-sections-v1]}
                   :cache (atom {})
                   :dry-run? true}
            feature (ensure! state object :calliope/song-sections-v1)]
        (is (= :dry-run-miss (:cache/disposition feature)))
        (is (= :missing (:feature/status feature)))))
    (testing "an unproducible feature fails at the boundary"
      (is (thrown? clojure.lang.ExceptionInfo
                   (ensure! {:runtime {}
                             :program program
                             :producer-index {}
                             :cache (atom {})
                             :dry-run? false}
                            object
                            :calliope/impossible-v1))))))

(deftest dry-run-builds-selection-and-prompts-without-model-calls
  (let [projection (into {}
                         (map (fn [index]
                                [(str "song-" index)
                                 {:title (str "Song " index)
                                  :classification :suno-lyric
                                  :body-sha256 (format "%064x" index)
                                  :file (str "lyrics/song-" index ".txt")
                                  :flags []}]))
                         (range 12))
        lyric (str "Title: Synthetic Song\n"
                   "Prompt:\nGlitch choir, 84 BPM.\n\n"
                   "**[Verse 1]**\nA branch becomes a promise.\n")
        appended (atom [])
        runtime {:read-edn (fn [_] projection)
                 :read-text (fn [_] lyric)
                 :read-ledger (fn [_] [])
                 :append-event! (fn [path event] (swap! appended conj [path event]))
                 :invoke-model! (fn [_] (throw (ex-info "model called in dry run" {})))
                 :now (constantly "2026-07-27T00:00:00Z")
                 :uuid (constantly "event-1")}
        result (runtime/run-classifier!
                runtime
                program
                :calliope/random-ten-theme-discovery-v1
                {:run-seed 3721599729 :dry-run? true})]
    (is (true? (:dry-run? result)))
    (is (= 10 (count (:selected result))))
    (is (pos? (count (:messages result))))
    (is (str/includes? (:content (last (:messages result))) "Object ID:"))
    (testing "a dry run appends nothing"
      (is (= [] @appended)))))
