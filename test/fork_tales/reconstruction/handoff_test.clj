(ns fork-tales.reconstruction.handoff-test
  "Tests for the μ1-μ6 handoff invariant interpreter.

  Packets are written string-keyed, the way they arrive from JSON, since that is
  the shape the retired Python tool consumed. A keyword-keyed case is included
  to prove EDN and JSON packets validate identically."
  (:require [clojure.test :refer [deftest is testing]]
            [fork-tales.law.reconstruction :as law]
            [fork-tales.reconstruction.handoff :as h]))

(def schema
  {"common_required_fields" ["handoff_kind" "role"]
   "handoff_kinds" ["reference_catalog_entry" "qc_review" "human_audit"
                    "restart_packet" "gemma_check_result" "final_release"
                    "transcription_result" "composition_draft"]
   "modes" ["scribe" "composition"]
   "roles" ["planner_agent" "transcriber" "producer" "qc_reviewer_agent"
            "human_auditor" "gemma_check_subagent"]
   "schemas" {"transcription_result" {"required" ["artifacts"]}}})

(defn- report [packet]
  (h/report [["p[0]" packet]] schema #{}))

(defn- errors-for [packet spec]
  (filterv #(= spec (:spec %)) (:errors (report packet))))

(defn- specs [packet] (set (:checked_specs (report packet))))

;; ---------------------------------------------------------------------------

(deftest law-is-data
  (testing "every invariant carries an id and a title"
    (is (= 6 (count law/handoff-invariants)))
    (is (every? :mu law/handoff-invariants))
    (is (every? :title law/handoff-invariants)))
  (testing "event vocabulary is closed and tiers are ordered"
    (is (= [:observed :derived :provisional :accepted] law/tiers))
    (is (contains? (set law/event-types) :handoff/validated))))

(deftest predicates
  (testing "nonempty? treats blank strings and empty colls as absent"
    (is (not (h/nonempty? "   ")))
    (is (not (h/nonempty? [])))
    (is (not (h/nonempty? nil)))
    (is (h/nonempty? "x"))
    (is (h/nonempty? false) "false is a present value, not an absent one"))
  (testing "has-span? accepts t0+t1, bars, or section — and nothing else"
    (is (h/has-span? {"t0" 1.0 "t1" 2.0}))
    (is (h/has-span? {:t0 1.0 :t1 2.0}))
    (is (h/has-span? {"bars" "17-24"}))
    (is (h/has-span? {"section" "final chorus"}))
    (is (not (h/has-span? {"t0" 1.0})) "half a time span is not a span")
    (is (not (h/has-span? {})))
    (is (not (h/has-span? "138.4-161.9")) "a string is not a structured span")))

(deftest common-vocabulary
  (testing "unknown closed-vocabulary values are rejected"
    (let [errs (errors-for {"handoff_kind" "telepathy" "role" "planner_agent"} "common")]
      (is (some #(= "p[0].handoff_kind" (:path %)) errs))))
  (testing "missing common required fields are reported"
    (is (seq (errors-for {"role" "planner_agent"} "common"))))
  (testing "per-kind required fields apply"
    (is (seq (errors-for {"handoff_kind" "transcription_result" "role" "transcriber"}
                         "kind-required")))))

(deftest mu1-accepted-artifacts
  (testing "an accepted artifact needs provenance, span, and unresolved_issues"
    (let [errs (errors-for {"handoff_kind" "reference_catalog_entry"
                            "role" "planner_agent"
                            "artifacts" [{"path" "a.ustx"}]}
                           "μ1")]
      (is (= 3 (count errs)))
      (is (= #{"p[0].artifacts[0].provenance"
               "p[0].artifacts[0].source_span"
               "p[0].artifacts[0].unresolved_issues"}
             (set (map :path errs))))))
  (testing "an empty unresolved_issues list satisfies μ1 — presence, not content"
    (is (empty? (errors-for {"handoff_kind" "reference_catalog_entry"
                             "role" "planner_agent"
                             "artifacts" [{"provenance" "demucs v4"
                                           "source_span" {"t0" 0.0 "t1" 12.0}
                                           "unresolved_issues" []}]}
                            "μ1"))))
  (testing "a bare path is not an artifact"
    (let [errs (errors-for {"handoff_kind" "final_release"
                            "role" "planner_agent"
                            "artifacts" ["renders/v16.wav"]}
                           "μ1")]
      (is (= 1 (count errs)))
      (is (re-find #"not a bare path" (:message (first errs))))))
  (testing "μ1 also scans accepted_artifacts on a qc_review"
    (is (seq (errors-for {"handoff_kind" "qc_review" "role" "qc_reviewer_agent"
                          "verdict" "accept"
                          "accepted_artifacts" [{"path" "x"}]}
                         "μ1"))))
  (testing "status alone can trigger μ1"
    (is (seq (errors-for {"handoff_kind" "transcription_result" "role" "transcriber"
                          "status" "approved"
                          "artifacts" [{"path" "x"}]}
                         "μ1")))))

(deftest mu2-reviewer-must-be-actionable
  (testing "a rejecting review with no reasons fails"
    (let [errs (errors-for {"handoff_kind" "qc_review" "role" "qc_reviewer_agent"
                            "verdict" "reject"}
                           "μ2")]
      (is (= 1 (count errs)))
      (is (re-find #"at least one reason" (:message (first errs))))))
  (testing "a reason without required_action fails"
    (is (seq (errors-for {"handoff_kind" "qc_review" "role" "qc_reviewer_agent"
                          "verdict" "revise"
                          "reasons" [{"note" "sounds off"}]}
                         "μ2"))))
  (testing "an actionable reason passes"
    (is (empty? (errors-for {"handoff_kind" "qc_review" "role" "qc_reviewer_agent"
                             "verdict" "revise"
                             "reasons" [{"required_action" "re-derive notes with lyric anchoring"}]}
                            "μ2"))))
  (testing "an accepting review does not trigger μ2 at all"
    (is (not (contains? (specs {"handoff_kind" "qc_review" "role" "qc_reviewer_agent"
                                "verdict" "accept"})
                        "μ2")))))

(deftest mu3-human-findings-are-structured
  (testing "findings need domain and a structured span"
    (let [errs (errors-for {"handoff_kind" "human_audit" "role" "human_auditor"
                            "verdict" "reject"
                            "findings" [{"finding" "outro is empty"}]}
                           "μ3")]
      (is (= #{"p[0].findings[0].domain" "p[0].findings[0].span"}
             (set (map :path errs))))))
  (testing "missing severity is a warning, not an error"
    (let [r (report {"handoff_kind" "human_audit" "role" "human_auditor"
                     "verdict" "reject"
                     "findings" [{"domain" "pitch" "span" {"t0" 261.4 "t1" 303.3}}]})]
      (is (:ok r))
      (is (= 1 (:warning_count r))))))

(deftest mu4-composition-references
  (testing "an unapproved reference is rejected in composition mode"
    (let [r (h/report [["p[0]" {"handoff_kind" "composition_draft"
                                "role" "producer" "mode" "composition"
                                "references" ["ref-unknown"]}]]
                      schema #{})]
      (is (not (:ok r)))
      (is (re-find #"is not approved" (-> r :errors first :message)))))
  (testing "an id in the approved catalog passes"
    (is (:ok (h/report [["p[0]" {"handoff_kind" "composition_draft"
                                 "role" "producer" "mode" "composition"
                                 "references" ["ref-heresy"]}]]
                       schema #{"ref-heresy"}))))
  (testing "an inline approved status passes"
    (is (:ok (h/report [["p[0]" {"handoff_kind" "composition_draft"
                                 "role" "producer" "mode" "composition"
                                 "references" [{"reference_id" "r1" "approval_status" "approved"}]}]]
                       schema #{}))))
  (testing "exploratory references warn rather than fail"
    (let [r (h/report [["p[0]" {"handoff_kind" "composition_draft"
                                "role" "producer" "mode" "composition"
                                "references" [{"reference_id" "r2" "exploratory" true}]}]]
                      schema #{})]
      (is (:ok r))
      (is (= 1 (:warning_count r)))))
  (testing "references nested under inputs and constraints are also checked"
    (is (not (:ok (h/report [["p[0]" {"handoff_kind" "composition_draft"
                                      "role" "producer" "mode" "composition"
                                      "inputs" {"references" ["nested-bad"]}}]]
                            schema #{})))))
  (testing "scribe mode does not trigger μ4"
    (is (not (contains? (specs {"handoff_kind" "composition_draft"
                                "role" "transcriber" "mode" "scribe"
                                "references" ["whatever"]})
                        "μ4")))))

(deftest mu5-restarts-carry-context
  (testing "a restart missing all four context fields reports four errors"
    (is (= 4 (count (errors-for {"handoff_kind" "restart_packet" "role" "planner_agent"}
                                "μ5")))))
  (testing "the is_restart flag triggers μ5 on any kind"
    (is (seq (errors-for {"handoff_kind" "composition_draft" "role" "transcriber"
                          "is_restart" true}
                         "μ5"))))
  (testing "a complete restart passes"
    (is (empty? (errors-for {"handoff_kind" "restart_packet" "role" "planner_agent"
                             "prior_plan" "plan-v1"
                             "failed_artifacts" ["v15.wav"]
                             "adjudication_report" "grade-v15.json"
                             "review_feedback" "coverage too low"}
                            "μ5")))))

(deftest mu6-gemma-is-not-an-authority
  (testing "gemma cannot accept"
    (is (seq (errors-for {"handoff_kind" "gemma_check_result"
                          "role" "gemma_check_subagent" "verdict" "accept"}
                         "μ6"))))
  (testing "gemma cannot emit a qc_review"
    (let [errs (errors-for {"handoff_kind" "qc_review" "role" "gemma_check_subagent"}
                           "μ6")]
      (is (some #(re-find #"cannot emit" (:message %)) errs))))
  (testing "a bounded gemma observation is fine"
    (is (empty? (errors-for {"handoff_kind" "gemma_check_result"
                             "role" "gemma_check_subagent" "verdict" "revise"}
                            "μ6"))))
  (testing "the law forbids :accepted on a check event"
    (let [check (get (:ft.rec/CheckObserved law/registry) 0)]
      (is (= :merge check) "CheckObserved is a merge over EventBase"))
    (is (= [:enum :observed :provisional]
           (-> law/registry :ft.rec/CheckObserved (nth 2) (nth 1) (nth 1)))
        "check events are structurally barred from :accepted")))

(deftest json-and-edn-agree
  (testing "keyword-keyed EDN packets validate identically to string-keyed JSON"
    (let [json-packet {"handoff_kind" "reference_catalog_entry" "role" "planner_agent"
                       "artifacts" [{"path" "a.ustx"}]}
          edn-packet {:handoff_kind "reference_catalog_entry" :role "planner_agent"
                      :artifacts [{:path "a.ustx"}]}]
      (is (= (:error_count (report json-packet))
             (:error_count (report edn-packet))))
      (is (= (set (map :path (:errors (report json-packet))))
             (set (map :path (:errors (report edn-packet)))))))))

(deftest report-shape-matches-retired-tool
  (let [r (report {"handoff_kind" "qc_review" "role" "qc_reviewer_agent" "verdict" "accept"})]
    (is (= "fork-tales-handoff-validation-report/v1" (:schema_version r)))
    (is (every? #(contains? r %)
                [:ok :checked_specs :error_count :warning_count :errors :warnings]))
    (is (vector? (:checked_specs r)))
    (is (= (:checked_specs r) (vec (sort (:checked_specs r)))) "specs are sorted")))
