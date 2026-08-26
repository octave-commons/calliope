(ns calliope.law.media
  "Malli contracts for the manifest-addressed Calliope media dataset."
  (:require [malli.core :as m]
            [malli.registry :as mr]))

(def registry
  {:calliope.media/manifest-envelope-v1
   [:map {:closed true}
    [:dataset/id :string]
    [:schema [:= :calliope.media/manifest-v1]]
    [:entries [:int {:min 1}]]
    [:bytes-total [:int {:min 0}]]
    [:generated :string]]

   :calliope.media/manifest-entry-v1
   [:map {:closed true}
    [:path [:and [:string {:min 1}] [:re #"^[^/\s]+/[^/\s]+\.(mp3|jpeg)$"]]]
    [:bytes [:int {:min 1}]]
    [:sha256 [:and :string [:re #"^[0-9a-f]{64}$"]]]]

   :calliope.media/manifest-v1
   [:map {:closed true}
    [:dataset/id :string]
    [:schema [:= :calliope.media/manifest-v1]]
    [:entries [:vector [:ref :calliope.media/manifest-entry-v1]]]
    [:bytes-total {:optional true} [:int {:min 0}]]
    [:generated :string]]})

(def malli-registry
  (mr/composite-registry m/default-registry registry))

(defn schema
  "Resolve a named media dataset contract."
  [schema-key]
  (m/schema [:ref schema-key] {:registry malli-registry}))

(defn valid?
  "Does a parsed media manifest satisfy its contract?"
  ([value]
   (valid? :calliope.media/manifest-v1 value))
  ([schema-key value]
   (m/validate (schema schema-key) value)))

(defn explain
  "Return Malli explain data for an invalid parsed media manifest."
  ([value]
   (explain :calliope.media/manifest-v1 value))
  ([schema-key value]
   (m/explain (schema schema-key) value)))

(defn decode-manifest
  "Return a validated parsed manifest, or Malli explain data on failure."
  [value]
  (if (valid? value) value (explain value)))
