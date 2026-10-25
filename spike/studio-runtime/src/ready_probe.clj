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
      (doseq [channel [:media :player]]
        (let [polls (atom 0)
              cause (ex-info "Synthetic asynchronous error" {:injected/channel channel})
              error! #(when (= 2 (swap! polls inc)) cause)
              media (reify MediaSnapshot (getError [_] (when (= channel :media) (error!))))
              player (reify PlayerSnapshot
                       (getMedia [_] media)
                       (getError [_] (when (= channel :player) (error!)))
                       (getStatus [_] "UNKNOWN"))]
          (try
            (probe/await-media-ready! player)
            (throw (ex-info "Injected error was not reported" {}))
            (catch clojure.lang.ExceptionInfo e
              (when-not (and (= "Native media failed before READY" (ex-message e))
                             (identical? cause (.getCause e)) (= 2 @polls))
                (throw (ex-info "Wrong injected-error outcome" {:channel channel} e)))
              (println "INJECTED ERROR PASS:" channel "two polls, original cause retained")))))
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
