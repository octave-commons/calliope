(require '[calliope.studio.i3 :as i3] '[calliope.studio.desktop :as desktop]
         '[clojure.edn :as edn] '[clojure.java.io :as io])

(let [[socket studio daw result-path] *command-line-args*
      _ (assert (.exists (io/file (.getParent (io/file socket)) ".exclusive-studio-desktop-probe")))
      profile {:profile/type :music-v1 :studio/id (edn/read-string studio) :daw/id (edn/read-string daw)}
      base (i3/native-controller socket) calls (atom [])
      controller (assoc base :execute! (fn [operation command]
                                         (swap! calls conj operation)
                                         ((:execute! base) operation command)))]
  (try
    (let [before (i3/read-snapshot! socket)
          _ (assert (:ok? before))
          _ (assert (= 2 (count (get-in before [:snapshot :windows]))))
          preview (i3/preview! controller profile)
          _ (assert (:ok? preview))
          initial-applied (i3/apply! controller (:plan/id preview))
          after-initial (i3/read-snapshot! socket)
          _ (assert (= :partial (:status initial-applied)) (pr-str initial-applied))
          _ (assert (= :desktop/user-change (:error/code initial-applied)))
          _ (assert (= 1 (count @calls)))
          _ (assert (= :floating-enable (:op (first @calls))))
          _ (assert (not= (get-in before [:snapshot :windows (:daw/id profile) :rect])
                          (get-in after-initial [:snapshot :windows (:daw/id profile) :rect])))
          ;; A second explicit simulated-operator action, not a controller retry.
          fresh-preview (i3/preview! controller profile)
          _ (assert (:ok? fresh-preview))
          _ (assert (not= (:plan/id preview) (:plan/id fresh-preview)))
          applied (i3/apply! controller (:plan/id fresh-preview))
          after (i3/read-snapshot! socket)
          evidence {:kind :isolated-real-i3-effects :clients :disposable-xmessage
                    :profile profile :preview preview :initial-apply initial-applied
                    :after-initial after-initial :fresh-preview fresh-preview
                    :explicit-fresh-preview-after-partial :simulated-operator
                    :applied applied :before before :after after :commands @calls}]
      (spit result-path (str (pr-str evidence) "\n"))
      (assert (:ok? applied) (pr-str applied))
      (assert (= (get-in before [:snapshot :focus]) (get-in after [:snapshot :focus])))
      (let [n (count @calls)]
        (assert (false? (:ok? (i3/apply! controller (:plan/id fresh-preview)))))
        (assert (= n (count @calls))))
      (let [preview (i3/preview! controller profile)
            manual {:op :move :id (:studio/id profile) :x 20 :y 20}
            _ (assert (:ok? ((:execute! base) manual (desktop/command manual))))
            n (count @calls)
            stale (i3/apply! controller (:plan/id preview))]
        (assert (= :desktop/stale-plan (:error/code stale)))
        (assert (= n (count @calls)))
        (spit result-path (str (pr-str (assoc evidence :manual-change-cancellation stale :duplicate-apply-zero-effects true)) "\n")))
      (prn {:ok? true :real-i3-commands (count @calls) :focus-unchanged true
            :initial-reflow-cancelled true :explicit-fresh-preview :simulated-operator
            :manual-change-cancelled-with-zero-commands true :result result-path}))
    (finally (shutdown-agents))))
