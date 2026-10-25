(ns calliope.studio.desktop
  "Pure music layout planning and fixed i3 command compilation."
  (:require [calliope.law.desktop :as law]))

(defn command [operation]
  (when-not (law/valid? :calliope.desktop/operation-v1 operation)
    (throw (ex-info "Invalid fixed desktop operation" {:error/code :desktop/invalid-operation})))
  (let [{:keys [op id width height x y]} operation prefix (str "[con_id=" id "] ")]
    (str prefix (case op
                  :floating-enable "floating enable"
                  :resize (str "resize set width " width " px height " height " px")
                  :move (str "move absolute position " x " px " y " px")))))

(defn- reason [snapshot profile id]
  (let [window (get-in snapshot [:windows id])]
    (cond
      (contains? (:opt-outs profile) id) :opted-out
      (nil? window) :missing
      (not= (:workspace window) (get-in snapshot [:workspace :id])) :different-workspace
      (:fullscreen? window) :fullscreen
      (or (< (get-in window [:rect :width]) 640) (< (get-in window [:rect :height]) 480)) :undersized)))

(defn- operations [snapshot {:keys [id rect]}]
  (let [window (get-in snapshot [:windows id])]
    (cond-> []
      (not (:floating? window)) (conj {:op :floating-enable :id id})
      (or (not (:floating? window))
          (not= (select-keys rect [:width :height]) (select-keys (:rect window) [:width :height])))
      (conj (merge {:op :resize :id id} (select-keys rect [:width :height])))
      (or (not (:floating? window))
          (not= (select-keys rect [:x :y]) (select-keys (:rect window) [:x :y])))
      (conj (merge {:op :move :id id} (select-keys rect [:x :y]))))))

(defn plan [snapshot profile]
  (cond
    (not (and (law/valid? :calliope.desktop/snapshot-v1 snapshot)
               (contains? (:windows snapshot) (:focus snapshot))
               (= (get-in snapshot [:windows (:focus snapshot) :workspace]) (get-in snapshot [:workspace :id]))
               (every? (fn [[id window]] (= id (:id window))) (:windows snapshot))))
    {:ok? false :error/code :desktop/invalid-snapshot}
    (not (and (law/valid? :calliope.desktop/profile-v1 profile)
               (not= (:studio/id profile) (:daw/id profile))))
    {:ok? false :error/code :desktop/invalid-profile}
    :else
    (let [ids (cond-> [(:studio/id profile)] (:daw/id profile) (conj (:daw/id profile)))
          skipped (vec (keep (fn [id] (when-let [why (reason snapshot profile id)] {:id id :reason why})) ids))
          eligible (filterv #(nil? (reason snapshot profile %)) ids)
          {:keys [x y width height] :as area} (get-in snapshot [:workspace :rect])
          fits-one? (and (>= width 640) (>= height 480))
          fits-two? (and fits-one? (>= width 1280))
          targets (cond
                    (not fits-one?) []
                    (and (= 2 (count eligible)) fits-two?)
                    [{:id (first eligible) :rect {:x x :y y :width (quot width 2) :height height}}
                     {:id (second eligible) :rect {:x (+ x (quot width 2)) :y y
                                                  :width (- width (quot width 2)) :height height}}]
                    (seq eligible) [{:id (first eligible) :rect area}]
                    :else [])
          selected (set (map :id targets))
          skipped (into skipped (for [id eligible :when (not (contains? selected id))] {:id id :reason :undersized}))
          ops (vec (mapcat #(operations snapshot %) targets))
          plan {:profile/type :music-v1 :expected/revision (:revision snapshot)
                :expected/focus (:focus snapshot) :targets targets :operations ops :skipped skipped}]
      (if (law/valid? :calliope.desktop/plan-v1 plan)
        {:ok? true :plan plan}
        {:ok? false :error/code :desktop/invalid-layout}))))
