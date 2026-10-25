(ns ready-probe
  "Synthetic error-getter injection through the real FX-thread poll boundary."
  (:require [native-probe :as probe]
            [cljfx.api :as fx])
  (:import [javafx.application Platform]))

(definterface MediaSnapshot (getError []))
(definterface PlayerSnapshot (getMedia []) (getError []) (getStatus []))

(defn -main [& _]
  (let [renderer (fx/create-renderer :middleware (fx/wrap-map-desc assoc :fx/type probe/view))]
    (fx/mount-renderer probe/state renderer)
    (try
      (doseq [phase [:ready :transport] channel [:media :player]]
        (let [polls (atom 0)
              status (atom (if (= phase :ready) "UNKNOWN" "READY"))
              cause (ex-info "Synthetic asynchronous error" {:injected/channel channel})
              error! #(when (= 2 (swap! polls inc)) (reset! status "HALTED") cause)
              media (reify MediaSnapshot (getError [_] (when (= channel :media) (error!))))
              player (reify PlayerSnapshot
                       (getMedia [_] media)
                       (getError [_] (when (= channel :player) (error!)))
                       (getStatus [_] @status))]
          (try
            (if (= phase :ready)
              (probe/await-media-ready! player)
              (probe/wait-media! player #(= "PLAYING" (str (.getStatus %)))))
            (throw (ex-info "Injected error was not reported" {}))
            (catch clojure.lang.ExceptionInfo e
              (when-not (and (identical? cause (.getCause e)) (= 2 @polls)
                             (= "HALTED" (:media/status (ex-data e))))
                (throw (ex-info "Wrong injected-error outcome" {:phase phase :channel channel} e)))
              (println "INJECTED ERROR PASS:" phase channel "two polls, original cause retained")))))
      (let [media (reify MediaSnapshot (getError [_] nil))
            player (reify PlayerSnapshot
                     (getMedia [_] media) (getError [_] nil) (getStatus [_] "HALTED"))]
        (try
          (probe/wait-media! player #(= "PLAYING" (str (.getStatus %))))
          (throw (ex-info "Unreported HALTED state was not refused" {}))
          (catch clojure.lang.ExceptionInfo e
            (when-not (= :media/halted-without-error (:media/error-code (ex-data e)))
              (throw (ex-info "HALTED was masked by another failure" {} e)))
            (println "HALTED PASS: explicit failure without inventing a reported cause"))))
      (let [media (reify MediaSnapshot (getError [_] nil))
            player (reify PlayerSnapshot
                     (getMedia [_] media) (getError [_] nil) (getStatus [_] "READY"))]
        (when-not (true? (probe/await-media-ready! player))
          (throw (ex-info "Unfailed READY state was refused" {})))
        (println "READY PASS: no reported error"))
      (finally
        (fx/unmount-renderer probe/state renderer)
        (Platform/exit)
        (shutdown-agents)))))
