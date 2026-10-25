(ns calliope.studio.desktop-test
  (:require [calliope.law.desktop :as law]
            [calliope.studio.desktop :as desktop]
            [calliope.studio.i3 :as i3]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]))

(def rect {:x 0 :y 0 :width 1920 :height 1080})
(def window {:id 10 :workspace 1 :rect {:x 0 :y 0 :width 800 :height 600}
             :floating? false :fullscreen? false})
(def snapshot {:revision (apply str (repeat 64 "a")) :focus 30 :workspace {:id 1 :rect rect}
               :windows {10 window 20 (assoc window :id 20) 30 (assoc window :id 30)}})
(def profile {:profile/type :music-v1 :studio/id 10 :daw/id 20})

(defn harness []
  (let [world (atom snapshot) calls (atom []) reads (atom 0)
        execute (fn [op command]
                  (swap! calls conj [op command])
                  (case (:op op)
                    :floating-enable (swap! world assoc-in [:windows (:id op) :floating?] true)
                    :resize (swap! world update-in [:windows (:id op) :rect] merge (select-keys op [:width :height]))
                    :move (swap! world update-in [:windows (:id op) :rect] merge (select-keys op [:x :y])))
                  {:ok? true})
        read (fn [] (swap! reads inc) {:ok? true :snapshot @world})]
    {:world world :calls calls :reads reads :execute execute :read read
     :controller (i3/controller {:read! read :execute! execute})}))

(deftest plain-named-closed-desktop-laws
  (is (= law/registry (edn/read-string (pr-str law/registry))))
  (is (law/valid? :calliope.desktop/snapshot-v1 snapshot))
  (is (not (law/valid? :calliope.desktop/profile-v1 (assoc profile :command "exec arbitrary")))))

(deftest profile-only-plans-explicit-selected-windows
  (let [result (desktop/plan snapshot profile) plan (:plan result)]
    (is (:ok? result))
    (is (law/valid? :calliope.desktop/plan-v1 plan))
    (is (= [10 20] (mapv :id (:targets plan))))
    (is (= [{:x 0 :y 0 :width 960 :height 1080} {:x 960 :y 0 :width 960 :height 1080}]
           (mapv :rect (:targets plan))))
    (is (= #{10 20} (set (map :id (:operations plan)))))
    (is (= "[con_id=10] resize set width 960 px height 1080 px"
           (desktop/command {:op :resize :id 10 :width 960 :height 1080})))
    (is (not-any? #(= :focus (:op %)) (:operations plan)))))

(deftest malformed-commands-never-become-i3-text
  (doseq [op [{:op :exec :id 10 :command "malicious"}
              {:op :move :id "10] exec bad" :x 0 :y 0}
              {:op :resize :id 10 :width 0 :height 720}
              {:op :floating-enable :id -1}
              {:op :floating-enable :id 10 :command "exec bad"}]]
    (is (thrown? clojure.lang.ExceptionInfo (desktop/command op))))
  (let [{:keys [controller calls]} (harness)]
    (is (false? (:ok? (i3/preview! controller (assoc profile :command "exec bad")))))
    (is (false? (:ok? (i3/apply! controller {:operations [{:op :exec :command "bad"}]}))))
    (is (empty? @calls))))

(deftest successful-apply-is-one-shot-and-preserves-focus-and-other-apps
  (let [{:keys [controller calls world]} (harness)
        before (get-in @world [:windows 30])
        preview (i3/preview! controller profile)
        result (i3/apply! controller (:plan/id preview))]
    (is (:ok? result) (pr-str result))
    (is (= 6 (count @calls)))
    (is (= 30 (:focus @world)))
    (is (= before (get-in @world [:windows 30])))
    (is (= #{10 20} (set (map (comp :id first) @calls))))
    (is (false? (:ok? (i3/apply! controller (:plan/id preview)))))
    (is (= 6 (count @calls)))))

(deftest stale-revision-manual-geometry-and-focus-cancel-with-zero-effects
  (doseq [change [#(assoc % :revision (apply str (repeat 64 "b")))
                  #(assoc-in % [:windows 10 :rect :width] 900)
                  #(assoc % :focus 20)]]
    (let [{:keys [controller calls world]} (harness) preview (i3/preview! controller profile)]
      (swap! world change)
      (is (= :desktop/stale-plan (:error/code (i3/apply! controller (:plan/id preview)))))
      (is (empty? @calls)))))

(deftest missing-small-opted-out-and-fullscreen-windows-have-reasons
  (doseq [[s p expected] [[(update snapshot :windows dissoc 20) profile :missing]
                          [(assoc-in snapshot [:windows 20 :rect :width] 100) profile :undersized]
                          [snapshot (assoc profile :opt-outs #{20}) :opted-out]
                          [(assoc-in snapshot [:windows 20 :fullscreen?] true) profile :fullscreen]]]
    (let [plan (:plan (desktop/plan s p))]
      (is (= expected (:reason (first (filter #(= 20 (:id %)) (:skipped plan))))))
      (is (= #{10} (set (map :id (:operations plan)))))))
  (testing "a small desktop retains a full-size studio slot and reports the missing DAW space"
    (let [s (assoc-in snapshot [:workspace :rect] {:x 0 :y 0 :width 960 :height 720})
          plan (:plan (desktop/plan s profile))]
      (is (= [{:x 0 :y 0 :width 960 :height 720}] (mapv :rect (:targets plan))))
      (is (= :undersized (:reason (first (:skipped plan))))))))

(deftest disconnect-never-reapplies-old-plan-on-reconnect
  (let [{:keys [world calls read execute]} (harness) fail? (atom true)
        controller (i3/controller {:read! read :execute! (fn [op cmd]
                                                          (if @fail?
                                                            {:ok? false :error/code :desktop/unavailable}
                                                            (execute op cmd)))})
        preview (i3/preview! controller profile)
        result (i3/apply! controller (:plan/id preview))]
    (is (false? (:ok? result)))
    (is (:uncertain? result))
    (reset! fail? false)
    (is (:ok? (i3/reconnect! controller)))
    (is (false? (:ok? (i3/apply! controller (:plan/id preview)))))
    (is (empty? @calls))
    (is (= snapshot @world))
    (let [fresh (i3/preview! controller profile)]
      (is (not= (:plan/id preview) (:plan/id fresh)))
      (is (:ok? (i3/apply! controller (:plan/id fresh)))))))

(deftest manual-change-during-apply-stops-with-explicit-partial-result
  (let [{:keys [world calls read execute]} (harness)
        controller (i3/controller {:read! read
                                   :execute! (fn [op command]
                                               (let [result (execute op command)]
                                                 (when (= 1 (count @calls)) (swap! world assoc :focus 20))
                                                 result))})
        preview (i3/preview! controller profile)
        result (i3/apply! controller (:plan/id preview))]
    (is (= :partial (:status result)))
    (is (= :desktop/focus-changed (:error/code result)))
    (is (= 1 (count @calls)))
    (is (= 20 (:focus @world)))))

(deftest newer-preview-invalidates-older-intent-with-zero-effects
  (let [{:keys [controller calls]} (harness)
        old (i3/preview! controller profile)
        fresh (i3/preview! controller (assoc profile :opt-outs #{20}))]
    (is (= :desktop/unknown-plan (:error/code (i3/apply! controller (:plan/id old)))))
    (is (empty? @calls))
    (is (:ok? (i3/apply! controller (:plan/id fresh))))
    (is (= #{10} (set (map (comp :id first) @calls))))))

(deftest native-tree-reduction-retains-focus-identity-and-floating-wrappers
  (let [node {:id 10 :window 100 :focused true :rect (:rect window) :nodes [] :floating_nodes []}
        tree {:id 1 :type "root" :nodes [{:id 2 :type "workspace" :rect rect
                                         :nodes [] :floating_nodes [{:id 3 :type "floating_con" :nodes [node]}]}]}
        result (i3/snapshot-from-tree tree)]
    (is (:ok? result))
    (is (= 10 (get-in result [:snapshot :focus])))
    (is (= 2 (get-in result [:snapshot :workspace :id])))
    (is (true? (get-in result [:snapshot :windows 10 :floating?])))
    (is (= result (i3/snapshot-from-tree tree)))
    (is (false? (:ok? (i3/snapshot-from-tree (assoc tree :nodes [])))))
    (is (false? (:ok? (i3/snapshot-from-tree
                      (assoc-in tree [:nodes 0 :floating_nodes 0 :nodes] [node node])))))))

(deftest a-lost-acknowledgement-is-not-an-empty-success-or-safe-retry
  (let [{:keys [world calls read execute]} (harness)
        c (i3/controller {:read! read :execute! (fn [op command]
                                                (execute op command)
                                                (throw (java.io.IOException. "Lost reply after effect")))})
        preview (i3/preview! c profile)
        result (i3/apply! c (:plan/id preview))]
    (is (= :partial (:status result)))
    (is (:uncertain? result))
    (is (= :floating-enable (get-in result [:attempted :op])))
    (is (empty? (:applied result)))
    (is (true? (get-in @world [:windows 10 :floating?])))
    (is (= 1 (count @calls)))
    (is (false? (:ok? (i3/apply! c (:plan/id preview)))))
    (is (= 1 (count @calls)))))

(deftest changed-geometry-after-an-acknowledged-resize-stops-further-effects
  (let [{:keys [world calls read execute]} (harness)
        c (i3/controller {:read! read :execute! (fn [op command]
                                                (let [result (execute op command)]
                                                  (when (= :resize (:op op))
                                                    (swap! world assoc-in [:windows (:id op) :rect :width] 700))
                                                  result))})
        preview (i3/preview! c profile)
        result (i3/apply! c (:plan/id preview))]
    (is (= :desktop/effect-not-established (:error/code result)))
    (is (= :partial (:status result)))
    (is (= 2 (count @calls)))
    (is (= 700 (get-in @world [:windows 10 :rect :width])))))
