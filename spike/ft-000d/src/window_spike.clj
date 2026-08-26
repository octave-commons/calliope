(ns window-spike
  "FT-000D spike B: minimal native window (cljfx/JavaFX, no browser runtime)
  wired through an in-process command/query boundary (no HTTP server).

  The window lists liked clips from the suno-meta projection and issues
  transport commands through the same boundary the future player shell will
  use. Auto-demonstrates play/pause through the boundary and exits.
  Run: clojure -M -m window-spike"
  (:require [cljfx.api :as fx]
            [clojure.edn :as edn]))

(import '[javafx.application Platform])

;; -------------------------------------------------- in-process boundary
;; Transport-neutral: the UI calls handle-command / handle-query as plain
;; functions. No HTTP server, no ledger writes, no FFmpeg — adapters sit
;; behind the boundary, not in the view.

(defonce *state
  (atom {:status :stopped :items [] :commands-served 0}))

(defn handle-command [command]
  (swap! *state update :commands-served inc)
  (case (:command/type command)
    :transport/play  (swap! *state assoc :status :playing)
    :transport/pause (swap! *state assoc :status :paused)
    :transport/stop  (swap! *state assoc :status :stopped)
    (swap! *state assoc :last-error (str "unknown command " command))))

(defn handle-query [query]
  (case (:query/type query)
    :library/liked-titles (:items @*state)
    :transport/status (:status @*state)))

;; ------------------------------------------------------------------- view

(defn root-view [{:keys [status items]}]
  {:fx/type :stage
   :showing true
   :title "FT-000D spike — native Clojure/JVM window"
   :width 640
   :height 400
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :spacing 10
                  :padding 16
                  :children
                  [{:fx/type :label
                    :text "Native window — cljfx/JavaFX, no browser runtime"}
                   {:fx/type :label
                    :text (str "Transport (via command boundary): "
                               (name status))}
                   {:fx/type :label :text "Liked clips (via query boundary):"}
                   {:fx/type :list-view
                    :pref-height 240
                    :items (vec items)}
                   {:fx/type :h-box
                    :spacing 8
                    :children
                    [{:fx/type :button
                      :text "Play"
                      :on-action (fn [_]
                                   (handle-command
                                    {:command/type :transport/play}))}
                     {:fx/type :button
                      :text "Pause"
                      :on-action (fn [_]
                                   (handle-command
                                    {:command/type :transport/pause}))}
                     {:fx/type :button
                      :text "Stop"
                      :on-action (fn [_]
                                   (handle-command
                                    {:command/type :transport/stop}))}]}]}}})

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root-view)))

(defn -main [& _]
  (let [projection (edn/read-string
                    (slurp "../../ledgers/projections/suno-meta-v1.edn"))
        liked (get-in projection [:index :liked])
        titles (->> liked
                    (take 12)
                    (mapv (fn [id]
                            (get-in projection [:clips id :clip/title]))))]
    (swap! *state assoc :items titles)
    (println "query boundary -> :library/liked-titles returned"
             (count titles) "items")
    (fx/mount-renderer *state renderer)
    (println "window shown on" (System/getProperty "os.name")
             "via JavaFX" (System/getProperty "javafx.version"))
    (Thread/sleep 1500)
    (println "command boundary -> :transport/play")
    (handle-command {:command/type :transport/play})
    (Thread/sleep 1500)
    (println "command boundary -> :transport/pause")
    (handle-command {:command/type :transport/pause})
    (Thread/sleep 1500)
    (println "final transport status:" (handle-query {:query/type :transport/status}))
    (println "commands served through boundary:"
             (:commands-served @*state))
    (Platform/exit)
    (println "WINDOW SPIKE: PASS")
    (shutdown-agents)
    (System/exit 0)))
