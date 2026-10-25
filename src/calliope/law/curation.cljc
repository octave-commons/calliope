(ns calliope.law.curation
  "GPL-3.0-or-later. Named portable phrase/proposal/decision data."
  (:require [calliope.law.capture :as capture]
            [malli.core :as m]
            [malli.registry :as mr]
            [malli.util :as mu]))

(defn command-form [type fields]
  [:merge [:ref :calliope.curation/command-base-v1]
   (into [:map [:command/type [:= type]]] fields)])

(def registry
  (merge capture/registry
         {:calliope.curation/text-v1 [:string {:min 1 :max 4096}]
          :calliope.curation/labels-v1 [:vector {:max 32} [:ref :calliope.studio/label-v1]]
          :calliope.curation/command-base-v1
          [:map {:closed true} [:command/type :keyword] [:command/version [:= 1]]
           [:request/id [:ref :calliope.capture/uuid-v1]]
           [:session/id [:ref :calliope.capture/uuid-v1]]
           [:session/revision [:ref :calliope.capture/nat-v1]]]
          :calliope.curation/range-v1
          [:map {:closed true} [:start-sample [:ref :calliope.capture/nat-v1]]
           [:end-sample [:ref :calliope.capture/positive-v1]]]
          :calliope.curation/mark-command-v1
          (command-form :studio/mark
                        [[:take/id [:ref :calliope.capture/uuid-v1]]
                         [:marker/id [:ref :calliope.capture/uuid-v1]]
                         [:range [:ref :calliope.curation/range-v1]]])
          :calliope.curation/select-command-v1
          (command-form :studio/select-clip
                        [[:take/id [:ref :calliope.capture/uuid-v1]]
                         [:clip/id [:ref :calliope.capture/uuid-v1]]
                         [:source/sha256 [:ref :calliope.studio/sha256-v1]]
                         [:range [:ref :calliope.curation/range-v1]]
                         [:clip/title [:ref :calliope.session/title-v1]]])
          :calliope.curation/annotate-command-v1
          (command-form :studio/annotate
                        [[:clip/id [:ref :calliope.capture/uuid-v1]]
                         [:annotation/text [:ref :calliope.curation/text-v1]]
                         [:annotation/labels [:ref :calliope.curation/labels-v1]]])
          :calliope.curation/request-command-v1
          (command-form :studio/request-proposal
                        [[:clip/id [:ref :calliope.capture/uuid-v1]]
                         [:proposal/id [:ref :calliope.capture/uuid-v1]]
                         [:proposal/prompt [:ref :calliope.curation/text-v1]]])
          :calliope.curation/output-v1
          [:map {:closed true} [:text [:ref :calliope.curation/text-v1]]
           [:labels [:ref :calliope.curation/labels-v1]]
           [:provider/id [:string {:min 1 :max 200}]]
           [:model/id [:string {:min 1 :max 200}]]
           [:prompt/sha256 [:ref :calliope.studio/sha256-v1]]
           [:evidence/refs [:vector {:min 1 :max 32} [:string {:min 1 :max 2048}]]]
           [:input/kind [:enum :synthetic-metadata :audio :midi-metadata]]
           [:execution/kind [:enum :fixture :local-inference]]]
          :calliope.curation/result-command-v1
          (command-form :studio/proposal-result
                        [[:proposal/id [:ref :calliope.capture/uuid-v1]]
                         [:proposal/output [:ref :calliope.curation/output-v1]]])
          :calliope.curation/failure-command-v1
          (command-form :studio/proposal-failed
                        [[:proposal/id [:ref :calliope.capture/uuid-v1]]
                         [:failure/code [:enum :timeout :unavailable :invalid-output :failed]]
                         [:failure/message [:string {:min 1 :max 500}]]])
          :calliope.curation/decide-command-v1
          (command-form :studio/decide
                        [[:proposal/id [:ref :calliope.capture/uuid-v1]]
                         [:decision/value [:enum :accept :reject]]])
          :calliope.curation/command-v1
          [:or [:ref :calliope.curation/mark-command-v1] [:ref :calliope.curation/select-command-v1]
           [:ref :calliope.curation/annotate-command-v1] [:ref :calliope.curation/request-command-v1]
           [:ref :calliope.curation/result-command-v1] [:ref :calliope.curation/failure-command-v1]
           [:ref :calliope.curation/decide-command-v1]]
          :calliope.curation/value-v1
          [:map {:closed true} [:value/type [:= :curation-applied]]
           [:curation/command [:ref :calliope.curation/command-v1]]]}))

;; Title is named here as well so this independent registry compiles without
;; loading the session/event catalog (which composes this registry).
(def complete-registry
  (assoc registry :calliope.session/title-v1
         [:and [:ref :calliope.studio/non-blank-string] [:string {:max 200}]]))
(def malli-registry (mr/composite-registry m/default-registry (mu/schemas) complete-registry))
(def validator (memoize #(m/validator [:ref %] {:registry malli-registry})))
(defn valid? [type value] (boolean (and (contains? complete-registry type) ((validator type) value))))
(def completion-types #{:studio/proposal-result :studio/proposal-failed})
(defn completion? [command] (contains? completion-types (:command/type command)))
