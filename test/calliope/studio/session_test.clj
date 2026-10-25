(ns calliope.studio.session-test
  (:require [calliope.law.session :as law]
            [calliope.studio.session :as session]
            [calliope.studio.synthetic :as synthetic]
            [calliope.studio.ingress :as ingress]
            [calliope.studio.ingress-test :as fixtures]
            [clio.extern.jvm.fs :as clio-fs]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is]])
  (:import [java.nio.file Files Path LinkOption]
           [java.util UUID]
           [java.util.concurrent TimeUnit]))

(def actor {:actor/id "simulated-session-test-operator" :actor/kind :user})
(def sid "40000000-0000-4000-8000-000000000001")
(def origin (:origin (synthetic/fixture 42)))

(defn command [type revision]
  {:command/type type :command/version 1 :request/id (str (UUID/randomUUID))
   :session/id sid :session/revision revision})

(defn create-command []
  (assoc (command :studio/session-create 0) :session/title "Synthetic music session"
         :work/id "session-test-work" :session/links ["urn:calliope:test:studio"]))

(defn arm-command [revision]
  (assoc (command :studio/arm revision) :capture/clock synthetic/clock :capture/origin origin))

(defn with-store [f]
  (let [parent (fixtures/temp-root) root (.resolve parent "session")
        store (session/create! root actor)]
    (try (f parent root store)
         (finally (session/close! store) (fixtures/delete-tree! parent)))))

(defn setup-capture! [store]
  (is (= :ok (:result/status (session/ui-command! store actor (create-command)))))
  (is (= :ok (:result/status (session/ui-command! store actor (arm-command 1)))))
  (merge (command :studio/ingest 2)
         (synthetic/write-fixture! (session/staging-root store) 42)))

(defn source-files [store]
  (with-open [paths (Files/list ^Path (:source/root store))]
    (sort (mapv #(str (.getFileName ^Path %)) (iterator-seq (.iterator paths))))))

(defn launch! [& arguments]
  (.start (ProcessBuilder. ^java.util.List
                          (into [(.toString (.resolve (Path/of (System/getProperty "java.home")
                                                               (make-array String 0)) "bin/java"))
                                 "-cp" (System/getProperty "java.class.path")
                                 "clojure.main" "-m" "calliope.studio.session-process"] arguments))))

(defn await-process! [process]
  (when-not (.waitFor ^Process process 45 TimeUnit/SECONDS)
    (.destroyForcibly ^Process process)
    (throw (ex-info "Session process test timed out" {})))
  (let [out (slurp (.getInputStream ^Process process)) err (slurp (.getErrorStream ^Process process))]
    (is (zero? (.exitValue ^Process process)) (str out err))))

(deftest session-contracts-remain-named-closed-and-portable
  (is (= law/registry (edn/read-string (pr-str law/registry))))
  (is (= law/catalog (edn/read-string (pr-str law/catalog))))
  (doseq [type (keys law/registry)] (is (ifn? (law/validator type)) (str type)))
  (is (law/valid? :calliope.session/command-v1 (create-command)))
  (is (not (law/valid? :calliope.session/command-v1 (assoc (create-command) :actor actor))))
  (is (not (law/valid? :calliope.session/command-v1 (assoc (create-command) :session/title " ")))))

(deftest sources-history-and-sqlite-survive-an-independent-jvm
  (with-store
    (fn [parent root store]
      (let [cmd (setup-capture! store)
            admitted (session/ui-command! store actor cmd)
            take (get-in admitted [:result/value :take])
            _ (is (= :ok (:result/status admitted)) (pr-str admitted))
            _ (is (= :ok (:result/status (session/ui-command! store actor (command :studio/stop 3)))))
            history (session/history store)
            tables (into {} (map (fn [q] [q (session/query-rows store q)]) [:sessions :events :sources :takes]))
            output (.resolve parent "recovered.edn")]
        (is (= 6 (count (:take/notes take))))
        (is (not= (get-in take [:take/audio :render/id]) (get-in take [:take/midi :source/id])))
        (is (= 2 (count (source-files store))))
        (is (= :stopped (:capture/state (session/view store))))
        (session/close! store)
        (Files/delete ^Path (:index/path store))
        (let [process (launch! "recover" (str root) (:actor/id actor) (str output))]
          (await-process! process))
        (let [recovered (edn/read-string (slurp (str output)))]
          (is (= history (:history recovered)))
          (is (= (:session (session/view store)) (get-in recovered [:view :session])))
          (is (= :unarmed (get-in recovered [:view :capture/state])))
          (is (= :capture/not-armed (get-in recovered [:restart-ingest :error/code])))
          (is (= tables (:tables recovered))))))))

(deftest successful-retries-are-one-command-and-changed-content-conflicts
  (with-store
    (fn [_ _ store]
      (let [cmd (setup-capture! store)
            first-result (session/ui-command! store actor cmd)
            before (session/history store) opens (atom 0)]
        (with-redefs-fn {#'ingress/snapshot-sources! (fn [& _] (swap! opens inc) (throw (Exception. "No retry payload")))}
          (fn []
            (is (= first-result (session/ui-command! store actor cmd)))
            (is (= :request/conflict
                   (:error/code (session/ui-command! store actor (assoc-in cmd [:audio :sha256] (apply str (repeat 64 "f")))))))
            (is (zero? @opens))))
        (is (= before (session/history store)))
        (is (= 3 (count (:rows (session/query-rows store :events)))))))))

(deftest authorization-and-arm-refusals-have-no-payload-or-durable-effect
  (with-store
    (fn [_ _ store]
      (let [cmd (setup-capture! store) before (session/history store) opens (atom 0)]
        (with-redefs-fn {#'ingress/snapshot-sources! (fn [& _] (swap! opens inc) (throw (Exception. "No unauthorized payload")))}
          (fn []
            (doseq [context [(assoc actor :actor/kind :agent) (assoc actor :actor/kind :system)
                             (assoc actor :actor/id "other-user")]]
              (is (= :command/not-authorized (:error/code (session/ui-command! store context cmd)))))
            (is (= :command/not-authorized (:error/code (session/provider-command! store actor cmd))))
            (is (= :command/not-authorized (:error/code (session/system-command! store actor cmd))))
            (is (= :command/invalid (:error/code (session/ui-command! store actor (assoc cmd :entrypoint :ui-user)))))
            (is (= :session/conflict (:error/code (session/ui-command! store actor (assoc cmd :session/revision 1)))))
            (is (= before (session/history store)))
            (is (= :ok (:result/status (session/ui-command! store actor (command :studio/stop 2)))))
            (is (= :capture/not-armed
                   (:error/code (session/ui-command! store actor (assoc cmd :session/revision 3)))))
            (is (zero? @opens))))
        (is (empty? (source-files store)))))))

(deftest failure-before-persistence-preserves-inventory-history-and-projection
  (with-store
    (fn [_ _ store]
      (let [cmd (setup-capture! store) before (session/history store)
            ledger-before (Files/readAllBytes ^Path (:ledger/path store))
            table-before (session/query-rows store :events)]
        (with-redefs [clio-fs/require-directory-sync-support!
                      (fn [_] (throw (java.io.IOException. "Injected pre-write durability refusal")))]
          (is (= :error (:result/status (session/ui-command! store actor cmd)))))
        (is (= before (session/history store)))
        (is (java.util.Arrays/equals ledger-before (Files/readAllBytes ^Path (:ledger/path store))))
        (is (= table-before (session/query-rows store :events)))
        (is (empty? (source-files store)))))))

(deftest post-write-uncertainty-keeps-the-exact-candidate-and-retries-once
  (with-store
    (fn [_ root store]
      (let [cmd (setup-capture! store) before (session/view store)
            sync clio-fs/sync-directory! failed? (atom false)
            result (with-redefs [clio-fs/sync-directory!
                                (fn [path]
                                  (if (and (= (str root) (str path)) (compare-and-set! failed? false true))
                                    (throw (java.io.IOException. "Injected directory force failure after ledger write"))
                                    (sync path)))]
                     (session/ui-command! store actor cmd))
            candidate (:append/recovery result)]
        (is (= :uncertain (:result/status result)) (pr-str result))
        (is (law/valid? :calliope.session/result-v1 result))
        (is (= (:session before) (:session (session/view store))))
        (is (:recovery/pending? (session/view store)))
        (is (= 2 (count (session/history store))))
        (is (.contains (slurp (str (:ledger/path store))) (get-in candidate [:event :event/id])))
        (let [retried (session/retry! store actor candidate)]
          (is (= :ok (:result/status retried)) (pr-str retried))
          (is (= (get-in candidate [:event :event/id]) (:event/id retried)))
          (is (= retried (session/retry! store actor candidate)))
          (is (= 3 (count (session/history store))))
          (is (= 3 (count (:rows (session/query-rows store :events)))))
          (is (false? (:recovery/pending? (session/view store)))))))))

(deftest broken-sources-ledger-and-schemas-never-recover-as-empty-success
  (doseq [fault [:missing-source :changed-source :partial-ledger :missing-schema :missing-ledger]]
    (with-store
      (fn [_ root store]
        (let [cmd (setup-capture! store)]
          (is (= :ok (:result/status (session/ui-command! store actor cmd))))
          (session/close! store)
          (case fault
            :missing-source (Files/delete (.resolve ^Path (:source/root store) ^String (:sha256 (:audio cmd))))
            :changed-source (Files/write (.resolve ^Path (:source/root store) ^String (:sha256 (:audio cmd)))
                                         (byte-array [1 2 3]) (make-array java.nio.file.OpenOption 0))
            :partial-ledger (spit (str (:ledger/path store)) "{:broken" :append true)
            :missing-schema (fixtures/delete-tree! (.resolve root "schemas"))
            :missing-ledger (Files/delete ^Path (:ledger/path store)))
          (is (thrown? clojure.lang.ExceptionInfo (session/open! root actor)) (str fault)))))))

(deftest two-independent-writers-admit-one-session-revision
  (with-store
    (fn [parent root store]
      (is (= :ok (:result/status (session/ui-command! store actor (create-command)))))
      (let [barrier (.resolve parent "go")
            outputs (mapv #(.resolve parent (str "writer-" % ".edn")) [1 2])
            commands (mapv #(.resolve parent (str "command-" % ".edn")) [1 2])]
        (doseq [file commands] (spit (str file) (pr-str (arm-command 1))))
        (let [processes (mapv (fn [cmd out] (launch! "compete" (str root) (:actor/id actor)
                                                   (str out) (str cmd) (str barrier))) commands outputs)]
          (let [ready (mapv #(Path/of (str % ".ready") (make-array String 0)) outputs)
                deadline (+ (System/nanoTime) 30000000000)]
            (while (not-every? #(Files/exists ^Path % (make-array LinkOption 0)) ready)
              (when (> (System/nanoTime) deadline)
                (doseq [process processes] (.destroyForcibly ^Process process))
                (throw (ex-info "Independent writers did not both open the initial revision" {})))
              (Thread/sleep 10)))
          (Files/createFile barrier (make-array java.nio.file.attribute.FileAttribute 0))
          (doseq [process processes] (await-process! process)))
        (let [results (mapv #(edn/read-string (slurp (str %))) outputs)]
          (is (= [1 1] (mapv :opened-revision results)))
          (is (= {:ok 1 :error 1} (frequencies (map #(get-in % [:command-result :result/status]) results))))
          (is (= :session/conflict (some #(get-in % [:command-result :error/code]) results)))
          (is (= (:history (first results)) (:history (second results))))
          (is (= 2 (count (:history (first results))))))))))
