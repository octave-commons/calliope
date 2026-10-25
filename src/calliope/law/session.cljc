(ns calliope.law.session
  "GPL-3.0-or-later. Calliope session data composed with Clio's event envelope."
  (:require [calliope.law.capture :as capture]
            [clio.law.append :as append-law]
            [clio.law.schema :as clio-schema]
            [malli.core :as m]
            [malli.registry :as mr]
            [malli.util :as mu]))

(def event-type :calliope.session/command-recorded-v1)

(def registry
  (merge
   capture/registry
   {:calliope.session/title-v1
    [:and [:ref :calliope.studio/non-blank-string] [:string {:max 200}]]
    :calliope.session/links-v1
    [:vector {:max 32} [:and [:ref :calliope.studio/non-blank-string] [:string {:max 2048}]]]
    :calliope.session/command-base-v1
    [:map {:closed true}
     [:command/type :keyword] [:command/version [:= 1]]
     [:request/id [:ref :calliope.capture/uuid-v1]]
     [:session/id [:ref :calliope.capture/uuid-v1]]
     [:session/revision [:ref :calliope.capture/nat-v1]]]
    :calliope.session/create-command-v1
    [:merge [:ref :calliope.session/command-base-v1]
     [:map [:command/type [:= :studio/session-create]]
      [:session/title [:ref :calliope.session/title-v1]]
      [:work/id [:ref :calliope.studio/non-blank-string]]
      [:session/links [:ref :calliope.session/links-v1]]]]
    :calliope.session/arm-command-v1
    [:merge [:ref :calliope.session/command-base-v1]
     [:map [:command/type [:= :studio/arm]]
      [:capture/clock [:ref :calliope.capture/clock-v1]]
      [:capture/origin [:ref :calliope.capture/origin-v1]]]]
    :calliope.session/stop-command-v1
    [:merge [:ref :calliope.session/command-base-v1]
     [:map [:command/type [:= :studio/stop]]]]
    :calliope.session/command-v1
    [:or [:ref :calliope.session/create-command-v1]
     [:ref :calliope.session/arm-command-v1]
     [:ref :calliope.capture/ingest-command-v1]
     [:ref :calliope.session/stop-command-v1]]
    :calliope.session/created-value-v1
    [:map {:closed true} [:value/type [:= :session-created]]
     [:session/id [:ref :calliope.capture/uuid-v1]]
     [:session/title [:ref :calliope.session/title-v1]]
     [:work/id [:ref :calliope.studio/non-blank-string]]
     [:session/links [:ref :calliope.session/links-v1]]]
    :calliope.session/armed-value-v1
    [:map {:closed true} [:value/type [:= :capture-armed]]
     [:capture/clock [:ref :calliope.capture/clock-v1]]
     [:capture/origin [:ref :calliope.capture/origin-v1]]]
    :calliope.session/stopped-value-v1
    [:map {:closed true} [:value/type [:= :capture-stopped]]]
    :calliope.session/take-value-v1
    [:map {:closed true} [:value/type [:= :take-admitted]]
     [:take [:ref :calliope.capture/take-v1]]]
    :calliope.session/value-v1
    [:or [:ref :calliope.session/created-value-v1]
     [:ref :calliope.session/armed-value-v1]
     [:ref :calliope.session/stopped-value-v1]
     [:ref :calliope.session/take-value-v1]]
    :calliope.session/record-v1
    [:map {:closed true} [:record/version [:= 1]]
     [:record/actor [:ref :calliope.studio/actor-v1]]
     [:record/command [:ref :calliope.session/command-v1]]
     [:record/value [:ref :calliope.session/value-v1]]]
    :calliope.session/append-recovery-v1 append-law/recovery
    :calliope.session/result-v1
    [:or
     [:map {:closed true} [:result/status [:= :ok]]
      [:request/id [:ref :calliope.capture/uuid-v1]]
      [:event/id [:ref :calliope.capture/uuid-v1]]
      [:result/value [:ref :calliope.session/value-v1]]]
     [:map {:closed true} [:result/status [:= :error]]
      [:request/id [:maybe [:ref :calliope.capture/uuid-v1]]]
      [:error/code :keyword] [:error/message :string]]
     [:map {:closed true} [:result/status [:= :uncertain]]
      [:request/id [:ref :calliope.capture/uuid-v1]]
      [:append/recovery [:ref :calliope.session/append-recovery-v1]]
      [:error/code :keyword] [:error/message :string]]]}))

(def malli-registry (mr/composite-registry m/default-registry (mu/schemas) registry))
(def validator (memoize (fn [type] (m/validator [:ref type] {:registry malli-registry}))))

(defn valid? [type value]
  (boolean (and (contains? registry type) ((validator type) value))))

(def catalog
  "Expand Malli utility forms through their owner before giving plain forms to
   Clio. References remain named and the catalog remains EDN-serializable."
  (assoc (into {} (map (fn [[type _]]
                        [type (m/form (m/deref-all (m/schema [:ref type] {:registry malli-registry})))])
                      registry))
         event-type (clio-schema/event-schema event-type [:ref :calliope.session/record-v1])))
