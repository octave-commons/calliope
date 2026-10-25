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
          applied (i3/apply! controller (:plan/id preview))
          after (i3/read-snapshot! socket)
          evidence {:kind :isolated-real-i3-effects :clients :disposable-xmessage
                    :profile profile :preview preview :applied applied
                    :before before :after after :commands @calls}]
      (spit result-path (str (pr-str evidence) "\n"))
      (assert (:ok? applied) (pr-str applied))
      (assert (= (get-in before [:snapshot :focus]) (get-in after [:snapshot :focus])))
      (let [n (count @calls)]
        (assert (false? (:ok? (i3/apply! controller (:plan/id preview)))))
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
            :manual-change-cancelled-with-zero-commands true :result result-path}))
    (finally (shutdown-agents))))
