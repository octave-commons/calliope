(ns calliope.law.desktop
  "Plain-data one-shot desktop profile boundary. No caller command language."
  (:require [calliope.law.studio :as studio]
            [malli.core :as m]
            [malli.registry :as mr]))

(def registry
  (merge studio/registry
         {:calliope.desktop/id-v1 [:int {:min 1 :max 9007199254740991}]
          :calliope.desktop/coordinate-v1 [:int {:min -2147483648 :max 2147483647}]
          :calliope.desktop/size-v1 [:int {:min 1 :max 65535}]
          :calliope.desktop/rect-v1
          [:map {:closed true}
           [:x [:ref :calliope.desktop/coordinate-v1]] [:y [:ref :calliope.desktop/coordinate-v1]]
           [:width [:ref :calliope.desktop/size-v1]] [:height [:ref :calliope.desktop/size-v1]]]
          :calliope.desktop/window-v1
          [:map {:closed true}
           [:id [:ref :calliope.desktop/id-v1]] [:workspace [:ref :calliope.desktop/id-v1]]
           [:rect [:ref :calliope.desktop/rect-v1]] [:floating? :boolean] [:fullscreen? :boolean]]
          :calliope.desktop/workspace-v1
          [:map {:closed true} [:id [:ref :calliope.desktop/id-v1]]
           [:rect [:ref :calliope.desktop/rect-v1]]]
          :calliope.desktop/snapshot-v1
          [:map {:closed true}
           [:revision [:ref :calliope.studio/sha256-v1]]
           [:focus [:ref :calliope.desktop/id-v1]] [:workspace [:ref :calliope.desktop/workspace-v1]]
           [:windows [:map-of [:ref :calliope.desktop/id-v1] [:ref :calliope.desktop/window-v1]]]]
          :calliope.desktop/profile-v1
          [:map {:closed true} [:profile/type [:= :music-v1]]
           [:studio/id [:ref :calliope.desktop/id-v1]]
           [:daw/id {:optional true} [:ref :calliope.desktop/id-v1]]
           [:opt-outs {:optional true} [:set [:ref :calliope.desktop/id-v1]]]]
          :calliope.desktop/operation-v1
          [:or
           [:map {:closed true} [:op [:= :floating-enable]] [:id [:ref :calliope.desktop/id-v1]]]
           [:map {:closed true} [:op [:= :resize]] [:id [:ref :calliope.desktop/id-v1]]
            [:width [:ref :calliope.desktop/size-v1]] [:height [:ref :calliope.desktop/size-v1]]]
           [:map {:closed true} [:op [:= :move]] [:id [:ref :calliope.desktop/id-v1]]
            [:x [:ref :calliope.desktop/coordinate-v1]] [:y [:ref :calliope.desktop/coordinate-v1]]]]}))

(def malli-registry (mr/composite-registry m/default-registry registry))
(def validator (memoize #(m/validator [:ref %] {:registry malli-registry})))
(defn valid? [type value]
  (boolean (and (contains? registry type) ((validator type) value))))
