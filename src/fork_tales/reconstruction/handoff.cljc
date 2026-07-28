(ns fork-tales.reconstruction.handoff
  "Pure interpreter for the μ1-μ6 handoff invariants.

  Port of the retired scripts/fork_tales_handoff_validate.py. The invariants
  themselves are not restated here — they are read from
  fork-tales.law.reconstruction/handoff-invariants, so adding or tightening an
  invariant is a data edit, not a code edit.

  No IO, no JSON, no ledger, no bb-only requires: this namespace is loadable and
  testable from both babashka and the JVM. Packets arrive already parsed, and
  may be string-keyed (from JSON) or keyword-keyed (from EDN) — both validate
  identically."
  (:require [clojure.string :as str]
            [fork-tales.law.reconstruction :as law]))

;; ---------------------------------------------------------------------------
;; Predicates — faithful to the retired Python semantics
;; ---------------------------------------------------------------------------

(defn- alt-keys
  "Both spellings of a key. Schema field names arrive as strings, packet keys as
  either, so lookup must be bidirectional or keyword-keyed EDN packets would
  silently skip every string-keyed schema check. nil is passed through so an
  absent handoff_kind cannot blow up on (name nil)."
  [k]
  (cond
    (keyword? k) [k (name k)]
    (string? k) [k (keyword k)]
    :else [k]))

(defn- key-in
  "The spelling actually present in `m`, if any."
  [m k]
  (first (filter #(contains? m %) (alt-keys k))))

(defn g
  "Get by keyword or string key, in either direction."
  [m k]
  (when (map? m)
    (when-let [kk (key-in m k)] (get m kk))))

(defn present?
  "Key present at all, regardless of value. μ1 requires an explicit empty list,
  so presence and non-emptiness are different questions."
  [m k]
  (boolean (and (map? m) (key-in m k))))

(defn fname
  "Render a field name for an error path. Never leaks a keyword's colon."
  [k]
  (if (keyword? k) (name k) (str k)))

(defn nonempty?
  [v]
  (cond
    (nil? v) false
    (string? v) (not (str/blank? v))
    (coll? v) (boolean (seq v))
    :else true))

(defn has-span?
  "A span needs t0+t1, or bars, or section."
  [span]
  (and (map? span)
       (boolean (or (and (present? span :t0) (present? span :t1))
                    (nonempty? (g span :bars))
                    (nonempty? (g span :section))))))

(defn ensure-seq [v] (cond (nil? v) [] (sequential? v) v :else [v]))

(defn artifact-items
  "Index artifacts as [label item] pairs; lists numerically, maps by key."
  [v]
  (cond
    (sequential? v) (map-indexed (fn [i item] [(str "artifacts[" i "]") item]) v)
    (map? v) (mapv (fn [[k item]] [(str "artifacts." (if (keyword? k) (name k) (str k))) item]) v)
    :else []))

;; ---------------------------------------------------------------------------
;; Accumulator
;; ---------------------------------------------------------------------------

(def empty-acc {:errors [] :warnings [] :checked-specs []})

(defn- add [acc bucket spec path message]
  (update acc bucket conj {:spec (name spec) :path path :message message}))

(defn- checked [acc spec]
  (update acc :checked-specs
          (fn [s] (if (contains? (set s) (name spec)) s (conj s (name spec))))))

;; ---------------------------------------------------------------------------
;; Invariant machinery
;; ---------------------------------------------------------------------------

(defn applies?
  "Does an invariant's :applies-when match? Any matching clause triggers,
  mirroring the Python `or` semantics. :verdict clauses are conjunctive with the
  kind/role clause, because μ2/μ3 only fire on a rejecting verdict."
  [{:keys [applies-when]} packet]
  (let [{kinds :handoff-kind, statuses :status, modes :mode, roles :role
         verdicts :verdict, flag :flag} applies-when
        kind (g packet :handoff_kind)
        status (or (g packet :status) (g packet :approval_status) (g packet :verdict))
        gate (or (and kinds (contains? kinds kind))
                 (and statuses (contains? statuses status))
                 (and modes (contains? modes (g packet :mode)))
                 (and roles (contains? roles (g packet :role)))
                 (and flag (nonempty? (g packet flag))))]
    (boolean
     (if verdicts
       (and gate (contains? verdicts (g packet :verdict)))
       gate))))

(defn- check-requires
  [acc spec requires obj path]
  (reduce
   (fn [a {:keys [field as message severity]}]
     (let [ok (case as
                :span (has-span? (g obj field))
                :present (present? obj field)
                (nonempty? (g obj field)))
           bucket (if (= severity :warning) :warnings :errors)]
       (if ok a (add a bucket spec (str path "." (fname field)) message))))
   acc requires))

(defn- mu1 [acc inv packet pname]
  (let [kind (g packet :handoff_kind)
        sources (concat (when (applies? inv packet) (artifact-items (g packet :artifacts)))
                        (when (= kind "qc_review") (artifact-items (g packet :accepted_artifacts))))]
    (if (empty? sources)
      acc
      (reduce
       (fn [a [path artifact]]
         (if-not (map? artifact)
           (add a :errors (:mu inv) (str pname "." path) (:object-required inv))
           (check-requires a (:mu inv) (:requires inv) artifact (str pname "." path))))
       (checked acc (:mu inv))
       sources))))

(defn- mu-collection
  "μ2 and μ3 share a shape: a collection whose members must each satisfy
  :requires, with at least one satisfying the first requirement."
  [acc {:keys [mu collection empty-message object-required requires any-message] :as inv} packet pname]
  (if-not (applies? inv packet)
    acc
    (let [acc (checked acc mu)
          items (ensure-seq (g packet collection))
          cname (name collection)]
      (if (empty? items)
        (add acc :errors mu (str pname "." cname) empty-message)
        (let [acc' (reduce
                    (fn [a [i item]]
                      (let [path (str pname "." cname "[" i "]")]
                        (if (and object-required (not (map? item)))
                          (add a :errors mu path object-required)
                          (check-requires a mu requires item path))))
                    acc (map-indexed vector items))
              first-field (:field (first requires))]
          (if (and any-message
                   (not (some #(and (map? %) (nonempty? (g % first-field))) items)))
            (add acc' :errors mu (str pname "." cname) any-message)
            acc'))))))

(defn- mu4 [acc inv packet pname approved-ids]
  (if-not (applies? inv packet)
    acc
    (let [refs (->> [(g packet :references)
                     (g (or (g packet :inputs) {}) :references)
                     (g (or (g packet :constraints) {}) :references)]
                    (mapcat ensure-seq)
                    (map #(if (string? %) {:reference_id %} %))
                    (filter map?))]
      (reduce
       (fn [a [i ref]]
         (let [rid (or (g ref :reference_id) (g ref :id) (g ref :path))
               status (or (g ref :approval_status) (g ref :status))
               path (str pname ".references[" i "]")]
           (cond
             (or (nonempty? (g ref :exploratory)) (nonempty? (g ref :non_training)))
             (add a :warnings (:mu inv) path
                  "reference marked exploratory/non-training; not counted as approved catalog input")
             (= status "approved") a
             (and rid (contains? approved-ids (str rid))) a
             :else (add a :errors (:mu inv) path
                        (str "composition reference " (pr-str rid) " is not approved")))))
       (checked acc (:mu inv))
       (map-indexed vector refs)))))

(defn- mu5 [acc inv packet pname]
  (if-not (applies? inv packet)
    acc
    (check-requires (checked acc (:mu inv)) (:mu inv) (:requires inv) packet pname)))

(defn- mu6 [acc {:keys [mu forbids] :as inv} packet pname]
  (if-not (applies? inv packet)
    acc
    (cond-> (checked acc mu)
      (contains? (:verdict forbids) (g packet :verdict))
      (add :errors mu (str pname ".verdict")
           "gemma_check_subagent cannot accept or approve work")

      (contains? (:handoff-kind forbids) (g packet :handoff_kind))
      (add :errors mu (str pname ".handoff_kind")
           "gemma_check_subagent cannot emit QC/catalog/final-release handoff"))))

;; ---------------------------------------------------------------------------
;; Common schema checks
;; ---------------------------------------------------------------------------

(defn- validate-common [acc packet schema pname]
  (let [acc (reduce (fn [a field]
                      (if (nonempty? (g packet field))
                        a
                        (add a :errors :common (str pname "." (fname field))
                             "missing required common handoff field")))
                    acc (ensure-seq (g schema :common_required_fields)))
        chk (fn [a field vocab-key label]
              (let [v (g packet field)
                    vocab (set (ensure-seq (g schema vocab-key)))]
                (if (and v (seq vocab) (not (contains? vocab v)))
                  (add a :errors :common (str pname "." (fname field))
                       (str "unknown " label " " (pr-str v)))
                  a)))
        acc (-> acc
                (chk :handoff_kind :handoff_kinds "handoff_kind")
                (chk :mode :modes "mode")
                (chk :role :roles "role"))
        kind-schema (g (or (g schema :schemas) {}) (g packet :handoff_kind))]
    (if-not (map? kind-schema)
      acc
      (reduce (fn [a field]
                (if (nonempty? (g packet field))
                  a
                  (add a :errors :kind-required (str pname "." (fname field))
                       (str "missing required field for " (g packet :handoff_kind)))))
              acc (ensure-seq (g kind-schema :required))))))

;; ---------------------------------------------------------------------------
;; Entry points
;; ---------------------------------------------------------------------------

(def ^:private inv-by-mu
  (into {} (map (juxt :mu identity)) law/handoff-invariants))

(defn validate-packet
  [acc packet schema pname approved-ids]
  (-> acc
      (validate-common packet schema pname)
      (mu1 (inv-by-mu :μ1) packet pname)
      (mu-collection (inv-by-mu :μ2) packet pname)
      (mu-collection (inv-by-mu :μ3) packet pname)
      (mu4 (inv-by-mu :μ4) packet pname approved-ids)
      (mu5 (inv-by-mu :μ5) packet pname)
      (mu6 (inv-by-mu :μ6) packet pname)))

(defn approved-ids
  "Extract approved reference ids from a catalog, tolerating both entry shapes."
  [catalog]
  (if-not (map? catalog)
    #{}
    (let [entries (or (g catalog :entries) (g catalog :references) [])
          entries (if (map? entries)
                    (mapv (fn [[k v]] (merge {:reference_id (if (keyword? k) (name k) k)}
                                             (when (map? v) v)))
                          entries)
                    (ensure-seq entries))]
      (into #{}
            (keep (fn [e]
                    (when (map? e)
                      (let [status (or (g e :approval_status) (g e :status))
                            rid (or (g e :reference_id) (g e :id))]
                        (when (and rid (= status "approved")) (str rid))))))
            entries))))

(defn report
  "Validate `named-packets` (a seq of [display-name packet]) into a report map.

  Key shape matches the retired Python tool's JSON output exactly, and so do
  :ok, :error_count, :warning_count, and every error path and message —
  differentially confirmed against the recovered tool over 365 packets.

  :checked_specs deliberately does not match. The retired tool marked μ1 checked
  unconditionally and μ2/μ3 on handoff_kind alone; here a spec is reported
  checked only when :applies-when actually fires, so committed *.validation.json
  artifacts replay with a wider :checked_specs than this produces. See
  docs/reconstruction/runtime-split.md."
  [named-packets schema approved]
  (let [acc (reduce (fn [a [pname packet]]
                      (if-not (map? packet)
                        (add a :errors :common pname "packet must be an object")
                        (validate-packet a packet schema pname approved)))
                    empty-acc named-packets)]
    {:schema_version "fork-tales-handoff-validation-report/v1"
     :ok (empty? (:errors acc))
     :checked_specs (vec (sort (:checked-specs acc)))
     :error_count (count (:errors acc))
     :warning_count (count (:warnings acc))
     :errors (:errors acc)
     :warnings (:warnings acc)}))
