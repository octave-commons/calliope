(ns fork-tales.classifier.runtime-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
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

(defn- production-result
  [messages]
  (let [content (:content (last messages))
        object-id (second (re-find #"Object ID: ([^\n]+)" content))]
    (pr-str
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
                   :quote-or-description "Glitch choir, 84 BPM, no heroic uplift."}]}})))

(defn- discovery-result
  [messages]
  (let [content (:content (last messages))
        ids (->> (re-seq #"Object ID: ([^\n]+)" content)
                 (map second)
                 distinct
                 (take 2)
                 vec)]
    (pr-str
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
        :concept/confidence 0.9}]})))

(deftest complete-run-extracts-caches-classifies-and-appends
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
           (if (= "gemma4:e2b" (:model/name model))
             (production-result messages)
             (discovery-result messages)))
         :now (constantly "2026-07-27T00:00:00Z")
         :uuid #(str "event-" (swap! ids inc))}
        result (runtime/run-classifier!
                fake-runtime
                program
                :fork-tales/random-ten-theme-discovery-v1
                {:run-seed 3721599729})
        appended (mapv second @events)]
    (testing "ten missing production features are extracted before classification"
      (is (= 10 (count (filter #(= :feature/extracted (:event/type %)) appended)))))
    (testing "the final result remains provisional and evidence-bound"
      (is (= :concept/discovered (get-in result [:event :event/type])))
      (is (= :provisional (get-in result [:event :event/status])))
      (is (= 10 (count (get-in result [:event :selection/object-ids]))))
      (is (= "branching as obligation"
             (get-in result [:result :concepts 0 :concept/name]))))
    (testing "all writes target the declared append-only ledger"
      (is (= #{"ledgers/classification.edn"}
             (set (map first @events)))))))
