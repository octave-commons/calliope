(ns calliope.companion.history-test
  (:require [calliope.companion.desktop :as desktop]
            [calliope.companion.review :as review]
            [calliope.companion.store :as store]
            [calliope.suno.metadata-test :as fixture]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]])
  (:import [javax.swing JList JLabel SwingUtilities]
           [javax.swing.tree TreePath DefaultMutableTreeNode]))

(def id fixture/id)
(def other-id fixture/other-id)
(defn revision [name parent patch]
  {:event/id name :event/type :listening/review-recorded :schema :calliope.listening/review-v2
   :clip/id id :review/parent parent :review/patch patch :ts "2026-10-04T00:00:00Z"})
(defn move [from to action]
  {:event/id (str (java.util.UUID/randomUUID)) :event/type :listening/history-moved
   :schema :calliope.listening/history-v1 :clip/id id :history/from from :history/to to :history/action action})

(deftest branching-undo-preserves-abandoned-futures-and-cursor-on-replay
  (let [a (revision "A" nil {:note "A"}) b (revision "B" "A" {:note "B"})
        c (revision "C" "A" {:note "C" :ratings {:technical-quality 0}})
        events [a b (move "B" "A" :undo) c (move "C" "A" :undo)]
        state (review/replay events)]
    (is (= ["B" "C"] (get-in state [:nodes "A" :children])))
    (is (= "A" (get-in state [:heads id])))
    (is (= "C" (review/redo-target state id)))
    (is (= "B" (:note (review/record-at state id "B"))))
    (is (= 0 (get-in state [:nodes "C" :record :ratings :technical-quality])))
    (let [redo-event (move "A" "C" :redo) checkout-event (move "C" "B" :checkout)
          redo (review/apply-event state redo-event)
          checkout (review/apply-event redo checkout-event)]
      (is (= "C" (get-in redo [:latest id :note])))
      (is (= "B" (get-in checkout [:latest id :note])))
      (is (= checkout (review/replay (conj events redo-event checkout-event)))))
    (testing "Undo to the baseline retains and redoes its last visited child"
      (let [root (review/apply-event state (move "A" nil :undo))]
        (is (not (review/can-undo? root id)))
        (is (nil? (get-in root [:latest id])))
        (is (= "A" (review/redo-target root id)))
        (let [new-root (review/apply-event root (revision "D" nil {:labels ["other idea"]}))]
          (is (= ["A" "D"] (get-in new-root [:roots id])))
          (is (= "D" (review/redo-target (review/apply-event new-root (move "D" nil :undo)) id))))))))

(deftest invalid-links-patches-and-stale-cursors-never-replay-as-truth
  (let [a (revision "A" nil {:note "A"}) state (review/replay [a])]
    (doseq [bad [(revision "A" "A" {:note "duplicate"})
                 (revision "B" "missing" {:note "dangling"})
                 (revision "B" nil {:note "stale"})
                 (revision "B" "A" {})
                 (revision "B" "A" {:clip/id other-id})
                 (revision "B" "A" {:note nil})
                 (revision "B" "A" {:ratings {:enjoyment 9}})
                 (assoc (revision "B" "A" {:note "cross clip"}) :clip/id other-id)
                 (move "stale" nil :undo) (move "A" "missing" :checkout)
                 (move "A" nil :redo) (move "A" nil :unknown)
                 (dissoc (move "A" nil :undo) :history/to)
                 (assoc (move "A" nil :undo) :schema :unknown)
                 (assoc (move "A" nil :undo) :event/id "A")]]
      (is (thrown? clojure.lang.ExceptionInfo (review/apply-event state bad))))
    (let [other (review/apply-event state (assoc (revision "O" nil {:note "Other"}) :clip/id other-id))]
      (is (thrown? clojure.lang.ExceptionInfo (review/apply-event other (move "A" "O" :checkout)))))))

(defn imported-root []
  (let [root (fixture/temp-dir) source (io/file root "clips.json")]
    (spit source (json/write-str [fixture/clip (assoc fixture/clip "id" other-id)]))
    (store/import! root source) root))

(deftest real-store-replays-legacy-records-patches-and-branches-without-rewriting
  (let [root (imported-root) ledger (io/file root "ledgers/studio.edn")
        legacy {:event/id "legacy" :event/type :listening/review-recorded :schema :calliope.listening/review-v1
                :ts "2026-10-03T00:00:00Z" :review (assoc (review/empty-review id) :note "Old 川")}
        prefix (pr-str legacy)]
    (try
      ;; Deliberately no trailing newline: append must preserve every original byte.
      (spit ledger prefix)
      (let [before (store/reviews root) record (assoc (get-in before [:latest id]) :note "B")
            b (store/save-review! root record "legacy")]
        (is (= {:note "B"} (:review/patch b)))
        (is (not (contains? b :review)))
        (is (.startsWith (slurp ledger) prefix))
        (is (= "legacy" (:review/parent b)))
        (let [bytes (slurp ledger)]
          (is (nil? (store/save-review! root record (:event/id b))))
          (is (= bytes (slurp ledger)))
          (is (thrown? clojure.lang.ExceptionInfo (store/save-review! root (assoc record :note "stale") "legacy")))
          (is (= bytes (slurp ledger))))
        (store/move-history! root id :undo nil (:event/id b))
        (let [c (store/save-review! root (assoc (review/empty-review id) :note "C") "legacy")]
          (is (= [(:event/id b) (:event/id c)] (get-in (store/reviews root) [:nodes "legacy" :children])))
          (store/move-history! root id :undo nil (:event/id c))
          (is (= (:event/id c) (review/redo-target (store/reviews root) id)))
          (store/move-history! root id :redo nil "legacy")
          (store/move-history! root id :checkout (:event/id b) (:event/id c))
          (let [state (store/reviews root) bytes (slurp ledger)]
            (is (= "B" (get-in state [:latest id :note])))
            (is (= state (review/replay (store/read-events ledger))))
            (is (nil? (store/move-history! root id :redo nil (:event/id b))))
            (is (= bytes (slurp ledger)))
            (is (.startsWith bytes prefix)))))
      (finally (fixture/clean! root)))))

(defn edt [f]
  (let [result (promise)]
    (SwingUtilities/invokeAndWait
     (bound-fn [] (deliver result (try {:value (f)} (catch Throwable e {:error e})))))
    (if-let [e (:error @result)] (throw e) (:value @result))))
(defn await! [predicate]
  (loop [remaining 400]
    (cond (predicate) true (zero? remaining) false
          :else (do (Thread/sleep 20) (recur (dec remaining))))))
(defn idle? [ui]
  (let [state @(:state ui)] (and (not (:busy? state)) (empty? (:queue state)) (empty? (:drafts state)))))
(defn ui! [root]
  (edt #(do (desktop/theme!) (desktop/make-panel root (:clips (store/catalog root)) (store/reviews root)))))
(defn dispose! [ui] (when ui (edt #((get-in ui [:actions :dispose])))))
(defn select! [ui id]
  (let [table (:table ui) index (first (keep-indexed #(when (= id (:clip/id %2)) %1) (:rows @(:state ui))))
        view (.convertRowIndexToView table index)]
    (.setRowSelectionInterval table view view)))

(deftest click-selection-keeps-table-and-combo-text-visible
  (let [root (imported-root) ui (ui! root)]
    (try
      (edt
       #(let [table (:table ui)]
          (doseq [row (range (.getRowCount table))]
            (.setRowSelectionInterval table row row)
            (doseq [column (range (.getColumnCount table)) selected? [false true] focused? [false true]]
              (let [value (.getValueAt table row column)
                    label (.getTableCellRendererComponent (.getCellRenderer table row column) table value selected? focused? row column)]
                (is (= (str value) (.getText ^JLabel label)))
                (is (not= (.getForeground label) (.getBackground label))))))
          (doseq [combo (cons (:disposition ui) (vals (:ratings ui))) index (range (.getItemCount combo))]
            (.setSelectedIndex combo index)
            (doseq [selected? [false true]]
              (let [label (.getListCellRendererComponent (.getRenderer combo) (JList.) (.getSelectedItem combo) -1 selected? true)]
                (is (= (.getSelectedItem combo) (.getText ^JLabel label)))
                (is (not= (.getForeground label) (.getBackground label))))))))
      (is (await! #(idle? ui)))
      (finally (dispose! ui) (fixture/clean! root)))))

(deftest autosave-retains-partial-markers-and-close-flushes-the-last-edit
  (let [root (imported-root) ui (ui! root) closed (promise)]
    (try
      (edt #(do (.setText (:note ui) "川: a useful motif") (.setText (:start ui) "1:")))
      (is (await! #(idle? ui)))
      (let [record (get-in (store/reviews root) [:latest id])]
        (is (= "川: a useful motif" (:note record)))
        (is (= {:start "1:" :end ""} (:marker-input record)))
        (is (nil? (:span record))))
      (edt #(do (select! ui other-id) (select! ui id)
                (is (= "1:" (.getText (:start ui))))
                (.setText (:start ui) "1:03") (.setText (:end ui) "1:08")
                (.setText (:note ui) "Last thought before closing")
                ((get-in ui [:actions :close]) (fn [] (deliver closed true)))))
      (is (= true (deref closed 8000 :timeout)))
      (let [record (get-in (store/reviews root) [:latest id])]
        (is (= "Last thought before closing" (:note record)))
        (is (= {:start 63.0 :end 68.0} (:span record))))
      (finally (dispose! ui) (fixture/clean! root)))))

(deftest navigation-and-late-save-callbacks-cannot-steal-the-new-song-form
  (let [root (imported-root) ui (ui! root) entered (promise) release (promise)
        real-save store/save-review! calls (atom 0)]
    (try
      (with-redefs [store/save-review! (fn [& args]
                                      (when (= 1 (swap! calls inc))
                                        (deliver entered true)
                                        (when (= :timeout (deref release 8000 :timeout)) (throw (ex-info "Test gate timed out" {}))))
                                      (apply real-save args))]
        (edt #(do (.setText (:note ui) "First thought") ((get-in ui [:actions :save]))))
        (is (= true (deref entered 8000 :timeout)))
        (edt #(do
                (is (.isEnabled (:table ui))) (is (.isEnabled (:note ui)))
                (select! ui other-id) (.setText (:note ui) "Second song")
                (select! ui id) (is (= "First thought" (.getText (:note ui))))
                (.setText (:note ui) "First thought amended")
                (select! ui other-id) (is (= "Second song" (.getText (:note ui))))
                ((get-in ui [:actions :flush]))))
        (deliver release true)
        (is (await! #(idle? ui))))
      (let [state (store/reviews root)]
        (is (= "First thought amended" (get-in state [:latest id :note])))
        (is (= "Second song" (get-in state [:latest other-id :note]))))
      (edt #(do (is (= other-id (:selected @(:state ui))))
                (is (= "Second song" (.getText (:note ui))))))
      (finally (deliver release true) (dispose! ui) (fixture/clean! root)))))

(deftest actual-history-tree-restores-a-sibling-and-reopens-at-that-cursor
  (let [root (imported-root) ui (ui! root)]
    (try
      (edt #(.setText (:note ui) "A")) (is (await! #(idle? ui)))
      (let [a (get-in (store/reviews root) [:heads id])]
        (edt #(.setText (:labels ui) "branch/B")) (is (await! #(idle? ui)))
        (let [b (get-in (store/reviews root) [:heads id])]
          (edt #(.doClick (:undo ui))) (is (await! #(idle? ui)))
          (edt #(do (is (= "" (.getText (:labels ui)))) (is (.isEnabled (:redo ui)))
                    (.doClick (:redo ui))))
          (is (await! #(idle? ui)))
          (edt #(do (is (= "branch/B" (.getText (:labels ui)))) (.doClick (:undo ui))))
          (is (await! #(idle? ui)))
          (edt #(.setText (:labels ui) "branch/C"))
          (is (await! #(idle? ui)))
          (let [c (get-in (store/reviews root) [:heads id])]
            (is (= [b c] (get-in (store/reviews root) [:nodes a :children])))
            (edt #(let [item (first (for [[node revision] @(:tree-ids ui) :when (= revision b)] node))]
                    (is (some? item))
                    (.setSelectionPath (:history-tree ui) (TreePath. (.getPath ^DefaultMutableTreeNode item)))
                    (.doClick (:restore ui))))
            (is (await! #(idle? ui)))
            (edt #(is (= "branch/B" (.getText (:labels ui)))))
            (let [reopened (ui! root)]
              (try
                (edt #(do (is (= "branch/B" (.getText (:labels reopened))))
                          (is (= b (get-in @(:state reopened) [:reviews :heads id])))
                          (is (= [b c] (get-in @(:state reopened) [:reviews :nodes a :children])))))
                (finally (dispose! reopened)))))))
      (finally (dispose! ui) (fixture/clean! root)))))

(deftest failed-write-retains-the-draft-and-prevents-close-until-retry
  (let [root (imported-root) ui (ui! root) closed (promise)]
    (try
      (with-redefs [store/save-review! (fn [& _] (throw (ex-info "Disk unavailable" {})))]
        (edt #(do (.setText (:note ui) "Keep this draft") ((get-in ui [:actions :save]))))
        (is (await! #(some? (:error @(:state ui)))))
        (edt #(do (is (= "Keep this draft" (.getText (:note ui))))
                  (is (= "Keep this draft" (get-in @(:state ui) [:drafts id :record :note])))
                  ((get-in ui [:actions :close]) (fn [] (deliver closed true)))))
        (is (await! #(and (some? (:error @(:state ui))) (not (:busy? @(:state ui))))))
        (is (not (realized? closed))))
      (edt #((get-in ui [:actions :reload])))
      (is (await! #(idle? ui)))
      (is (= "Keep this draft" (get-in (store/reviews root) [:latest id :note])))
      (edt #((get-in ui [:actions :close]) (fn [] (deliver closed true))))
      (is (= true (deref closed 8000 :timeout)))
      (finally (dispose! ui) (fixture/clean! root)))))
