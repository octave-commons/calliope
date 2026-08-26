(ns fork-tales.law.reconstruction
  "Pure-data Malli contracts for the audio reconstruction event ledger.

  Reconstruction is event-sourced on the same terms as corpus ingestion: every
  stage appends an immutable event to `ledgers/reconstruction.edn`, and every
  derived view (grades, coverage reports, candidate rankings) is a projection
  that can be thrown away and rebuilt.

  Two rules are encoded structurally rather than left to convention.

  Epistemic tier is mandatory on every event. `:observed` is measured from
  bytes, `:derived` is computed from observations, `:provisional` is proposed by
  a model or heuristic, `:accepted` requires an explicit human decision. A
  `:check/observed` event can never carry `:accepted` — that is what makes
  Gemma Check a linter instead of an authority (invariant μ6).

  Provenance is mandatory. Any event that names an artifact carries that
  artifact's `sha256`, so a projection can always be re-derived and any claim
  re-checked against the bytes it was made from.

  Definitions here contain no executable code; runtime adapters interpret them.")

;; ---------------------------------------------------------------------------
;; Vocabularies — closed sets, per the classifier convention
;; ---------------------------------------------------------------------------

(def event-types
  "Closed vocabulary of reconstruction event types, in pipeline order."
  [:reconstruction/run-started
   :evidence/preflighted
   :stem/separated
   :f0/extracted
   :notes/derived
   :alignment/derived
   :performance/authored
   :render/produced
   :check/observed
   :grade/scored
   :handoff/validated
   :review/decided
   :audit/decided
   :reconstruction/run-finished])

(def tiers
  "Epistemic tiers, ordered weakest to strongest. Never silently promoted."
  [:observed :derived :provisional :accepted])

(def verdicts
  [:accept :revise :reject])

(def lanes
  "Rubric domains a claim can be scored in."
  [:lyrics :rhythm :pitch :delivery :timbre :render-fidelity :arrangement])

;; ---------------------------------------------------------------------------
;; Shared shapes
;; ---------------------------------------------------------------------------

(def registry
  {:ft.rec/Tier      (into [:enum] tiers)
   :ft.rec/EventType (into [:enum] event-types)
   :ft.rec/Verdict   (into [:enum] verdicts)
   :ft.rec/Lane      (into [:enum] lanes)

   :ft.rec/Sha256 [:re #"^[0-9a-f]{64}$"]

   ;; An artifact reference is never a bare path. Bytes are identified by hash
   ;; so a claim survives the file being moved, renamed, or regenerated.
   :ft.rec/ArtifactRef
   [:map
    [:artifact/path :string]
    [:artifact/sha256 [:ref :ft.rec/Sha256]]
    [:artifact/bytes {:optional true} [:int {:min 0}]]
    [:artifact/role {:optional true} :keyword]]

   ;; Spans are structural. "the second chorus" is not a span; 138.4-161.9 is.
   :ft.rec/Span
   [:and
    [:map
     [:span/t0 {:optional true} [:double {:min 0.0}]]
     [:span/t1 {:optional true} [:double {:min 0.0}]]
     [:span/bars {:optional true} :string]
     [:span/section {:optional true} :string]]
    [:fn {:error/message "span needs either t0+t1, bars, or section"}
     '(fn [{:span/keys [t0 t1 bars section]}]
        (boolean (or (and t0 t1) bars section)))]]

   :ft.rec/Provenance
   [:map
    [:prov/tool :string]
    [:prov/version {:optional true} :string]
    [:prov/model {:optional true} :string]
    [:prov/params {:optional true} [:map-of :keyword :any]]
    [:prov/host-class {:optional true} :keyword]]

   ;; Every event carries these. `:event/tier` is not optional anywhere.
   :ft.rec/EventBase
   [:map
    [:event/id :uuid]
    [:event/type [:ref :ft.rec/EventType]]
    [:event/tier [:ref :ft.rec/Tier]]
    [:run/id :uuid]
    [:ts :string]
    [:case/id :string]
    [:note {:optional true} :string]]

   ;; -----------------------------------------------------------------------
   ;; Per-stage payloads
   ;; -----------------------------------------------------------------------

   :ft.rec/RunStarted
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:case/reference-root :string]
     [:objective :string]
     [:mode [:enum :scribe :composition]]]]

   ;; Whether a preserved artifact's embedded paths still resolve. Exists because
   ;; a grader fed unreachable evidence scores those features null and reports
   ;; lower coverage instead of failing, so a broken input imitates a weak
   ;; candidate. This event makes that difference explicit and auditable.
   :ft.rec/EvidencePreflighted
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:event/tier [:= :observed]]
     [:artifact [:ref :ft.rec/ArtifactRef]]
     [:ok? :boolean]
     [:total [:int {:min 0}]]
     [:counts [:map-of :keyword [:int {:min 0}]]]
     [:missing [:sequential :string]]
     [:translated {:optional true} [:sequential [:tuple :string :string]]]
     [:shadowed {:optional true} [:sequential [:tuple :string :string]]]
     [:rules-source :string]]]

   :ft.rec/StemSeparated
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:source [:ref :ft.rec/ArtifactRef]]
     [:stems [:sequential [:ref :ft.rec/ArtifactRef]]]
     [:separator :string]
     [:prov [:ref :ft.rec/Provenance]]]]

   :ft.rec/F0Extracted
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:source [:ref :ft.rec/ArtifactRef]]
     [:output [:ref :ft.rec/ArtifactRef]]
     [:frames [:int {:min 0}]]
     [:prov [:ref :ft.rec/Provenance]]]]

   ;; Coverage is first-class because low coverage is the failure that stalled
   ;; the vocal lane, and a note fixture that covers 11% of a track must not be
   ;; able to look like a success.
   :ft.rec/NotesDerived
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:source [:ref :ft.rec/ArtifactRef]]
     [:output [:ref :ft.rec/ArtifactRef]]
     [:note-count [:int {:min 0}]]
     [:coverage/covered-seconds [:double {:min 0.0}]]
     [:coverage/total-seconds [:double {:min 0.0}]]
     [:coverage/ratio [:double {:min 0.0 :max 1.0}]]
     [:coverage/gaps {:optional true} [:sequential [:ref :ft.rec/Span]]]
     [:prov [:ref :ft.rec/Provenance]]]]

   :ft.rec/AlignmentDerived
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:source [:ref :ft.rec/ArtifactRef]]
     [:output [:ref :ft.rec/ArtifactRef]]
     [:segments [:int {:min 0}]]
     ;; Timing scaffolding only. STT text is never the lyric source of truth.
     [:timing-only? [:= true]]
     [:prov [:ref :ft.rec/Provenance]]]]

   :ft.rec/PerformanceAuthored
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:output [:ref :ft.rec/ArtifactRef]]
     [:voicebank :string]
     [:version :string]
     [:kana-layer? :boolean]
     [:derived-from [:sequential [:ref :ft.rec/ArtifactRef]]]
     [:prov [:ref :ft.rec/Provenance]]]]

   :ft.rec/RenderProduced
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:output [:ref :ft.rec/ArtifactRef]]
     [:version :string]
     [:derived-from [:sequential [:ref :ft.rec/ArtifactRef]]]
     [:prov [:ref :ft.rec/Provenance]]]]

   ;; Bounded observations from the cheap local checker. Deliberately has no
   ;; verdict field: μ6 says this lane cannot accept work.
   :ft.rec/CheckObserved
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:event/tier [:enum :observed :provisional]]
     [:candidate [:ref :ft.rec/ArtifactRef]]
     [:reference {:optional true} [:ref :ft.rec/ArtifactRef]]
     [:observations [:sequential
                     [:map
                      [:lane [:ref :ft.rec/Lane]]
                      [:span {:optional true} [:ref :ft.rec/Span]]
                      [:observation :string]
                      [:confidence [:double {:min 0.0 :max 1.0}]]]]]
     [:next-tool-actions {:optional true} [:sequential :string]]
     [:prov [:ref :ft.rec/Provenance]]]]

   :ft.rec/GradeScored
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:event/tier [:= :derived]]
     [:candidate [:ref :ft.rec/ArtifactRef]]
     [:rubric [:ref :ft.rec/ArtifactRef]]
     [:scores [:sequential
               [:map
                [:lane [:ref :ft.rec/Lane]]
                [:score [:double {:min 0.0 :max 1.0}]]
                [:weight [:double {:min 0.0}]]
                [:basis :string]
                [:contested? {:optional true} :boolean]]]]
     [:weighted-total [:double {:min 0.0 :max 1.0}]]
     [:prov [:ref :ft.rec/Provenance]]]]

   :ft.rec/HandoffValidated
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:event/tier [:= :derived]]
     [:packets [:sequential [:ref :ft.rec/ArtifactRef]]]
     [:checked-specs [:sequential :string]]
     [:ok? :boolean]
     [:error-count [:int {:min 0}]]
     [:warning-count [:int {:min 0}]]
     [:errors [:sequential [:map
                            [:spec :string]
                            [:path :string]
                            [:message :string]]]]]]

   ;; The only lane that may write :accepted, and only with a steward.
   :ft.rec/ReviewDecided
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:candidate [:ref :ft.rec/ArtifactRef]]
     [:verdict [:ref :ft.rec/Verdict]]
     [:reasons [:sequential
                [:map
                 [:lane {:optional true} [:ref :ft.rec/Lane]]
                 [:span {:optional true} [:ref :ft.rec/Span]]
                 [:severity {:optional true} [:enum :blocking :major :minor :note]]
                 [:required-action :string]]]]
     [:role-signature :string]
     [:prov [:ref :ft.rec/Provenance]]]]

   :ft.rec/AuditDecided
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:candidate [:ref :ft.rec/ArtifactRef]]
     [:verdict [:ref :ft.rec/Verdict]]
     [:findings [:sequential
                 [:map
                  [:lane [:ref :ft.rec/Lane]]
                  [:span [:ref :ft.rec/Span]]
                  [:severity [:enum :blocking :major :minor :note]]
                  [:finding :string]]]]
     [:auditor :string]]]

   :ft.rec/RunFinished
   [:merge
    [:ref :ft.rec/EventBase]
    [:map
     [:events-appended [:int {:min 0}]]
     [:outcome [:enum :completed :abandoned :blocked]]
     [:blockers {:optional true} [:sequential :string]]]]

   :ft.rec/Event
   [:multi {:dispatch :event/type}
    [:reconstruction/run-started  [:ref :ft.rec/RunStarted]]
    [:evidence/preflighted        [:ref :ft.rec/EvidencePreflighted]]
    [:stem/separated              [:ref :ft.rec/StemSeparated]]
    [:f0/extracted                [:ref :ft.rec/F0Extracted]]
    [:notes/derived               [:ref :ft.rec/NotesDerived]]
    [:alignment/derived           [:ref :ft.rec/AlignmentDerived]]
    [:performance/authored        [:ref :ft.rec/PerformanceAuthored]]
    [:render/produced             [:ref :ft.rec/RenderProduced]]
    [:check/observed              [:ref :ft.rec/CheckObserved]]
    [:grade/scored                [:ref :ft.rec/GradeScored]]
    [:handoff/validated           [:ref :ft.rec/HandoffValidated]]
    [:review/decided              [:ref :ft.rec/ReviewDecided]]
    [:audit/decided               [:ref :ft.rec/AuditDecided]]
    [:reconstruction/run-finished [:ref :ft.rec/RunFinished]]]})

;; ---------------------------------------------------------------------------
;; Handoff invariants μ1-μ6 — data, not code
;; ---------------------------------------------------------------------------

(def handoff-invariants
  "The μ invariants from docs/reconstruction/operating-model.md, as data.

  `:applies-when` and `:requires` are interpreted by the validator runtime in
  scripts/reconstruction/validate.clj. Keeping them here means the law is
  inspectable and diffable, and the runner stays a thin interpreter."
  [{:mu :μ1
    :title "Accepted artifacts carry provenance, span, and unresolved issues"
    :applies-when {:handoff-kind #{"reference_catalog_entry" "final_release"}
                   :status #{"accept" "approved" "promoted" "final"}}
    :also-scans {:handoff-kind "qc_review" :field :accepted_artifacts}
    :requires [{:field :provenance :message "accepted artifact missing provenance"}
               {:field :source_span :as :span :message "accepted artifact missing source span"}
               {:field :unresolved_issues :as :present
                :message "accepted artifact must include unresolved_issues list, even if empty"}]
    :object-required "accepted artifact must be an object, not a bare path"}

   {:mu :μ2
    :title "Reviewer rejection requires an actionable required_action"
    :applies-when {:handoff-kind #{"qc_review"} :verdict #{"revise" "reject"}}
    :collection :reasons
    :empty-message "reviewer rejection/revision must include at least one reason"
    :requires [{:field :required_action :message "reason missing actionable required_action"}]
    :any-message "no actionable required_action found"}

   {:mu :μ3
    :title "Human rejection is structured by domain and span"
    :applies-when {:handoff-kind #{"human_audit"} :verdict #{"revise" "reject"}}
    :collection :findings
    :empty-message "human rejection/revision must include structured findings"
    :object-required "finding must be an object"
    :requires [{:field :domain :message "finding missing domain"}
               {:field :span :as :span :message "finding missing structured span"}
               {:field :severity :severity :warning :message "finding should include severity"}]}

   {:mu :μ4
    :title "Composition mode may only use approved references"
    :applies-when {:mode #{"composition"}}
    :reference-check true}

   {:mu :μ5
    :title "Restarts carry prior plan, failures, adjudication, and feedback"
    :applies-when {:handoff-kind #{"restart_packet"} :flag :is_restart}
    :requires [{:field :prior_plan :message "restart missing required context"}
               {:field :failed_artifacts :message "restart missing required context"}
               {:field :adjudication_report :message "restart missing required context"}
               {:field :review_feedback :message "restart missing required context"}]}

   {:mu :μ6
    :title "Gemma Check is a checker, never an acceptance authority"
    :applies-when {:role #{"gemma_check_subagent"} :handoff-kind #{"gemma_check_result"}}
    :forbids {:verdict #{"accept" "approve"}
              :handoff-kind #{"qc_review" "reference_catalog_entry" "final_release"}}}])
