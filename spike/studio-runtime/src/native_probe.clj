(ns native-probe
  "Disposable native discovery; no production completion claims."
  (:require [cljfx.api :as fx]
            [clojure.edn :as edn])
  (:import [javafx.application Platform]
           [javafx.scene.media Media MediaPlayer]
           [javafx.util Duration]
           [java.sql DriverManager]
           [java.io File]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]))

(defn on-fx [f]
  (let [result (promise)]
    (Platform/runLater #(try (deliver result [:ok (f)])
                            (catch Throwable t (deliver result [:error t]))))
    (let [[status value] (deref result 10000 [:error (ex-info "FX timeout" {})])]
      (if (= :ok status) value (throw value)))))

(defn wait-for [f]
  (loop [n 0]
    (cond (f) true
          (> n 100) (throw (ex-info "Native transition timed out" {}))
          :else (do (Thread/sleep 100) (recur (inc n))))))

(defonce state (atom {:status :stopped}))
(defn command! [command]
  (case command
    :play (swap! state assoc :status :playing)
    :stop (swap! state assoc :status :stopped)))
(defn query [] @state)
(defn view [{:keys [status]}]
  {:fx/type :stage :showing true :title "Calliope native runtime discovery"
   :width 640 :height 480
   :scene {:fx/type :scene
           :root {:fx/type :v-box :spacing 12 :padding 20
                  :children [{:fx/type :label :text "Calliope studio — native runtime discovery"}
                             {:fx/type :label :text (str "Transport via application boundary: " (name status))}
                             {:fx/type :label :text "No capture armed. Corpus playback uses a private null sink."}
                             {:fx/type :button :text "Play" :on-action (fn [_] (command! :play))}
                             {:fx/type :button :text "Stop" :on-action (fn [_] (command! :stop))}]}}})

(defn rebuild-query [db source]
  (with-open [c (DriverManager/getConnection (str "jdbc:sqlite:" db))
              s (.createStatement c)]
    (.execute s "CREATE TABLE IF NOT EXISTS phrases (id TEXT PRIMARY KEY, note TEXT)")
    (.execute s "DELETE FROM phrases")
    (with-open [p (.prepareStatement c "INSERT INTO phrases VALUES (?, ?)")]
      (doseq [[id note] (edn/read-string (slurp source))]
        (.setString p 1 id) (.setString p 2 note) (.executeUpdate p)))
    (with-open [r (.executeQuery s "SELECT id, note FROM phrases ORDER BY id")]
      (loop [rows []]
        (if (.next r) (recur (conj rows [(.getString r 1) (.getString r 2)])) rows)))))

(defn snapshot! [path]
  (on-fx
   (fn []
     (let [window (first (filter #(.isShowing %) (javafx.stage.Window/getWindows)))
           image (.snapshot (.getScene window) nil)
           width (int (.getWidth image))
           height (int (.getHeight image))
           buffered (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
           reader (.getPixelReader image)]
       (doseq [y (range height) x (range width)]
         (.setRGB buffered x y (.getArgb reader x y)))
       (when-not (ImageIO/write buffered "png" (File. path))
         (throw (ex-info "PNG writer unavailable" {})))))))

(defn -main [& [asset db source screenshot]]
  (when-not (and asset db source
                 (.isFile (File. asset)) (.isFile (File. source)))
    (throw (ex-info "Usage: native-probe existing.mp3 fresh.db phrases.edn [window.png]" {})))
  (when (.exists (File. db))
    (throw (ex-info "Refusing to replace an existing database; supply a fresh scratch path" {:path db})))
  (let [renderer (fx/create-renderer :middleware (fx/wrap-map-desc assoc :fx/type view))]
    (try
      (fx/mount-renderer state renderer)
      (wait-for #(on-fx (fn [] (seq (javafx.stage.Window/getWindows)))))
      (command! :play)
      (when-not (= :playing (:status (query)))
        (throw (ex-info "Command/query play mismatch" {})))
      (command! :stop)
      (when-not (= :stopped (:status (query)))
        (throw (ex-info "Command/query stop mismatch" {})))
      (when screenshot (snapshot! screenshot))
      (println "WINDOW: actual cljfx native stage shown; in-process command/query exercised")
      (let [player (on-fx #(MediaPlayer. (Media. (str (.toURI (File. asset))))))]
        (try
          (on-fx #(.setVolume player 0.1))
          (wait-for #(= "READY" (on-fx (fn [] (str (.getStatus player))))))
          (let [duration (on-fx #(.toMillis (.getTotalDuration player)))]
            (when-not (> duration 1000) (throw (ex-info "Invalid duration" {:duration duration})))
            (println "DURATION:" duration)
            (on-fx #(.play player))
            (wait-for #(= "PLAYING" (on-fx (fn [] (str (.getStatus player))))))
            (wait-for #(> (on-fx (fn [] (.toMillis (.getCurrentTime player)))) 250))
            (on-fx #(.pause player))
            (wait-for #(= "PAUSED" (on-fx (fn [] (str (.getStatus player))))))
            (let [before (on-fx #(.toMillis (.getCurrentTime player)))]
              (Thread/sleep 300)
              (when-not (< (Math/abs (- before (on-fx #(.toMillis (.getCurrentTime player))))) 50)
                (throw (ex-info "Pause did not hold position" {}))))
            (on-fx #(.seek player (Duration/millis 5000)))
            (wait-for #(< (Math/abs (- 5000 (on-fx (fn [] (.toMillis (.getCurrentTime player)))))) 500))
            (on-fx #(.play player))
            (wait-for #(> (on-fx (fn [] (.toMillis (.getCurrentTime player)))) 5250))
            (on-fx #(.stop player))
            (wait-for #(= "STOPPED" (on-fx (fn [] (str (.getStatus player))))))
            (println "AUDIO: play/progress/pause-held/seek/resume-progress/stop asserted"))
          (finally (on-fx #(.dispose player)))))
      (let [rows (rebuild-query db source)]
        (when-not (.delete (File. db)) (throw (ex-info "Could not delete disposable index" {})))
        (when-not (= rows (rebuild-query db source)) (throw (ex-info "Rebuild mismatch" {})))
        (println "SQLITE: deleted and rebuilt; identical query rows" rows))
      (println "ENVIRONMENT:" (System/getProperty "os.name") (System/getProperty "java.version")
               "JavaFX" (System/getProperty "javafx.version"))
      (println "NATIVE DISCOVERY PASS — private Xvfb/null sink; no hardware or human listening claim")
      (catch Throwable t
        (.printStackTrace t)
        (System/exit 1))
      (finally
        (fx/unmount-renderer state renderer)
        (Platform/exit)
        (shutdown-agents)))))
