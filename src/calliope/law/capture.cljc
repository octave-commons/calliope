(ns calliope.law.capture
  "Versioned plain-data capture contracts. No device, file or model effects."
  (:require [calliope.law.studio :as studio]
            [malli.core :as m]
            [malli.registry :as mr]))

(def max-sample 9007199254740991)

(def registry
  (merge
   studio/registry
   {:calliope.capture/nat-v1 [:int {:min 0 :max max-sample}]
    :calliope.capture/positive-v1 [:int {:min 1 :max max-sample}]
    :calliope.capture/uuid-v1
    [:re "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"]
    :calliope.capture/clock-v1
    [:map {:closed true}
     [:sample-rate [:int {:min 8000 :max 192000}]]
     [:ppq [:int {:min 1 :max 32767}]]
     [:tempo-us-per-quarter [:int {:min 1 :max 16777215}]]
     [:sample-origin [:ref :calliope.capture/nat-v1]]
     [:rounding [:= :floor]]
     [:mapping/id [:= :ppq-samples-v1]]]
    :calliope.capture/midi-body-v1
    [:or
     [:map {:closed true} [:kind [:= :note-on]] [:channel [:int {:min 0 :max 15}]]
      [:pitch [:int {:min 0 :max 127}]] [:velocity [:int {:min 0 :max 127}]]]
     [:map {:closed true} [:kind [:= :note-off]] [:channel [:int {:min 0 :max 15}]]
      [:pitch [:int {:min 0 :max 127}]] [:velocity [:int {:min 0 :max 127}]]]
     [:map {:closed true} [:kind [:= :control-change]] [:channel [:int {:min 0 :max 15}]]
      [:controller [:int {:min 0 :max 127}]] [:value [:int {:min 0 :max 127}]]]]
    :calliope.capture/packet-v1
    [:map {:closed true}
     [:packet/seq [:ref :calliope.capture/nat-v1]]
     [:device/id [:ref :calliope.studio/non-blank-string]]
     [:device/epoch [:ref :calliope.capture/nat-v1]]
     [:raw/tick [:ref :calliope.capture/nat-v1]]
     [:midi [:ref :calliope.capture/midi-body-v1]]]
    :calliope.capture/disconnect-v1
    [:map {:closed true} [:kind [:= :disconnect]]
     [:device/id [:ref :calliope.studio/non-blank-string]]
     [:device/epoch [:ref :calliope.capture/nat-v1]]
     [:sample/offset [:ref :calliope.capture/nat-v1]]]
    :calliope.capture/reconnect-v1
    [:map {:closed true} [:kind [:= :reconnect]]
     [:device/id [:ref :calliope.studio/non-blank-string]]
     [:device/epoch [:ref :calliope.capture/nat-v1]]
     [:sample-origin [:ref :calliope.capture/nat-v1]]]
    :calliope.capture/stop-v1
    [:map {:closed true} [:kind [:= :stop]]
     [:sample/offset [:ref :calliope.capture/nat-v1]]]
    :calliope.capture/staged-source-v1
    [:map {:closed true}
     [:filename [:ref :calliope.studio/non-blank-string]]
     [:sha256 [:ref :calliope.studio/sha256-v1]]]
    :calliope.capture/ingest-command-v1
    [:map {:closed true}
     [:command/type [:= :studio/ingest]] [:command/version [:= 1]]
     [:request/id [:ref :calliope.capture/uuid-v1]]
     [:session/id [:ref :calliope.capture/uuid-v1]]
     [:session/revision [:ref :calliope.capture/nat-v1]]
     [:audio [:ref :calliope.capture/staged-source-v1]]
     [:midi [:ref :calliope.capture/staged-source-v1]]]
    :calliope.capture/origin-v1
    [:map {:closed true} [:kind [:= :synthetic]]
     [:generator/id [:= :studio-fixture-v1]]
     [:seed [:ref :calliope.capture/nat-v1]]
     [:channel-roles [:tuple [:= :guitar-fixture] [:= :voice-fixture]]]]
    :calliope.capture/audio-v1
    [:map {:closed true}
     [:render/id [:ref :calliope.capture/uuid-v1]]
     [:sha256 [:ref :calliope.studio/sha256-v1]] [:format [:= :wav]]
     [:sample-rate [:int {:min 8000 :max 192000}]]
     [:channels [:int {:min 1 :max 2}]]
     [:frames [:ref :calliope.capture/positive-v1]]]
    :calliope.capture/midi-source-v1
    [:map {:closed true}
     [:source/id [:ref :calliope.capture/uuid-v1]]
     [:sha256 [:ref :calliope.studio/sha256-v1]] [:format [:= :smf]]]
    :calliope.capture/take-v1
    [:map {:closed true}
     [:take/id [:ref :calliope.capture/uuid-v1]]
     [:take/session-id [:ref :calliope.capture/uuid-v1]]
     [:take/audio [:ref :calliope.capture/audio-v1]]
     [:take/midi [:ref :calliope.capture/midi-source-v1]]
     [:take/clock [:ref :calliope.capture/clock-v1]]
     [:take/events [:vector :map]] [:take/notes [:vector :map]]
     [:take/interruptions [:vector :map]]
     [:take/origin [:ref :calliope.capture/origin-v1]]]
    :calliope.capture/input-v1
    [:or [:ref :calliope.capture/packet-v1]
     [:ref :calliope.capture/disconnect-v1]
     [:ref :calliope.capture/reconnect-v1]
     [:ref :calliope.capture/stop-v1]]}))

(def malli-registry (mr/composite-registry m/default-registry registry))
(def validator
  (memoize (fn [type] (m/validator [:ref type] {:registry malli-registry}))))

(defn valid? [type value]
  (boolean (and (contains? registry type) ((validator type) value))))
