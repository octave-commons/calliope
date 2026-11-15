(ns calliope.studio.proposals
  "GPL-3.0-or-later. Bounded local worker; no durable-agent execution claim."
  (:require [calliope.studio.session :as session])
  (:import [java.util.concurrent ThreadPoolExecutor ArrayBlockingQueue TimeUnit
            ThreadFactory Executors RejectedExecutionException Future]
           [java.security MessageDigest]
           [java.nio.charset StandardCharsets]
           [java.util HexFormat]))

(defn prompt-digest [text]
  (.formatHex (HexFormat/of) (.digest (MessageDigest/getInstance "SHA-256")
                                    (.getBytes ^String text StandardCharsets/UTF_8))))

(defn- daemon-factory [label]
  (reify ThreadFactory
    (newThread [_ runnable] (doto (Thread. runnable label) (.setDaemon true)))))

(defn- worker! [store]
  (let [state (:state store)]
    (locking state
      (or @(:proposal/worker store)
          (let [pool (ThreadPoolExecutor. 1 1 0 TimeUnit/MILLISECONDS (ArrayBlockingQueue. 1)
                                          (daemon-factory "calliope-proposal-worker"))
                timer (Executors/newSingleThreadScheduledExecutor (daemon-factory "calliope-proposal-timeout"))
                worker {:pool pool :timer timer}]
            (reset! (:proposal/worker store) worker)
            (swap! (:close-hooks store) conj #(do (.shutdownNow pool) (.shutdownNow timer)))
            worker)))))

(defn- valid-options? [options]
  (and (= #{:timeout-ms :provider/id :model/id :execution/kind} (set (keys options)))
       (integer? (:timeout-ms options)) (<= 100 (:timeout-ms options) 30000)
       (every? #(and (string? %) (<= 1 (count %) 200)) ((juxt :provider/id :model/id) options))
       (contains? #{:fixture :local-inference} (:execution/kind options))))

(defn start!
  "Native user requests a proposal. The configured provider receives only bounded
   metadata and returns {:text ... :labels [...]}; it receives no user authority,
   store or audio bytes. A fixed one-thread/one-queued-job worker bounds ignored
   interrupts. Timeout/failure is a separate completion, never a human decision."
  [store actor command provider options]
  (when-not (and (ifn? provider) (valid-options? options)
                 (= :studio/request-proposal (:command/type command)))
    (throw (ex-info "Invalid local proposal worker configuration" {:error/code :proposal/configuration})))
  (let [request (session/ui-command! store actor command)]
    (if-not (= :ok (:result/status request)) {:request request}
      (let [channel (session/proposal-channel! store actor (:proposal/id command))
            {:keys [pool timer]} (worker! store)
            done (atom false) completion (promise) running (atom nil)
            finish! (fn [payload]
                      (when (compare-and-set! done false true)
                        (deliver completion (channel payload))))
            failure! (fn [code message] (finish! {:failure/code code :failure/message message}))
            source (get-in (session/view store) [:session :clips (:clip/id command)])
            input {:prompt (:proposal/prompt command) :clip source :input/kind :synthetic-metadata}
            timeout-task (.schedule ^java.util.concurrent.ScheduledExecutorService timer
                                    ^Runnable (fn []
                                                (when (compare-and-set! done false true)
                                                  (when-let [task @running] (.cancel ^Future task true))
                                                  (.purge ^ThreadPoolExecutor pool)
                                                  (deliver completion (channel {:failure/code :timeout
                                                                                :failure/message "Local provider exceeded its configured deadline"}))))
                                    (long (:timeout-ms options)) TimeUnit/MILLISECONDS)]
        (try
          (reset! running
                  (.submit ^ThreadPoolExecutor pool
                           ^Runnable
                           (fn []
                             (try
                               (let [output (provider input)]
                                 (if-not (and (map? output) (= #{:text :labels} (set (keys output))))
                                   (failure! :invalid-output "Provider returned an invalid result shape")
                                   (let [payload {:proposal/output
                                                  (merge output (select-keys options [:provider/id :model/id :execution/kind])
                                                         {:prompt/sha256 (prompt-digest (:prompt input))
                                                          :evidence/refs [(str "event:" (:source/event source))]
                                                          :input/kind :synthetic-metadata})}
                                         ;; The channel validates the complete closed result.
                                         result (when (compare-and-set! done false true) (channel payload))]
                                     (when result
                                       (if (= :command/invalid (:error/code result))
                                         (deliver completion (channel {:failure/code :invalid-output :failure/message "Provider result failed the output contract"}))
                                         (deliver completion result))))))
                               (catch java.io.IOException _ (failure! :unavailable "Configured local provider is unavailable"))
                               (catch InterruptedException _ (failure! :failed "Local provider was interrupted"))
                               (catch Exception _ (failure! :failed "Local provider failed"))
                               (finally (.cancel ^Future timeout-task false))))))
          (catch RejectedExecutionException _
            (.cancel ^Future timeout-task false)
            (failure! :unavailable "Local proposal worker is busy; capture remains available")))
        {:request request :completion completion}))))
