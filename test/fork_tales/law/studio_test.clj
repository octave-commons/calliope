(ns fork-tales.law.studio-test
  (:require [clojure.test :refer [deftest is testing]]
            [fork-tales.law.studio :as studio]
            [malli.core :as m]
            [malli.registry :as mr]))

(def registry
  (mr/composite-registry m/default-registry studio/schemas))

(defn valid? [schema-name value]
  (m/validate [:ref schema-name] value {:registry registry}))

(def actor {:actor/id "err" :actor/kind :user})

(def sha (apply str (repeat 64 "a")))
(def sha-b (apply str (repeat 64 "b")))

(def render
  {:render/id "r-1"
   :render/work-id "w-1"
   :render/sha256 sha
   :render/duration-ms 279120
   :render/codec "mp3"
   :render/source {:source/renderer "suno"
                   :source/renderer-clip-id "28c9cc35-78ae-47a2-abee-39fa681ea818"}})

(def clip
  {:clip/id "c-1"
   :clip/source {:source/type :render :source/id "r-1" :source/sha256 sha}
   :clip/range {:start-ms 1200 :end-ms 43820}
   :clip/fades {:in-ms 0 :out-ms 500}
   :clip/gain-db 0.0
   :clip/title "Good opening before drift"
   :clip/status :accepted
   :clip/created-by actor
   :clip/created-at "2026-08-02T00:00:00Z"})

(deftest object-types-are-distinct
  (testing "work, render, clip, arrangement, and export are separate schemas"
    (is (valid? "work/v1" {:work/id "w-1" :work/title "η"}))
    (is (valid? "render/v1" render))
    (is (valid? "clip/v1" clip))
    (is (valid? "arrangement/v1"
                {:arrangement/id "a-1"
                 :arrangement/entries [{:entry/clip-id "c-1" :entry/position 0}]
                 :arrangement/cross-work? false
                 :arrangement/version 1
                 :arrangement/created-by actor
                 :arrangement/created-at "2026-08-02T00:00:00Z"}))
    (is (valid? "export/v1"
                {:export/id "e-1"
                 :export/arrangement-id "a-1"
                 :export/arrangement-version 1
                 :export/source-hashes [sha]
                 :export/renderer "ffmpeg 7.1"
                 :export/encoding {:encoding/format :flac :encoding/settings {:level 8}}
                 :export/output-sha256 sha-b
                 :export/created-at "2026-08-02T00:00:00Z"}))
    (testing "a work is not valid as any playable type"
      (is (not (valid? "render/v1" {:work/id "w-1" :work/title "η"})))
      (is (not (valid? "clip/v1" {:work/id "w-1" :work/title "η"}))))))

(deftest clip-law-requires-immutable-source-and-valid-range
  (testing "clip source must be a render with a content hash"
    (is (valid? "clip/v1" clip))
    (is (not (valid? "clip/v1"
                     (assoc-in clip [:clip/source :source/sha256] "not-a-hash"))))
    (is (not (valid? "clip/v1"
                     (update clip :clip/source dissoc :source/sha256))))
    (testing "cross-scope: a clip cannot source a work or another clip"
      (is (not (valid? "clip/v1"
                       (assoc-in clip [:clip/source :source/type] :work))))
      (is (not (valid? "clip/v1"
                       (assoc-in clip [:clip/source :source/type] :clip))))))
  (testing "ranges must be positive and ordered"
    (is (not (valid? "clip/v1"
                     (assoc clip :clip/range {:start-ms 43820 :end-ms 1200}))))
    (is (not (valid? "clip/v1"
                     (assoc clip :clip/range {:start-ms 100 :end-ms 100}))))
    (is (not (valid? "clip/v1"
                     (assoc clip :clip/range {:start-ms -5 :end-ms 100}))))
    (is (not (valid? "clip/v1"
                     (assoc clip :clip/range {:start-ms 0 :end-ms 0})))))
  (testing "unknown keys are rejected (closed map)"
    (is (not (valid? "clip/v1" (assoc clip :clip/destructive-edit? true))))))

(deftest rating-law-is-scoped-and-complete
  (let [rating {:rating/id "rt-1"
                :rating/subject {:subject/type :clip :subject/id "c-1"}
                :rating/dimension :enjoyment
                :rating/scale :zero-to-five
                :rating/value 5
                :rating/actor actor
                :rating/at "2026-08-02T00:00:00Z"}]
    (testing "subject, dimension, scale, value, actor, and time are required"
      (is (valid? "rating/v1" rating))
      (doseq [k [:rating/subject :rating/dimension :rating/scale
                 :rating/value :rating/actor :rating/at]]
        (is (not (valid? "rating/v1" (dissoc rating k))))))
    (testing "value must fit the declared scale"
      (is (not (valid? "rating/v1" (assoc rating :rating/value 6))))
      (is (not (valid? "rating/v1" (assoc rating :rating/value -1)))))
    (testing "all five ADR-001 dimensions are available on every object type"
      (doseq [d [:enjoyment :publishability :weirdness :salvageability
                 :technical-quality]
              t [:work :render :clip :arrangement :export]]
        (is (valid? "rating/v1"
                    (-> rating
                        (assoc :rating/dimension d)
                        (assoc :rating/subject {:subject/type t :subject/id "x"}))))))
    (testing "unknown dimensions are rejected"
      (is (not (valid? "rating/v1"
                       (assoc rating :rating/dimension :overall)))))))

(deftest marker-law-preserves-scope-and-epistemic-status
  (let [marker {:marker/id "m-1"
                :marker/subject {:subject/type :render :subject/id "r-1"}
                :marker/kind :good-intro
                :marker/at-ms 1200
                :marker/epistemic :accepted
                :marker/created-by actor
                :marker/created-at "2026-08-02T00:00:00Z"}]
    (is (valid? "marker/v1" marker))
    (is (valid? "marker/v1" (assoc-in marker [:marker/subject :subject/type] :arrangement)))
    (testing "cross-scope: markers never annotate clips, exports, or works"
      (doseq [t [:clip :export :work]]
        (is (not (valid? "marker/v1"
                         (assoc-in marker [:marker/subject :subject/type] t))))))
    (testing "epistemic status is part of the contract"
      (doseq [e [:observed :derived :provisional :accepted]]
        (is (valid? "marker/v1" (assoc marker :marker/epistemic e))))
      (is (not (valid? "marker/v1" (dissoc marker :marker/epistemic)))))))

(deftest playlist-smart-list-workspace-are-distinct
  (testing "playlist membership accepts every declared playable ref type"
    (doseq [t [:render :clip :arrangement :export]]
      (is (valid? "playlist/v1"
                  {:playlist/id "p-1"
                   :playlist/title "Mix"
                   :playlist/entries [{:entry/ref {:ref/type t :ref/id "x"}
                                       :entry/position 0}]
                   :playlist/created-by actor
                   :playlist/created-at "2026-08-02T00:00:00Z"}))))
  (testing "a playlist cannot contain a work directly"
    (is (not (valid? "playlist/entry-v1"
                     {:entry/ref {:ref/type :work :ref/id "w-1"}
                      :entry/position 0}))))
  (testing "smart list stores a closed query, not frozen membership"
    (is (valid? "smart-list/v1"
                {:smart-list/id "sl-1"
                 :smart-list/title "Salvageable nocturnes"
                 :smart-list/query
                 {:where [:and
                          [:gte :rating/enjoyment 4]
                          [:eq :disposition :salvage]
                          [:contains :labels :salvage/good-intro]]
                  :sort [[:rating/salvageability :desc]
                         [:duration-ms :asc]]}
                 :smart-list/created-by actor
                 :smart-list/created-at "2026-08-02T00:00:00Z"}))
    (is (not (valid? "smart-list/query-v1"
                     {:where [:and [:gte :rating/enjoyment 4]]
                      :members ["r-1" "r-2"]})))
    (is (not (valid? "smart-list/where-v1" [:eval "some code"]))))
  (testing "workspace is its own object with attention context"
    (is (valid? "workspace/v1"
                {:workspace/id "ws-1"
                 :workspace/title "Gates of Aker release pass"
                 :workspace/filters [:eq :disposition :keeper]
                 :workspace/pinned [{:ref/type :render :ref/id "r-1"}]
                 :workspace/queue [{:ref/type :clip :ref/id "c-1"}]
                 :workspace/notes "compare rerenders"
                 :workspace/focus {:focus/kind :release :focus/id "rel-1"}
                 :workspace/created-by actor
                 :workspace/created-at "2026-08-02T00:00:00Z"}))
    (is (not (valid? "workspace/v1"
                     {:playlist/id "ws-1"
                      :playlist/title "disguised playlist"
                      :playlist/entries []
                      :playlist/created-by actor
                      :playlist/created-at "2026-08-02T00:00:00Z"})))))

(deftest release-and-publication-preserve-per-target-state
  (let [release {:release/id "rel-1"
                 :release/title "Fork Tax Lullabies"
                 :release/type :album
                 :release/assets [{:ref/type :export :ref/id "e-1"}]
                 :release/rights-basis "cc-by-sa-4.0"
                 :release/provenance {:provenance/source-hashes [sha]}
                 :release/targets
                 [{:target/id :soundcloud :target/state :ready}
                  {:target/id :youtube
                   :target/state :manual-action-required
                   :target/attempts
                   [{:attempt/id "att-1"
                     :attempt/at "2026-08-02T00:00:00Z"
                     :attempt/state :failed
                     :attempt/summary "oauth expired"}]}]
                 :release/acceptance :candidate}]
    (is (valid? "release/v1" release))
    (testing "targets declare capabilities"
      (is (valid? "publication-target/v1"
                  {:target/id :youtube
                   :target/capabilities #{:resumable-upload :metadata-sync}}))
      (is (not (valid? "publication-target/v1"
                       {:target/id :youtube :target/capabilities #{}}))))
    (testing "the full attempt state machine is available per target"
      (doseq [s [:planned :validating :rendering :ready :authenticating
                 :uploading :processing :published :failed :unavailable
                 :cancelled :manual-action-required]]
        (is (valid? "release/target-state-v1"
                    {:target/id :soundcloud :target/state s}))))
    (testing "credentials have no place in durable records (closed maps)"
      (is (not (valid? "publication/attempt-v1"
                       {:attempt/id "att-1"
                        :attempt/at "2026-08-02T00:00:00Z"
                        :attempt/state :published
                        :attempt/access-token "secret"})))
      (is (not (valid? "release/target-state-v1"
                       {:target/id :soundcloud
                        :target/state :ready
                        :target/refresh-token "secret"}))))))

(deftest disposition-and-label-contracts
  (doseq [d [:keeper :salvage :reject :unreviewed]]
    (is (valid? "disposition/v1" d)))
  (is (not (valid? "disposition/v1" :masterpiece)))
  (doseq [l ["mood/nocturnal" "voice/clean-female" "issue/late-collapse"
             "salvage/good-intro" "release/needs-artwork" "world/gates-of-aker"]]
    (is (valid? "label/v1" l)))
  (is (not (valid? "label/v1" "no-namespace")))
  (is (not (valid? "label/v1" "UPPER/case"))))
