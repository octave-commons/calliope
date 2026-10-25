(ns calliope.studio.content-test
  (:require [calliope.law.studio :as law]
            [calliope.studio.content :as content]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]))

(def actor {:actor/id "operator" :actor/kind :user})
(def stamp "2026-10-25T08:00:00Z")
(def sha (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))
(def render
  {:render/id "r1" :render/work-id "w1" :render/sha256 sha
   :render/duration-ms 100 :render/codec "wav"
   :render/source {:source/renderer "synthetic-fixture"}})
(def clip
  {:clip/id "c1" :clip/source {:source/type :render :source/id "r1" :source/sha256 sha}
   :clip/range {:start-ms 10 :end-ms 90} :clip/fades {:in-ms 10 :out-ms 20}
   :clip/gain-db 0.0 :clip/title "Phrase" :clip/status :accepted
   :clip/created-by actor :clip/created-at stamp})
(def arrangement
  {:arrangement/id "a1" :arrangement/entries [{:entry/clip-id "c1" :entry/position 0}]
   :arrangement/cross-work? false :arrangement/version 1
   :arrangement/created-by actor :arrangement/created-at stamp})
(def export
  {:export/id "e1" :export/arrangement-id "a1" :export/arrangement-version 1
   :export/source-hashes [sha] :export/renderer "fixture-renderer-v1"
   :export/encoding {:encoding/format :wav :encoding/settings {:bits 16}}
   :export/output-sha256 sha-b :export/created-at stamp})
(def marker
  {:marker/id "m1" :marker/subject {:subject/type :render :subject/id "r1"}
   :marker/kind :phrase :marker/range {:start-ms 0 :end-ms 100}
   :marker/epistemic :observed :marker/created-by actor :marker/created-at stamp})
(def rating
  {:rating/id "rt1" :rating/subject {:subject/type :clip :subject/id "c1"}
   :rating/dimension :enjoyment :rating/scale :zero-to-five :rating/value 5
   :rating/actor actor :rating/at stamp})

(deftest registry-is-named-composable-plain-data
  (is (= law/registry (edn/read-string (pr-str law/registry))))
  (is (every? #(and (keyword? %) (namespace %)) (keys law/registry)))
  (is ((law/validator :calliope.studio/clip-v1) clip))
  (is (not ((law/validator :calliope.studio/clip-v1) (assoc clip :unknown true)))))

(deftest objects-never-cross-promote
  (let [objects {:calliope.studio/work-v1 {:work/id "w1" :work/title "Session"}
                 :calliope.studio/render-v1 render
                 :calliope.studio/clip-v1 clip
                 :calliope.studio/arrangement-v1 arrangement
                 :calliope.studio/export-v1 export}]
    (doseq [[type value] objects]
      (is (content/valid? type value))
      (is (not (content/valid? type (assoc value :unknown true))))
      (doseq [[other-type other] objects :when (not= type other-type)]
        (is (not (content/valid? type other))))))
  (is (not (content/valid? :calliope.studio/playable-ref-v1
                           {:ref/type :work :ref/id "w1"})))
  (is (not (content/valid? :calliope.studio/work-v1
                           {:work/id "w1" :work/title "   "})))
  (is (not (content/valid? :calliope.studio/clip-v1
                           (assoc clip :clip/created-at "2026-02-29T00:00:00Z")))))

(deftest clip-context-preserves-exact-source-and-bounds
  (is (:ok? (content/clip-check clip render)))
  (doseq [candidate [(assoc-in clip [:clip/source :source/id] "another")
                     (assoc-in clip [:clip/source :source/sha256] sha-b)
                     (assoc-in clip [:clip/source :source/type] :work)
                     (assoc clip :clip/range {:start-ms -1 :end-ms 10})
                     (assoc clip :clip/range {:start-ms 10 :end-ms 10})
                     (assoc clip :clip/range {:start-ms 90 :end-ms 10})
                     (assoc clip :clip/range {:start-ms 0 :end-ms 101})
                     (assoc clip :clip/fades {:in-ms 40 :out-ms 41})
                     (assoc clip :clip/gain-db ##NaN)]]
    (is (false? (:ok? (content/clip-check candidate render))) (pr-str candidate)))
  (testing "exact source end and exactly-fitting fades are valid"
    (is (:ok? (content/clip-check
               (assoc clip :clip/range {:start-ms 0 :end-ms 100}
                           :clip/fades {:in-ms 40 :out-ms 60}) render))))
  (is (false? (:ok? (content/clip-check clip nil)))))

(deftest marker-and-rating-context-retain-scope
  (is (:ok? (content/marker-check marker render)))
  (is (:ok? (content/marker-check (-> marker (dissoc :marker/range)
                                      (assoc :marker/at-ms 0)) render)))
  (is (:ok? (content/marker-check (-> marker
                                      (assoc :marker/subject {:subject/type :arrangement :subject/id "a1"}))
                                 arrangement 100)))
  (doseq [candidate [(dissoc marker :marker/range)
                     (assoc marker :marker/at-ms 10)
                     (assoc marker :marker/range {:start-ms 0 :end-ms 101})
                     (assoc marker :marker/range {:start-ms 90 :end-ms 10})
                     (assoc-in marker [:marker/subject :subject/id] "other")
                     (assoc-in marker [:marker/subject :subject/type] :clip)
                     (-> marker (assoc :marker/epistemic :accepted)
                         (assoc :marker/created-by {:actor/id "model" :actor/kind :agent}))]]
    (is (false? (:ok? (content/marker-check candidate render)))))
  (is (content/rating-for? rating {:subject/type :clip :subject/id "c1"}))
  (is (not (content/rating-for? rating {:subject/type :render :subject/id "c1"})))
  (is (not (content/rating-for? rating {:subject/type :clip :subject/id "other"})))
  (doseq [key [:rating/subject :rating/dimension :rating/scale
               :rating/value :rating/actor :rating/at]]
    (is (not (content/valid? :calliope.studio/rating-v1 (dissoc rating key)))))
  (doseq [value [-1 6]]
    (is (not (content/valid? :calliope.studio/rating-v1 (assoc rating :rating/value value))))))

(deftest arrangement-resolves-accepted-clips-in-declared-order
  (let [c2 (assoc clip :clip/id "c2")
        clips {"c1" clip "c2" c2}
        renders {"r1" render}
        a (assoc arrangement :arrangement/entries
                 [{:entry/clip-id "c1" :entry/position 7}
                  {:entry/clip-id "c2" :entry/position 2}])]
    (is (= ["c2" "c1"] (mapv :entry/clip-id (:entries (content/arrangement-check a clips renders)))))
    (is (false? (:ok? (content/arrangement-check
                       (assoc a :arrangement/entries
                              [{:entry/clip-id "c1" :entry/position 2}
                               {:entry/clip-id "c2" :entry/position 2}]) clips renders))))
    (is (false? (:ok? (content/arrangement-check a (dissoc clips "c2") renders))))
    (is (false? (:ok? (content/arrangement-check a (assoc-in clips ["c2" :clip/id] "wrong") renders))))
    (is (false? (:ok? (content/arrangement-check a (assoc-in clips ["c2" :clip/status] :provisional) renders))))
    (let [r2 (assoc render :render/id "r2" :render/work-id "w2" :render/sha256 sha-b)
          mixed-clips (assoc clips "c2" (assoc c2 :clip/source
                                              {:source/type :render :source/id "r2" :source/sha256 sha-b}))
          mixed-renders (assoc renders "r2" r2)]
      (is (false? (:ok? (content/arrangement-check a mixed-clips mixed-renders))))
      (let [result (content/arrangement-check (assoc a :arrangement/cross-work? true)
                                             mixed-clips mixed-renders)]
        (is (:ok? result))
        (is (= #{"w1" "w2"} (:work-ids result)))
        (is (= #{sha sha-b} (:source-hashes result)))))))

(deftest exports-bind-the-complete-arrangement-context
  (is (:ok? (content/export-check export arrangement {"c1" clip} {"r1" render})))
  (doseq [candidate [(assoc export :export/arrangement-id "another")
                     (assoc export :export/arrangement-version 2)
                     (assoc export :export/source-hashes [sha-b])
                     (assoc export :export/source-hashes [sha sha])
                     (dissoc export :export/renderer)
                     (dissoc export :export/encoding)
                     (assoc export :export/output-sha256 "bad")]]
    (is (false? (:ok? (content/export-check candidate arrangement {"c1" clip} {"r1" render})))))
  (is (false? (:ok? (content/export-check export arrangement {} {"r1" render})))))

(deftest export-lineage-requires-every-distinct-source
  (let [r2 (assoc render :render/id "r2" :render/sha256 sha-b)
        c2 (assoc clip :clip/id "c2" :clip/source
                  {:source/type :render :source/id "r2" :source/sha256 sha-b})
        a (update arrangement :arrangement/entries conj {:entry/clip-id "c2" :entry/position 1})
        clips {"c1" clip "c2" c2} renders {"r1" render "r2" r2}]
    (is (:ok? (content/export-check (assoc export :export/source-hashes [sha-b sha]) a clips renders)))
    (is (false? (:ok? (content/export-check export a clips renders))))
    (is (false? (:ok? (content/export-check
                       (assoc export :export/source-hashes [sha sha-b (apply str (repeat 64 "c"))])
                       a clips renders))))))


(deftest organization-and-query-programs-stay-distinct
  (let [playlist {:playlist/id "p1" :playlist/title "Review"
                  :playlist/entries [{:entry/ref {:ref/type :clip :ref/id "c1"} :entry/position 0}]
                  :playlist/created-by actor :playlist/created-at stamp}
        smart {:smart-list/id "s1" :smart-list/title "Salvage"
               :smart-list/query {:where [:and [:gte :rating/enjoyment 4]
                                           [:eq :disposition :salvage]
                                           [:contains :labels :salvage/good-intro]]}
               :smart-list/created-by actor :smart-list/created-at stamp}
        workspace {:workspace/id "ws1" :workspace/title "Session"
                   :workspace/queue [{:ref/type :render :ref/id "r1"}]
                   :workspace/created-by actor :workspace/created-at stamp}]
    (is (content/valid? :calliope.studio/playlist-v1 playlist))
    (is (content/valid? :calliope.studio/smart-list-v1 smart))
    (is (content/valid? :calliope.studio/workspace-v1 workspace))
    (is (not (content/valid? :calliope.studio/workspace-v1 playlist)))
    (is (not (content/valid? :calliope.studio/playlist-v1 workspace)))
    (doseq [query [[:eval "arbitrary code"]
                   [:and] [:not] [:eq :field (fn [] :side-effect)]
                   [:contains :labels '(eval something)]]]
      (is (not (content/valid? :calliope.studio/where-v1 query))))
    (is (not (content/valid? :calliope.studio/smart-list-v1
                             (assoc-in smart [:smart-list/query :members] ["c1"]))))))

(deftest pure-validation-does-not-mutate-source-values
  (let [before [render clip arrangement export rating marker]]
    (content/clip-check clip render)
    (content/arrangement-check arrangement {"c1" clip} {"r1" render})
    (content/export-check export arrangement {"c1" clip} {"r1" render})
    (is (= before [render clip arrangement export rating marker]))))

(deftest scalar-and-encoding-admission-never-executes-values
  (let [calls (atom 0)
        executable (fn [] (swap! calls inc))]
    (is (not (content/valid? :calliope.studio/query-value-v1 ##NaN)))
    (is (not (content/valid? :calliope.studio/encoding-v1
                             {:encoding/format :wav
                              :encoding/settings {:dynamic executable}})))
    (is (not (content/valid? :calliope.studio/where-v1 [:eq :field executable])))
    (is (zero? @calls))))

(deftest oversized-fades-refuse-without-arithmetic-overflow
  (let [too-large (assoc clip :clip/fades {:in-ms Long/MAX_VALUE :out-ms 1})
        longest-render (assoc render :render/duration-ms Long/MAX_VALUE)
        longest-clip (assoc clip :clip/range {:start-ms 0 :end-ms Long/MAX_VALUE}
                                :clip/fades {:in-ms (dec Long/MAX_VALUE) :out-ms 1})]
    (is (false? (content/valid? :calliope.studio/clip-v1 too-large)))
    (is (= {:ok? false :errors [{:error/code :invalid-clip}]}
           (content/clip-check too-large render)))
    (is (:ok? (content/clip-check longest-clip longest-render)))))

(deftest marker-duration-context-cannot-expand-the-source
  (let [point (-> marker (dissoc :marker/range) (assoc :marker/at-ms 100))
        arrangement-marker (assoc marker :marker/subject
                                  {:subject/type :arrangement :subject/id "a1"})]
    (is (:ok? (content/marker-check point render)))
    (is (false? (:ok? (content/marker-check (assoc point :marker/at-ms 101) render))))
    (is (false? (:ok? (content/marker-check (assoc point :marker/at-ms 101) render 101))))
    (is (false? (:ok? (content/marker-check arrangement-marker arrangement))))
    (is (false? (:ok? (content/marker-check marker nil))))))
