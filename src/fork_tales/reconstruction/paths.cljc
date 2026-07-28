(ns fork-tales.reconstruction.paths
  "Resolve host paths recorded inside preserved evidence artifacts.

  Artifacts under `references/` are append-only records of runs that really
  happened, so their embedded paths are never rewritten. Most of those paths no
  longer resolve: they name `/home/err/devel/Music/...` while the bytes have
  always been at `/home/err/Music/...`.

  The consequence found on 2026-07-27 is the reason this namespace exists. Given
  such an artifact, `audio_grade.py` does not fail — it scores the unreachable
  features `null` and reports a lower coverage, so a broken input looks like a
  weak candidate. Silent degradation is worse than an error, because it is
  indistinguishable from a real result.

  This namespace turns that back into an explicit, checkable claim: translate by
  declared rule, classify every reference, and let the caller refuse to proceed.
  Pure — existence checking is injected, so it is testable without a filesystem."
  (:require [clojure.string :as str]))

(def ^:private path-re
  "Absolute POSIX paths as they appear inside evidence JSON. Deliberately stops
  at quote, comma and whitespace, matching how these artifacts embed them."
  #"/(?:home|tmp|mnt|media|opt|srv|var)/[^\"'\s,;)\]}]+")

(defn collect-paths
  "Every distinct absolute path appearing anywhere in nested data, sorted.
  Walks maps, vectors, sets and strings; ignores everything else."
  [data]
  (let [acc (volatile! (transient #{}))]
    (letfn [(walk [x]
              (cond
                (map? x) (run! (fn [[k v]] (walk k) (walk v)) x)
                (coll? x) (run! walk x)
                (string? x) (run! #(vswap! acc conj! %) (re-seq path-re x))))]
      (walk data))
    (vec (sort (persistent! @acc)))))

(defn translate
  "Apply the first matching rule to `path`. Returns the path unchanged when no
  rule applies. Rules are ordered; longest-prefix is the caller's job to encode."
  [rules path]
  (or (some (fn [{:keys [from to]}]
              (when (str/starts-with? path from)
                (str to (subs path (count from)))))
            rules)
      path))

(defn classify
  "Classify one path against the filesystem via `exists?`.

    :resolved   present as written
    :translated absent as written, present after a declared rule
    :missing    absent either way — the caller must not treat this as data
    :shadowed   present as written AND a rule matches, so the wrong file may be
                read silently. This is the decoy case that started all of it."
  [rules exists? path]
  (let [t (translate rules path)
        here? (exists? path)
        there? (and (not= t path) (exists? t))]
    (cond
      (and here? (not= t path)) {:path path :resolved t :status :shadowed}
      here?                     {:path path :resolved path :status :resolved}
      there?                    {:path path :resolved t :status :translated}
      :else                     {:path path :resolved nil :status :missing})))

(defn report
  "Classify every path in `data`. `:ok?` is false when anything is :missing.

  :shadowed does not fail the report — the file is readable — but it is surfaced
  because reading it may silently use a stale copy."
  [{:keys [rules exists? data]}]
  (let [entries (mapv #(classify rules exists? %) (collect-paths data))
        by (group-by :status entries)]
    {:schema_version "fork-tales-path-preflight/v1"
     :ok? (empty? (:missing by))
     :total (count entries)
     :counts (into {} (map (fn [[k v]] [k (count v)])) by)
     :missing (mapv :path (:missing by))
     :translated (mapv (juxt :path :resolved) (:translated by))
     :shadowed (mapv (juxt :path :resolved) (:shadowed by))
     :entries entries}))

(defn rewrite
  "Return `data` with every translatable path replaced by its resolved form.
  Used to build a *scratch* copy for re-running a grader. Never write the result
  back over a committed artifact — that would rewrite history to fix a symptom."
  [rules data]
  (letfn [(walk [x]
            (cond
              (map? x) (into (empty x) (map (fn [[k v]] [(walk k) (walk v)])) x)
              (vector? x) (mapv walk x)
              (set? x) (into #{} (map walk) x)
              (seq? x) (map walk x)
              (string? x) (str/replace x path-re #(translate rules %))
              :else x))]
    (walk data)))
