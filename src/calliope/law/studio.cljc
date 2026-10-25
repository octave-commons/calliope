(ns calliope.law.studio
  "Plain-data content contracts recovered from PR #7, adapted to current Calliope.
   Cross-field and referenced-object admission is in calliope.studio.content.
   Release/publication state is outside this content recovery."
  (:require [malli.core :as m]
            [malli.registry :as mr]))

(def registry
  {:calliope.studio/non-blank-string [:re "[\\s\\S]*\\S[\\s\\S]*"]
   :calliope.studio/id-v1 [:or :uuid [:ref :calliope.studio/non-blank-string]]
   :calliope.studio/sha256-v1 [:re "^[0-9a-f]{64}$"]
   :calliope.studio/instant-v1 [:string {:min 1}]
   :calliope.studio/nat-v1 [:int {:min 0}]
   :calliope.studio/positive-v1 [:int {:min 1}]
   :calliope.studio/number-v1 [:or :int :double]
   :calliope.studio/actor-v1
   [:map {:closed true}
    [:actor/id [:ref :calliope.studio/non-blank-string]]
    [:actor/kind [:enum :user :agent :classifier :system]]]

   :calliope.studio/object-type-v1 [:enum :work :render :clip :arrangement :export]
   :calliope.studio/playable-type-v1 [:enum :render :clip :arrangement :export]
   :calliope.studio/playable-ref-v1
   [:map {:closed true}
    [:ref/type [:ref :calliope.studio/playable-type-v1]]
    [:ref/id [:ref :calliope.studio/id-v1]]]
   :calliope.studio/subject-ref-v1
   [:map {:closed true}
    [:subject/type [:ref :calliope.studio/object-type-v1]]
    [:subject/id [:ref :calliope.studio/id-v1]]]

   :calliope.studio/work-v1
   [:map {:closed true}
    [:work/id [:ref :calliope.studio/id-v1]]
    [:work/title [:ref :calliope.studio/non-blank-string]]
    [:work/notes {:optional true} :string]]
   :calliope.studio/render-source-v1
   [:map {:closed true}
    [:source/renderer [:ref :calliope.studio/non-blank-string]]
    [:source/renderer-clip-id {:optional true} [:ref :calliope.studio/non-blank-string]]
    [:source/uri {:optional true} [:ref :calliope.studio/non-blank-string]]]
   :calliope.studio/render-v1
   [:map {:closed true}
    [:render/id [:ref :calliope.studio/id-v1]]
    [:render/work-id [:ref :calliope.studio/id-v1]]
    [:render/sha256 [:ref :calliope.studio/sha256-v1]]
    [:render/duration-ms [:ref :calliope.studio/positive-v1]]
    [:render/codec [:ref :calliope.studio/non-blank-string]]
    [:render/source [:ref :calliope.studio/render-source-v1]]]

   :calliope.studio/time-range-v1
   [:map {:closed true}
    [:start-ms [:ref :calliope.studio/nat-v1]]
    [:end-ms [:ref :calliope.studio/positive-v1]]]
   :calliope.studio/fades-v1
   [:map {:closed true}
    [:in-ms [:ref :calliope.studio/nat-v1]]
    [:out-ms [:ref :calliope.studio/nat-v1]]]
   :calliope.studio/marker-subject-v1
   [:map {:closed true}
    [:subject/type [:enum :render :arrangement]]
    [:subject/id [:ref :calliope.studio/id-v1]]]
   :calliope.studio/marker-v1
   [:map {:closed true}
    [:marker/id [:ref :calliope.studio/id-v1]]
    [:marker/subject [:ref :calliope.studio/marker-subject-v1]]
    [:marker/kind :keyword]
    [:marker/at-ms {:optional true} [:ref :calliope.studio/nat-v1]]
    [:marker/range {:optional true} [:ref :calliope.studio/time-range-v1]]
    [:marker/epistemic [:enum :observed :derived :provisional :accepted]]
    [:marker/created-by [:ref :calliope.studio/actor-v1]]
    [:marker/created-at [:ref :calliope.studio/instant-v1]]]
   :calliope.studio/clip-source-v1
   [:map {:closed true}
    [:source/type [:= :render]]
    [:source/id [:ref :calliope.studio/id-v1]]
    [:source/sha256 [:ref :calliope.studio/sha256-v1]]]
   :calliope.studio/clip-v1
   [:map {:closed true}
    [:clip/id [:ref :calliope.studio/id-v1]]
    [:clip/source [:ref :calliope.studio/clip-source-v1]]
    [:clip/range [:ref :calliope.studio/time-range-v1]]
    [:clip/fades {:optional true} [:ref :calliope.studio/fades-v1]]
    [:clip/gain-db {:optional true} [:ref :calliope.studio/number-v1]]
    [:clip/title {:optional true} [:ref :calliope.studio/non-blank-string]]
    [:clip/status [:enum :provisional :accepted :rejected]]
    [:clip/created-by [:ref :calliope.studio/actor-v1]]
    [:clip/created-at [:ref :calliope.studio/instant-v1]]]

   :calliope.studio/transition-v1
   [:map {:closed true}
    [:transition/kind [:enum :cut :crossfade :gap]]
    [:transition/duration-ms [:ref :calliope.studio/nat-v1]]]
   :calliope.studio/arrangement-entry-v1
   [:map {:closed true}
    [:entry/clip-id [:ref :calliope.studio/id-v1]]
    [:entry/position [:ref :calliope.studio/nat-v1]]
    [:entry/transition {:optional true} [:ref :calliope.studio/transition-v1]]]
   :calliope.studio/arrangement-v1
   [:map {:closed true}
    [:arrangement/id [:ref :calliope.studio/id-v1]]
    [:arrangement/entries [:vector {:min 1} [:ref :calliope.studio/arrangement-entry-v1]]]
    [:arrangement/cross-work? :boolean]
    [:arrangement/version [:ref :calliope.studio/positive-v1]]
    [:arrangement/created-by [:ref :calliope.studio/actor-v1]]
    [:arrangement/created-at [:ref :calliope.studio/instant-v1]]]
   :calliope.studio/encoding-v1
   [:map {:closed true}
    [:encoding/format [:enum :wav :flac :mp3 :video]]
    [:encoding/settings :map]]
   :calliope.studio/export-v1
   [:map {:closed true}
    [:export/id [:ref :calliope.studio/id-v1]]
    [:export/arrangement-id [:ref :calliope.studio/id-v1]]
    [:export/arrangement-version [:ref :calliope.studio/positive-v1]]
    [:export/source-hashes [:vector {:min 1} [:ref :calliope.studio/sha256-v1]]]
    [:export/renderer [:ref :calliope.studio/non-blank-string]]
    [:export/encoding [:ref :calliope.studio/encoding-v1]]
    [:export/output-sha256 [:ref :calliope.studio/sha256-v1]]
    [:export/created-at [:ref :calliope.studio/instant-v1]]]

   :calliope.studio/rating-v1
   [:map {:closed true}
    [:rating/id [:ref :calliope.studio/id-v1]]
    [:rating/subject [:ref :calliope.studio/subject-ref-v1]]
    [:rating/dimension [:enum :enjoyment :publishability :weirdness :salvageability
                        :technical-quality]]
    [:rating/scale [:= :zero-to-five]]
    [:rating/value [:int {:min 0 :max 5}]]
    [:rating/actor [:ref :calliope.studio/actor-v1]]
    [:rating/at [:ref :calliope.studio/instant-v1]]]
   :calliope.studio/disposition-v1 [:enum :keeper :salvage :reject :unreviewed]
   :calliope.studio/label-v1 [:re "^[a-z0-9][a-z0-9-]*/[a-z0-9][a-z0-9-]*$"]
   :calliope.studio/playlist-entry-v1
   [:map {:closed true}
    [:entry/ref [:ref :calliope.studio/playable-ref-v1]]
    [:entry/position [:ref :calliope.studio/nat-v1]]]
   :calliope.studio/playlist-v1
   [:map {:closed true}
    [:playlist/id [:ref :calliope.studio/id-v1]]
    [:playlist/title [:ref :calliope.studio/non-blank-string]]
    [:playlist/entries [:vector [:ref :calliope.studio/playlist-entry-v1]]]
    [:playlist/created-by [:ref :calliope.studio/actor-v1]]
    [:playlist/created-at [:ref :calliope.studio/instant-v1]]]
   :calliope.studio/query-value-v1 [:maybe [:or :string :keyword :boolean :int :double]]
   :calliope.studio/where-v1
   [:or
    [:tuple [:= :eq] :keyword [:ref :calliope.studio/query-value-v1]]
    [:tuple [:= :not-eq] :keyword [:ref :calliope.studio/query-value-v1]]
    [:tuple [:= :gte] :keyword [:ref :calliope.studio/query-value-v1]]
    [:tuple [:= :lte] :keyword [:ref :calliope.studio/query-value-v1]]
    [:tuple [:= :contains] :keyword [:ref :calliope.studio/query-value-v1]]
    [:tuple [:= :not] [:ref :calliope.studio/where-v1]]
    [:cat [:= :and] [:+ [:schema [:ref :calliope.studio/where-v1]]]]
    [:cat [:= :or] [:+ [:schema [:ref :calliope.studio/where-v1]]]]]
   :calliope.studio/query-v1
   [:map {:closed true}
    [:where [:ref :calliope.studio/where-v1]]
    [:sort {:optional true} [:vector [:tuple :keyword [:enum :asc :desc]]]]]
   :calliope.studio/smart-list-v1
   [:map {:closed true}
    [:smart-list/id [:ref :calliope.studio/id-v1]]
    [:smart-list/title [:ref :calliope.studio/non-blank-string]]
    [:smart-list/query [:ref :calliope.studio/query-v1]]
    [:smart-list/created-by [:ref :calliope.studio/actor-v1]]
    [:smart-list/created-at [:ref :calliope.studio/instant-v1]]]
   :calliope.studio/workspace-focus-v1
   [:map {:closed true}
    [:focus/kind [:enum :arrangement :release :comparison]]
    [:focus/id [:ref :calliope.studio/id-v1]]]
   :calliope.studio/workspace-v1
   [:map {:closed true}
    [:workspace/id [:ref :calliope.studio/id-v1]]
    [:workspace/title [:ref :calliope.studio/non-blank-string]]
    [:workspace/filters {:optional true} [:ref :calliope.studio/where-v1]]
    [:workspace/pinned {:optional true} [:vector [:ref :calliope.studio/playable-ref-v1]]]
    [:workspace/queue {:optional true} [:vector [:ref :calliope.studio/playable-ref-v1]]]
    [:workspace/notes {:optional true} :string]
    [:workspace/focus {:optional true} [:ref :calliope.studio/workspace-focus-v1]]
    [:workspace/created-by [:ref :calliope.studio/actor-v1]]
    [:workspace/created-at [:ref :calliope.studio/instant-v1]]]})

(def malli-registry (mr/composite-registry m/default-registry registry))

(defn schema [schema-key]
  (m/schema [:ref schema-key] {:registry malli-registry}))

(def validator
  "Compiled structural validator. Use content admission for cross-field/context laws."
  (memoize (fn [schema-key] (m/validator (schema schema-key)))))

