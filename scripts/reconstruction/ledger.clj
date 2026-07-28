(ns reconstruction.ledger
  "Append-only event ledger for audio reconstruction.

  This namespace is the seam between runtimes. Babashka lanes (validate, grade,
  judge) and JVM lanes (DSP metrics via libpython-clj) never call each other —
  they both append events here, and every projection is rebuilt from the file.
  That is why the pipeline needs no pod: the ledger is the IPC.

  Loadable from both bb and JVM Clojure: no bb-only or JVM-only requires."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def ledger-name "reconstruction.edn")

(defn repo-root
  "Resolve the repository root from this file's location, so no absolute host
  path is ever baked in. Override with FORK_TALES_ROOT."
  []
  (or (System/getenv "FORK_TALES_ROOT")
      (let [cwd (.getCanonicalFile (io/file "."))]
        (loop [d cwd]
          (cond
            (nil? d) (str cwd)
            (.exists (io/file d "deps.edn")) (str d)
            :else (recur (.getParentFile d)))))))

(defn ledger-path []
  (str (io/file (repo-root) "ledgers" ledger-name)))

(defn now-iso [] (str (Instant/now)))
(defn uuid [] (UUID/randomUUID))

(defn sha256-hex
  "Content hash of a file. Streams, so a 300MB wav does not land in memory."
  [path]
  (let [md (MessageDigest/getInstance "SHA-256")
        buf (byte-array 65536)]
    (with-open [in (io/input-stream (io/file path))]
      (loop []
        (let [n (.read in buf)]
          (when (pos? n)
            (.update md buf 0 n)
            (recur)))))
    (str/join (map #(format "%02x" %) (.digest md)))))

(defn artifact-ref
  "Build an :ft.rec/ArtifactRef for a real file. Paths are stored relative to
  the repo root when the file is inside it, absolute otherwise — so a tracked
  artifact reference stays valid on any machine."
  ([path] (artifact-ref path nil))
  ([path role]
   (let [f (io/file path)
         root (str (io/file (repo-root)))
         abs (.getCanonicalPath f)
         rel (if (str/starts-with? abs (str root "/"))
               (subs abs (inc (count root)))
               abs)]
     (cond-> {:artifact/path rel
              :artifact/sha256 (sha256-hex f)
              :artifact/bytes (.length f)}
       role (assoc :artifact/role role)))))

(defn append-event!
  "Append one event as a single EDN line. Never rewrites, never reorders."
  [event]
  (let [p (ledger-path)]
    (io/make-parents p)
    (spit p (str (pr-str event) "\n") :append true)
    event))

(defn read-events
  "Read the ledger as a lazy-ish vector of events. Blank lines tolerated."
  []
  (let [p (ledger-path)]
    (if-not (.exists (io/file p))
      []
      (into []
            (comp (remove str/blank?)
                  (map #(edn/read-string {:readers {}} %)))
            (str/split-lines (slurp p))))))

(defn base-event
  "Common envelope. :event/tier is required by law and therefore required here."
  [{:keys [type tier run-id case-id note]}]
  (cond-> {:event/id (uuid)
           :event/type type
           :event/tier tier
           :run/id run-id
           :ts (now-iso)
           :case/id case-id}
    note (assoc :note note)))

(defn events-for-run [run-id]
  (filterv #(= run-id (:run/id %)) (read-events)))

(defn latest-of-type
  "Most recent event of a type, by ledger order. Order is truth; ts is evidence."
  [events type]
  (last (filterv #(= type (:event/type %)) events)))
