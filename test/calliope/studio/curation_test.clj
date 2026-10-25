(ns calliope.studio.curation-test
  (:require [calliope.law.curation :as law]
            [calliope.studio.session :as session]
            [calliope.studio.session-test :as fixture]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]])
  (:import [java.util UUID]
           [java.nio.file Files Path]))

(def clip-id "60000000-0000-4000-8000-000000000001")
(def proposal-id "60000000-0000-4000-8000-000000000002")
(def marker-id "60000000-0000-4000-8000-000000000003")
(defn revision [store] (get-in (session/view store) [:session :session/revision]))
(defn cmd [store type values] (merge (fixture/command type (revision store)) values))
(defn send! [store type values]
  (session/ui-command! store fixture/actor (cmd store type values)))
(defn ok! [result] (is (= :ok (:result/status result)) (pr-str result)) result)
(defn state [store] (:session (session/view store)))

(defn with-clip [f]
  (fixture/with-store
   (fn [parent root store]
     (let [ingest (fixture/setup-capture! store)
           admitted (session/ui-command! store fixture/actor ingest)
           take (get-in admitted [:result/value :take])]
       (ok! admitted)
       (ok! (send! store :studio/mark {:take/id (:take/id take) :marker/id marker-id
                                      :range {:start-sample 1 :end-sample 24000}}))
       (ok! (send! store :studio/stop {}))
       (ok! (send! store :studio/select-clip {:take/id (:take/id take) :clip/id clip-id
                                             :source/sha256 (get-in take [:take/audio :sha256])
                                             :range {:start-sample 1 :end-sample 24000} :clip/title "Phrase one"}))
       (f parent root store take)))))

(def output
  {:text "Provisional synthetic motif: keep the short opening."
   :labels ["music/motif"] :provider/id "deterministic-fixture"
   :model/id "fixture-v1" :prompt/sha256 (apply str (repeat 64 "a"))
   :evidence/refs ["urn:calliope:synthetic-midi-metadata"]
   :input/kind :synthetic-metadata :execution/kind :fixture})

(defn request! [store]
  (ok! (send! store :studio/request-proposal {:clip/id clip-id :proposal/id proposal-id
                                            :proposal/prompt "Suggest a motif from this synthetic phrase."})))
(defn complete! [store]
  (let [channel ((requiring-resolve 'calliope.studio.session/proposal-channel!)
                 store fixture/actor proposal-id)]
    (ok! (channel {:proposal/output output}))))

(deftest curation-laws-are-portable-closed-and-bounded
  (is (= law/complete-registry (edn/read-string (pr-str law/complete-registry))))
  (doseq [type (keys law/complete-registry)] (is (ifn? (law/validator type))))
  (is (law/valid? :calliope.curation/output-v1 output))
  (is (not (law/valid? :calliope.curation/output-v1 (assoc output :actor fixture/actor))))
  (is (not (law/valid? :calliope.curation/output-v1 (assoc output :text (apply str (repeat 4097 "x")))))))

(deftest mark-select-refine-annotate-are-source-linked-nondestructive-and-recoverable
  (with-clip
   (fn [parent root store take]
     (let [hashes (mapv #(get-in take %) [[:take/audio :sha256] [:take/midi :sha256]])
           paths (mapv #(session/source-path! store %) [(get-in take [:take/audio :render/id]) (get-in take [:take/midi :source/id])])
           bytes (mapv #(Files/readAllBytes (Path/of % (make-array String 0))) paths)]
       (ok! (send! store :studio/annotate {:clip/id clip-id :annotation/text "Keep the pick attack."
                                          :annotation/labels ["music/guitar"]}))
       (let [selection (get-in (state store) [:clips clip-id])]
         (is (= {:start-sample 1 :end-sample 24000} (:range selection)))
         (is (= {:start-ms 0 :end-ms 500} (get-in selection [:clip :clip/range])))
         (is (= clip-id (get-in selection [:clip :clip/id])))
         (is (= hashes (:source/hashes selection)))
         (is (= "Keep the pick attack." (get-in (state store) [:annotations clip-id 0 :text]))))
       (is (= :error (:result/status (send! store :studio/select-clip
                                          {:take/id (:take/id take) :clip/id clip-id :source/sha256 (first hashes)
                                           :range {:start-sample 24000 :end-sample 1} :clip/title "Bad"}))))
       (doseq [[path before] (map vector paths bytes)]
         (is (java.util.Arrays/equals before (Files/readAllBytes (Path/of path (make-array String 0))))))
       (let [before (state store) history (session/history store) output-file (.resolve parent "curation.edn")]
         (session/close! store)
         (Files/delete ^Path (:index/path store))
         (fixture/await-process! (fixture/launch! "inspect" (str root) (:actor/id fixture/actor) (str output-file)))
         (let [after (edn/read-string (slurp (str output-file)))]
           (is (= history (:history after)))
           (is (= before (get-in after [:view :session])))))))))

(deftest accepting-is-a-separate-attributed-decision
  (with-clip
   (fn [_ _ store _]
     (request! store) (complete! store)
     (is (empty? (:decisions (state store))))
     (is (empty? (:accepted-labels (state store))))
     (let [before (session/history store)]
       (dotimes [_ 3] (session/view store))
       (is (= before (session/history store))))
     (ok! (send! store :studio/decide {:proposal/id proposal-id :decision/value :accept}))
     (let [decision (first (:decisions (state store)))]
       (is (= :accept (:decision/value decision)))
       (is (= fixture/actor (:decision/actor decision)))
       (is (= proposal-id (:proposal/id decision)))
       (is (= clip-id (:clip/id decision)))
       (is (string? (:decision/source-event decision)))
       (is (= ["music/motif"] (get-in (state store) [:accepted-labels clip-id])))))))

(deftest rejecting-does-not-promote-labels-and-recovers-separately
  (with-clip
   (fn [parent root store _]
     (request! store) (complete! store)
     (ok! (send! store :studio/decide {:proposal/id proposal-id :decision/value :reject}))
     (is (= :reject (:decision/value (first (:decisions (state store))))))
     (is (empty? (:accepted-labels (state store))))
     (let [before (state store) output-file (.resolve parent "decision.edn")]
       (session/close! store)
       (fixture/await-process! (fixture/launch! "inspect" (str root) (:actor/id fixture/actor) (str output-file)))
       (is (= before (get-in (edn/read-string (slurp (str output-file))) [:view :session])))))))

(deftest only-trusted-user-entrypoint-may-decide
  (with-clip
   (fn [_ _ store _]
     (request! store) (complete! store)
     (let [command (cmd store :studio/decide {:proposal/id proposal-id :decision/value :accept})
           before (session/history store)]
       (doseq [kind [:agent :system]]
         (is (= :command/not-authorized (:error/code (session/ui-command! store (assoc fixture/actor :actor/kind kind) command)))))
       (is (= :command/not-authorized (:error/code (session/provider-command! store fixture/actor command))))
       (is (= :command/not-authorized (:error/code (session/system-command! store fixture/actor command))))
       (is (= :command/invalid (:error/code (session/ui-command! store fixture/actor (assoc command :entrypoint :ui-user)))))
       (is (= before (session/history store)))
       (is (empty? (:decisions (state store))))
       (is (empty? (:accepted-labels (state store))))
       (is (= :proposal/not-found (:error/code (send! store :studio/decide {:proposal/id (str (UUID/randomUUID)) :decision/value :accept}))))))))

(deftest provider-cannot-change-clip-or-self-promote
  (with-clip
   (fn [_ _ store _]
     (request! store)
     (let [channel ((requiring-resolve 'calliope.studio.session/proposal-channel!) store fixture/actor proposal-id)
           before (session/history store)]
       (is (= :command/invalid (:error/code (channel {:proposal/output output :decision/value :accept}))))
       (is (= before (session/history store)))
       (ok! (channel {:proposal/output output}))
       (is (empty? (:decisions (state store))))
       (is (= :command/not-authorized (:error/code (send! store :studio/proposal-result {:proposal/id proposal-id :proposal/output output}))))))))

(deftest unfinished-local-proposal-recovers-as-interrupted-not-executing
  (with-clip
   (fn [_ root store _]
     (request! store)
     (session/close! store)
     (let [recovered (session/open! root fixture/actor)]
       (try
         (is (= :interrupted (get-in (session/view recovered) [:proposal/statuses proposal-id])))
         (is (empty? (:decisions (state recovered))))
         (finally (session/close! recovered)))))))

(defn await-status! [store id expected]
  (let [deadline (+ (System/nanoTime) 5000000000)]
    (loop []
      (if (= expected (get-in (state store) [:proposals id :proposal/status])) true
          (if (> (System/nanoTime) deadline) false
              (do (Thread/sleep 10) (recur)))))))

(deftest stalled-provider-does-not-own-capture-and-times-out-explicitly
  (with-clip
   (fn [_ _ store _]
     (let [entered (promise) release (promise)
           start! (requiring-resolve 'calliope.studio.proposals/start!)
           request (cmd store :studio/request-proposal {:clip/id clip-id :proposal/id proposal-id :proposal/prompt "Synthetic motif"})
           job (start! store fixture/actor request
                       (fn [_] (deliver entered true) @release {:text "late" :labels []})
                       {:timeout-ms 300 :provider/id "test-stalled" :model/id "fixture" :execution/kind :fixture})]
       (ok! (:request job))
       (is (= true (deref entered 2000 false)))
       (let [stop (future (send! store :studio/stop {}))]
         (is (= :ok (:result/status (deref stop 2000 {:result/status :timeout})))))
       (is (await-status! store proposal-id :failed))
       (is (= :timeout (get-in (state store) [:proposals proposal-id :failure/code])))
       (deliver release true)
       (is (empty? (:decisions (state store))))))))

(deftest failed-and-malformed-provider-output-never-becomes-a-decision
  (doseq [[provider expected] [[(fn [_] (throw (java.io.IOException. "Unavailable test provider"))) :unavailable]
                              [(fn [_] {:text "bad" :labels [] :decision/value :accept}) :invalid-output]]]
    (with-clip
     (fn [_ _ store _]
       (let [job ((requiring-resolve 'calliope.studio.proposals/start!)
                  store fixture/actor
                  (cmd store :studio/request-proposal {:clip/id clip-id :proposal/id proposal-id :proposal/prompt "Synthetic motif"})
                  provider {:timeout-ms 1000 :provider/id "test" :model/id "fixture" :execution/kind :fixture})]
         (ok! (:request job))
         (is (await-status! store proposal-id :failed))
         (is (= expected (get-in (state store) [:proposals proposal-id :failure/code])))
         (is (empty? (:decisions (state store))))
         (ok! (send! store :studio/stop {})))))))

(deftest historical-arm-cannot-authorize-mark-after-restart
  (fixture/with-store
   (fn [_ root store]
     (let [ingest (fixture/setup-capture! store) result (session/ui-command! store fixture/actor ingest)
           take (get-in result [:result/value :take])]
       (ok! result)
       (session/close! store)
       (let [recovered (session/open! root fixture/actor)]
         (try
           (let [before (session/history recovered)
                 result (send! recovered :studio/mark {:take/id (:take/id take) :marker/id marker-id
                                                       :range {:start-sample 0 :end-sample 100}})]
             (is (= :capture/not-armed (:error/code result)))
             (is (= before (session/history recovered))))
           (finally (session/close! recovered))))))))

(deftest stale-clip-cannot-be-decided-and-source-corruption-cannot-admit-curation
  (with-clip
   (fn [_ _ store take]
     (request! store) (complete! store)
     (ok! (send! store :studio/select-clip {:take/id (:take/id take) :clip/id clip-id
                                           :source/sha256 (get-in take [:take/audio :sha256])
                                           :range {:start-sample 0 :end-sample 100} :clip/title "Refined"}))
     (is (= :proposal/stale-clip (:error/code (send! store :studio/decide {:proposal/id proposal-id :decision/value :accept}))))
     (let [source (session/source-path! store (get-in take [:take/audio :render/id]))
           before (session/history store)]
       (spit source "Injected corrupt canonical source")
       (is (= :error (:result/status (send! store :studio/annotate {:clip/id clip-id :annotation/text "refused" :annotation/labels []}))))
       (is (= before (session/history store)))))))

(deftest completion-payloads-return-explicit-failure-for-nonmaps
  (with-clip
   (fn [_ _ store _]
     (request! store)
     (let [channel ((requiring-resolve 'calliope.studio.session/proposal-channel!) store fixture/actor proposal-id)]
       (doseq [payload [nil "unexpected" 42 [:unexpected]]]
         (is (= :command/invalid (:error/code (channel payload)))))))))
