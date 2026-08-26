(ns audio-spike
  "FT-000D spike A: JavaFX Media playback against a real corpus MP3.

  Demonstrates: toolkit init, duration report, play, seek, pause, resume,
  stop, and clean JVM exit. Run:
    clojure -M -m audio-spike <path-to-mp3>")

(import '[javafx.application Platform]
        '[javafx.scene.media Media MediaPlayer]
        '[javafx.util Duration]
        '[java.util.concurrent CountDownLatch TimeUnit])

(defn -main [& [mp3-path]]
  (let [mp3 (or mp3-path "../../tracks/aquila-regina/211cce48.mp3")
        file (java.io.File. mp3)]
    (when-not (.exists file)
      (println "FAIL: file not found:" (.getAbsolutePath file))
      (System/exit 1))
    (println "media:" (.getAbsolutePath file) (str "(" (.length file) " bytes)"))
    (Platform/startup (fn []))
    (let [ready (CountDownLatch. 1)
          media (Media. (str (.toURI file)))
          player (doto (MediaPlayer. media)
                   (.setOnReady (fn [] (.countDown ready)))
                   (.setVolume 0.25))]
      (if (.await ready 15 TimeUnit/SECONDS)
        (let [total (.toMillis (.getDuration media))]
          (println (format "duration: %.0f ms (%.1f s)" total (/ total 1000.0)))
          (println "play 2.0 s at volume 0.25 ...")
          (.play player)
          (Thread/sleep 2000)
          (let [seek-to (* 0.3 total)]
            (println (format "seek to %.0f ms (30%%) ..." seek-to))
            (.seek player (Duration/millis seek-to))
            (Thread/sleep 500)
            (println (format "position after seek: %.0f ms"
                             (.toMillis (.getCurrentTime player)))))
          (println "pause 1.0 s ...")
          (.pause player)
          (Thread/sleep 1000)
          (println (format "status while paused: %s (position %.0f ms)"
                           (.getStatus player)
                           (.toMillis (.getCurrentTime player))))
          (println "resume 1.5 s ...")
          (.play player)
          (Thread/sleep 1500)
          (println (format "status while playing: %s (position %.0f ms)"
                           (.getStatus player)
                           (.toMillis (.getCurrentTime player))))
          (.stop player)
          (.dispose player)
          (println "stop+dispose: clean")
          (println "AUDIO SPIKE: PASS"))
        (do (println "FAIL: media not ready within 15 s")
            (System/exit 1))))
    (Platform/exit)
    (shutdown-agents)
    (System/exit 0)))
