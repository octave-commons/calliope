(ns fork-tales.reconstruction.paths-test
  "Tests for evidence path resolution.

  `exists?` is injected as a set membership check, so these run without touching
  a filesystem and the decoy case can be modelled exactly."
  (:require [clojure.test :refer [deftest is testing]]
            [fork-tales.law.reconstruction :as law]
            [fork-tales.reconstruction.paths :as p]))

(def rules
  [{:from "/home/err/devel/Music/fork-tales" :to "/home/err/Music/fork-tales"}
   {:from "/home/err/devel/Music/heresy_between" :to "/home/err/Music/heresy_between"}])

(deftest collects-paths-from-nested-data
  (testing "finds paths in map values, vectors, and inside longer strings"
    (let [data {"a" "/home/err/Music/x.wav"
                "b" ["/tmp/y.json" {"c" "/home/err/devel/Music/fork-tales/z.wav"}]
                "d" "refs: /home/err/Music/p.csv,/home/err/Music/q.csv"}]
      (is (= ["/home/err/Music/p.csv"
              "/home/err/Music/q.csv"
              "/home/err/Music/x.wav"
              "/home/err/devel/Music/fork-tales/z.wav"
              "/tmp/y.json"]
             (p/collect-paths data)))))
  (testing "deduplicates and ignores non-paths"
    (is (= ["/home/err/a"] (p/collect-paths {"x" "/home/err/a" "y" "/home/err/a" "z" "not a path" "n" 42}))))
  (testing "does not swallow trailing punctuation"
    (is (= ["/home/err/a.wav"] (p/collect-paths {"k" "see /home/err/a.wav, then stop"})))))

(deftest translate-applies-first-matching-rule
  (is (= "/home/err/Music/fork-tales/r/v.wav"
         (p/translate rules "/home/err/devel/Music/fork-tales/r/v.wav")))
  (testing "unmatched paths pass through untouched"
    (is (= "/home/err/other/x" (p/translate rules "/home/err/other/x"))))
  (testing "an empty ruleset is identity"
    (is (= "/a/b" (p/translate [] "/a/b")))))

(deftest classify-statuses
  (let [present #{"/home/err/Music/fork-tales/ok.wav"
                  "/home/err/devel/Music/fork-tales/decoy.wav"
                  "/home/err/Music/fork-tales/decoy.wav"
                  "/home/err/plain.wav"}
        c #(p/classify rules (fn [x] (contains? present x)) %)]
    (testing "resolved: present as written, no rule matches"
      (is (= :resolved (:status (c "/home/err/plain.wav")))))
    (testing "translated: absent as written, present after a rule"
      (let [r (c "/home/err/devel/Music/fork-tales/ok.wav")]
        (is (= :translated (:status r)))
        (is (= "/home/err/Music/fork-tales/ok.wav" (:resolved r)))))
    (testing "missing: absent both ways, and carries no resolved path"
      (let [r (c "/home/err/devel/Music/fork-tales/gone.wav")]
        (is (= :missing (:status r)))
        (is (nil? (:resolved r)))))
    (testing "shadowed: the decoy case — readable as written, but a rule also matches"
      (let [r (c "/home/err/devel/Music/fork-tales/decoy.wav")]
        (is (= :shadowed (:status r))
            "a stale path that still resolves is the trap that started this")
        (is (= "/home/err/Music/fork-tales/decoy.wav" (:resolved r)))))))

(deftest report-gates-on-missing-only
  (let [present #{"/home/err/Music/fork-tales/a.wav"}
        exists? #(contains? present %)]
    (testing "all-translatable evidence is ok"
      (let [r (p/report {:rules rules :exists? exists?
                         :data {"x" "/home/err/devel/Music/fork-tales/a.wav"}})]
        (is (:ok? r))
        (is (= 1 (:total r)))
        (is (= {:translated 1} (:counts r)))
        (is (= [["/home/err/devel/Music/fork-tales/a.wav" "/home/err/Music/fork-tales/a.wav"]]
               (:translated r)))))
    (testing "any missing path fails the report"
      (let [r (p/report {:rules rules :exists? exists?
                         :data {"x" "/home/err/devel/Music/fork-tales/nope.wav"}})]
        (is (not (:ok? r)))
        (is (= ["/home/err/devel/Music/fork-tales/nope.wav"] (:missing r)))))
    (testing "shadowed is surfaced but does not fail — the file is readable"
      (let [ex #(contains? #{"/home/err/devel/Music/fork-tales/a.wav"
                             "/home/err/Music/fork-tales/a.wav"} %)
            r (p/report {:rules rules :exists? ex
                         :data {"x" "/home/err/devel/Music/fork-tales/a.wav"}})]
        (is (:ok? r))
        (is (= 1 (count (:shadowed r))))))
    (testing "empty evidence is vacuously ok"
      (is (:ok? (p/report {:rules rules :exists? exists? :data {}}))))))

(deftest rewrite-translates-without-touching-structure
  (let [data {"original" {"audio" "/home/err/devel/Music/fork-tales/o.wav"}
              "refs" ["/home/err/devel/Music/fork-tales/a.csv"
                      "/home/err/Music/fork-tales/b.csv"]
              "count" 517
              "note" "pair: /home/err/devel/Music/fork-tales/x,/home/err/devel/Music/fork-tales/y"}
        out (p/rewrite rules data)]
    (is (= "/home/err/Music/fork-tales/o.wav" (get-in out ["original" "audio"])))
    (is (= ["/home/err/Music/fork-tales/a.csv" "/home/err/Music/fork-tales/b.csv"] (get out "refs")))
    (is (= 517 (get out "count")) "non-strings are untouched")
    (is (= "pair: /home/err/Music/fork-tales/x,/home/err/Music/fork-tales/y" (get out "note"))
        "multiple paths inside one string are all translated")
    (is (= (keys data) (keys out)) "shape is preserved")))

(deftest law-declares-the-event
  (testing "the preflight event is in the closed vocabulary"
    (is (contains? (set law/event-types) :evidence/preflighted)))
  (testing "it is structurally pinned to :observed — it measures bytes, not opinion"
    (is (= [:= :observed]
           (-> law/registry :ft.rec/EvidencePreflighted (nth 2) (nth 1) (nth 1))))))
