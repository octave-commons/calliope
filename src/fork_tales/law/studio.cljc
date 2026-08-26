(ns fork-tales.law.studio
  "Versioned Malli contracts for the media workbench domain (FT-000B).

  Governing authority: ADR-001 and docs/designs/media-workbench-v1.md.

  Work, render, clip, arrangement, and export are distinct object types and
  are never collapsed into one another. A render is immutable source audio; a
  clip is a non-destructive span of exactly one render; an arrangement is an
  ordered edit decision list; an export is a rebuildable derivative. Ratings
  are multidimensional and scoped to one subject. Playlists, smart lists, and
  workspaces are distinct organization objects. Release and publication state
  is tracked per target, and no contract here carries credentials.")

(def schemas
  {;; ------------------------------------------------------------ primitives
   "studio/non-empty-string" [:string {:min 1}]

   "studio/positive-int" [:int {:min 1}]

   "studio/nat-int" [:int {:min 0}]

   "studio/object-id-v1" [:or :uuid [:string {:min 1}]]

   "studio/sha256-v1" [:re {:error/message "64 lowercase hex chars"}
                       #"[0-9a-f]{64}"]

   "studio/iso-instant-v1" [:string {:min 1}]

   "studio/actor-v1"
   [:map {:closed true}
    [:actor/id [:ref "studio/non-empty-string"]]
    [:actor/kind [:enum :user :agent :classifier :system]]]

   ;; ------------------------------------------------------- object identity
   ;; ADR-001 section 2: the playable abstraction is broader than a track
   ;; file. Works are conceptual and are NOT playable refs.
   "object/type-v1" [:enum :work :render :clip :arrangement :export]

   "playable/type-v1" [:enum :render :clip :arrangement :export]

   "playable/ref-v1"
   [:map {:closed true}
    [:ref/type [:ref "playable/type-v1"]]
    [:ref/id [:ref "studio/object-id-v1"]]]

   "studio/subject-ref-v1"
   [:map {:closed true}
    [:subject/type [:ref "object/type-v1"]]
    [:subject/id [:ref "studio/object-id-v1"]]]

   ;; ------------------------------------------------------------------ work
   "work/v1"
   [:map {:closed true}
    [:work/id [:ref "studio/object-id-v1"]]
    [:work/title [:ref "studio/non-empty-string"]]
    [:work/notes {:optional true} :string]]

   ;; ---------------------------------------------------------------- render
   ;; Immutable source audio. Content hash is part of identity.
   "render/source-v1"
   [:map {:closed true}
    [:source/renderer [:ref "studio/non-empty-string"]]
    [:source/renderer-clip-id {:optional true} [:ref "studio/non-empty-string"]]
    [:source/uri {:optional true} [:ref "studio/non-empty-string"]]]

   "render/v1"
   [:map {:closed true}
    [:render/id [:ref "studio/object-id-v1"]]
    [:render/work-id [:ref "studio/object-id-v1"]]
    [:render/sha256 [:ref "studio/sha256-v1"]]
    [:render/duration-ms [:ref "studio/positive-int"]]
    [:render/codec [:ref "studio/non-empty-string"]]
    [:render/source [:ref "render/source-v1"]]]

   ;; ------------------------------------------------------------- time math
   "studio/time-range-v1"
   [:and
    [:map {:closed true}
     [:start-ms [:ref "studio/nat-int"]]
     [:end-ms [:ref "studio/positive-int"]]]
    [:fn {:error/message "end-ms must exceed start-ms"}
     (fn [{:keys [start-ms end-ms]}] (> end-ms start-ms))]]

   "clip/fades-v1"
   [:and
    [:map {:closed true}
     [:in-ms [:ref "studio/nat-int"]]
     [:out-ms [:ref "studio/nat-int"]]]
    [:fn {:error/message "fade durations must fit the clip range"}
     (fn [{:keys [in-ms out-ms]}] (and (nat-int? in-ms) (nat-int? out-ms)))]]

   ;; ---------------------------------------------------------------- marker
   ;; Markers annotate renders or arrangements, never clips or works. Model
   ;; markers stay derived/provisional; only explicit durable decisions are
   ;; accepted (ADR-001 section 6).
   "marker/subject-v1"
   [:map {:closed true}
    [:subject/type [:enum :render :arrangement]]
    [:subject/id [:ref "studio/object-id-v1"]]]

   "marker/epistemic-v1" [:enum :observed :derived :provisional :accepted]

   "marker/v1"
   [:map {:closed true}
    [:marker/id [:ref "studio/object-id-v1"]]
    [:marker/subject [:ref "marker/subject-v1"]]
    [:marker/kind :keyword]
    [:marker/at-ms {:optional true} [:ref "studio/nat-int"]]
    [:marker/range {:optional true} [:ref "studio/time-range-v1"]]
    [:marker/epistemic [:ref "marker/epistemic-v1"]]
    [:marker/created-by [:ref "studio/actor-v1"]]
    [:marker/created-at [:ref "studio/iso-instant-v1"]]]

   ;; ------------------------------------------------------------------ clip
   ;; Non-destructive playable span of exactly one immutable render.
   "clip/source-v1"
   [:map {:closed true}
    [:source/type [:= :render]]
    [:source/id [:ref "studio/object-id-v1"]]
    [:source/sha256 [:ref "studio/sha256-v1"]]]

   "clip/status-v1" [:enum :provisional :accepted :rejected]

   "clip/v1"
   [:map {:closed true}
    [:clip/id [:ref "studio/object-id-v1"]]
    [:clip/source [:ref "clip/source-v1"]]
    [:clip/range [:ref "studio/time-range-v1"]]
    [:clip/fades {:optional true} [:ref "clip/fades-v1"]]
    [:clip/gain-db {:optional true} :double]
    [:clip/title {:optional true} [:ref "studio/non-empty-string"]]
    [:clip/status [:ref "clip/status-v1"]]
    [:clip/created-by [:ref "studio/actor-v1"]]
    [:clip/created-at [:ref "studio/iso-instant-v1"]]]

   ;; ------------------------------------------------------------ arrangement
   "transition/kind-v1" [:enum :cut :crossfade :gap]

   "arrangement/transition-v1"
   [:map {:closed true}
    [:transition/kind [:ref "transition/kind-v1"]]
    [:transition/duration-ms [:ref "studio/nat-int"]]]

   "arrangement/entry-v1"
   [:map {:closed true}
    [:entry/clip-id [:ref "studio/object-id-v1"]]
    [:entry/position [:ref "studio/nat-int"]]
    [:entry/transition {:optional true} [:ref "arrangement/transition-v1"]]]

   "arrangement/v1"
   [:map {:closed true}
    [:arrangement/id [:ref "studio/object-id-v1"]]
    [:arrangement/entries
     [:vector {:min 1} [:ref "arrangement/entry-v1"]]]
    [:arrangement/cross-work? :boolean]
    [:arrangement/version [:ref "studio/positive-int"]]
    [:arrangement/created-by [:ref "studio/actor-v1"]]
    [:arrangement/created-at [:ref "studio/iso-instant-v1"]]]

   ;; ---------------------------------------------------------------- export
   ;; Materialized derivative; preserves the complete derivation chain.
   "export/encoding-v1"
   [:map {:closed true}
    [:encoding/format [:enum :wav :flac :mp3 :video]]
    [:encoding/settings :map]]

   "export/v1"
   [:map {:closed true}
    [:export/id [:ref "studio/object-id-v1"]]
    [:export/arrangement-id [:ref "studio/object-id-v1"]]
    [:export/arrangement-version [:ref "studio/positive-int"]]
    [:export/source-hashes
     [:vector {:min 1} [:ref "studio/sha256-v1"]]]
    [:export/renderer [:ref "studio/non-empty-string"]]
    [:export/encoding [:ref "export/encoding-v1"]]
    [:export/output-sha256 [:ref "studio/sha256-v1"]]
    [:export/created-at [:ref "studio/iso-instant-v1"]]]

   ;; ---------------------------------------------------------------- rating
   ;; ADR-001 section 4: multidimensional and scoped. A clip-level rating is
   ;; not a render-level rating; the subject type is part of the contract.
   "rating/dimension-v1"
   [:enum :enjoyment :publishability :weirdness :salvageability
    :technical-quality]

   "rating/scale-v1" [:enum :zero-to-five]

   "rating/v1"
   [:map {:closed true}
    [:rating/id [:ref "studio/object-id-v1"]]
    [:rating/subject [:ref "studio/subject-ref-v1"]]
    [:rating/dimension [:ref "rating/dimension-v1"]]
    [:rating/scale [:ref "rating/scale-v1"]]
    [:rating/value [:int {:min 0 :max 5}]]
    [:rating/actor [:ref "studio/actor-v1"]]
    [:rating/at [:ref "studio/iso-instant-v1"]]]

   ;; ------------------------------------------------------------ disposition
   "disposition/v1" [:enum :keeper :salvage :reject :unreviewed]

   ;; ----------------------------------------------------------------- label
   ;; User-authored, namespace-capable: mood/nocturnal, issue/late-collapse.
   "label/v1" [:re {:error/message "namespace/name, lowercase"}
               #"^[a-z0-9][a-z0-9-]*/[a-z0-9][a-z0-9-]*$"]

   ;; -------------------------------------------------------------- playlist
   ;; Ordered durable sequence of playable refs (ADR-001 section 5).
   "playlist/entry-v1"
   [:map {:closed true}
    [:entry/ref [:ref "playable/ref-v1"]]
    [:entry/position [:ref "studio/nat-int"]]]

   "playlist/v1"
   [:map {:closed true}
    [:playlist/id [:ref "studio/object-id-v1"]]
    [:playlist/title [:ref "studio/non-empty-string"]]
    [:playlist/entries [:vector [:ref "playlist/entry-v1"]]]
    [:playlist/created-by [:ref "studio/actor-v1"]]
    [:playlist/created-at [:ref "studio/iso-instant-v1"]]]

   ;; ------------------------------------------------------------ smart list
   ;; Saved closed query evaluated against current projections; membership is
   ;; never frozen into the list (design: smart lists).
   "smart-list/where-v1"
   [:or
    [:tuple [:= :eq] :keyword :any]
    [:tuple [:= :not-eq] :keyword :any]
    [:tuple [:= :gte] :keyword :any]
    [:tuple [:= :lte] :keyword :any]
    [:tuple [:= :contains] :keyword :any]
    [:tuple [:= :not] [:ref "smart-list/where-v1"]]
    [:cat [:= :and] [:+ [:schema [:ref "smart-list/where-v1"]]]]
    [:cat [:= :or] [:+ [:schema [:ref "smart-list/where-v1"]]]]]

   "smart-list/sort-v1"
   [:vector
    [:tuple :keyword [:enum :asc :desc]]]

   "smart-list/query-v1"
   [:map {:closed true}
    [:where [:ref "smart-list/where-v1"]]
    [:sort {:optional true} [:ref "smart-list/sort-v1"]]]

   "smart-list/v1"
   [:map {:closed true}
    [:smart-list/id [:ref "studio/object-id-v1"]]
    [:smart-list/title [:ref "studio/non-empty-string"]]
    [:smart-list/query [:ref "smart-list/query-v1"]]
    [:smart-list/created-by [:ref "studio/actor-v1"]]
    [:smart-list/created-at [:ref "studio/iso-instant-v1"]]]

   ;; -------------------------------------------------------------- workspace
   ;; Saved attention context; may contain playlists and smart lists but is
   ;; reducible to neither (ADR-001 section 5).
   "workspace/focus-v1"
   [:map {:closed true}
    [:focus/kind [:enum :arrangement :release :comparison]]
    [:focus/id [:ref "studio/object-id-v1"]]]

   "workspace/v1"
   [:map {:closed true}
    [:workspace/id [:ref "studio/object-id-v1"]]
    [:workspace/title [:ref "studio/non-empty-string"]]
    [:workspace/filters {:optional true} [:ref "smart-list/where-v1"]]
    [:workspace/pinned {:optional true} [:vector [:ref "playable/ref-v1"]]]
    [:workspace/queue {:optional true} [:vector [:ref "playable/ref-v1"]]]
    [:workspace/notes {:optional true} :string]
    [:workspace/focus {:optional true} [:ref "workspace/focus-v1"]]
    [:workspace/created-by [:ref "studio/actor-v1"]]
    [:workspace/created-at [:ref "studio/iso-instant-v1"]]]

   ;; ----------------------------------------------- release and publication
   ;; ADR-001 sections 7-8: release is local before publication; targets
   ;; declare capabilities; per-target state machines are independent; no
   ;; credentials in durable records.
   "target/capability-v1"
   [:enum :direct-upload :resumable-upload :metadata-sync :export-package
    :manual-handoff :distributor-handoff]

   "publication-target/v1"
   [:map {:closed true}
    [:target/id :keyword]
    [:target/capabilities
     [:set {:min 1} [:ref "target/capability-v1"]]]]

   "publication/state-v1"
   [:enum :planned :validating :rendering :ready :authenticating :uploading
    :processing :published :failed :unavailable :cancelled
    :manual-action-required]

   "publication/attempt-v1"
   [:map {:closed true}
    [:attempt/id [:ref "studio/object-id-v1"]]
    [:attempt/at [:ref "studio/iso-instant-v1"]]
    [:attempt/state [:ref "publication/state-v1"]]
    [:attempt/checkpoint {:optional true} :map]
    [:attempt/external-url {:optional true} [:ref "studio/non-empty-string"]]
    [:attempt/summary {:optional true} :string]]

   "release/target-state-v1"
   [:map {:closed true}
    [:target/id :keyword]
    [:target/state [:ref "publication/state-v1"]]
    [:target/attempts {:optional true}
     [:vector [:ref "publication/attempt-v1"]]]]

   "release/type-v1" [:enum :single :ep :album :compilation]

   "release/acceptance-v1" [:enum :draft :candidate :accepted]

   "release/v1"
   [:map {:closed true}
    [:release/id [:ref "studio/object-id-v1"]]
    [:release/title [:ref "studio/non-empty-string"]]
    [:release/type [:ref "release/type-v1"]]
    [:release/assets [:vector {:min 1} [:ref "playable/ref-v1"]]]
    [:release/artwork {:optional true} :map]
    [:release/credits {:optional true} [:vector :map]]
    [:release/rights-basis [:ref "studio/non-empty-string"]]
    [:release/provenance
     [:map {:closed true}
      [:provenance/source-hashes
       [:vector {:min 1} [:ref "studio/sha256-v1"]]]]]
    [:release/targets [:vector [:ref "release/target-state-v1"]]]
    [:release/acceptance [:ref "release/acceptance-v1"]]]})
