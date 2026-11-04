(ns calliope.suno.metadata-test
  (:require [calliope.suno.metadata :as metadata]
            [calliope.companion.review :as review]
            [calliope.companion.store :as store]
            [calliope.companion.desktop :as desktop]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]])
  (:import [java.nio.file Files]
           [javax.swing SwingUtilities]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]))

(def id "12345678-1234-1234-1234-123456789abc")
(def other-id "abcdefab-1234-1234-1234-123456789abc")
(def clip {"id" id "title" "Same title" "is_liked" false
           "model_name" "chirp-hawk"
           "metadata" {"prompt" "[Verse]\n川を見て" "tags" "glitch choir"
                       "duration" 120.5 "control_sliders" {"weirdness_constraint" 0.6
                                                           "style_weight" 0.74 "aug_creativity" 1}}})

(deftest renderer-fields-and-unknown-values
  (let [c (metadata/normalize clip)]
    (is (= "[Verse]\n川を見て" (:clip/lyrics c)))
    (is (= 0.74 (get (:clip/controls c) "style_weight")))
    (is (= 1 (get-in c [:clip/raw "metadata" "control_sliders" "aug_creativity"])))
    (is (false? (:clip/liked c)))
    (is (nil? (:clip/liked (metadata/normalize (dissoc clip "is_liked")))))
    (is (nil? (:clip/controls (metadata/normalize (assoc clip "metadata" {})))))
    (is (= 2 (metadata/source-variation "title (2).json"))))
  (doseq [bad [(dissoc clip "id") (assoc clip "id" "title-is-not-identity")
               (assoc clip "metadata" nil) (assoc clip "title" 42)
               (assoc-in clip ["metadata" "duration"] -1)
               (assoc-in clip ["metadata" "duration"] Double/NaN)
               (assoc-in clip ["metadata" "prompt"] ["bad"])
               (assoc-in clip ["metadata" "control_sliders"] "bad")]]
    (is (thrown? clojure.lang.ExceptionInfo (metadata/normalize bad)))))

(deftest privacy-and-payload-forms
  (let [raw (assoc clip "user_id" "personal-id" "avatar_image_url" "private-avatar"
                  "authorization" "private-auth"
                  "new_field" {"secret" "private-secret" "color" "green"})
        sanitized (:clip/raw (metadata/normalize raw))]
    (is (= {"color" "green"} (get sanitized "new_field")))
    (is (not-any? #(contains? sanitized %) ["user_id" "avatar_image_url" "authorization"])))
  (doseq [payload [clip [clip] {"clips" [clip]}]]
    (is (= [clip] (metadata/payload-clips payload))))
  (is (thrown? clojure.lang.ExceptionInfo (metadata/payload-clips {"songs" []})))
  (is (= "v5" (:clip/model (metadata/normalize
                            (-> clip (dissoc "model_name")
                                (assoc-in ["metadata" "model_badges" "songcard" "display_name"] "v5"))))))
  (let [old (metadata/normalize (metadata/parse-text (str "Title: Legacy\r\nID: " id "\r\nPrompt:\r\nLyrics")))]
    (is (= "Lyrics" (:clip/lyrics old))) (is (nil? (:clip/styles old))) (is (nil? (:clip/liked old)))))

(defn temp-dir [] (.toFile (Files/createTempDirectory "calliope-metadata-test" (make-array java.nio.file.attribute.FileAttribute 0))))
(defn clean! [root] (doseq [file (reverse (file-seq root))] (io/delete-file file true)))

(deftest actual-filesystem-ingest-and-repeatable-rebuild
  (let [root (temp-dir) source (io/file root "input") ledger (io/file root "ledgers/ingest.edn")]
    (try
      (.mkdirs source) (io/make-parents ledger)
      (spit ledger "{:event/type :existing/history :value 42}\n")
      (let [prefix (slurp ledger)]
        (spit (io/file source "batch.json") (json/write-str {"clips" [clip (assoc clip "id" other-id)]}))
        ;; Malformed input is visible; valid observations can still be admitted.
        (spit (io/file source "bad.json") "{bad")
        (spit (io/file source "not-a-suno-song.txt") "ordinary notes")
        (spit (io/file source "audio.mp3") "not parsed or copied")
        (let [result (store/import! root source)]
          (is (= 2 (:appended result))) (is (= 2 (:clips result)))
          (is (= 1 (:skipped result))) (is (= 1 (count (:errors result)))))
        (is (.startsWith (slurp ledger) prefix))
        (let [bytes-before (slurp ledger)]
          (is (zero? (:appended (store/import! root source))))
          (is (= bytes-before (slurp ledger))))
        ;; Different export wrapper/time must not re-observe unchanged clip data.
        (spit (io/file source "batch.json") (json/write-str {"exported_at" "later" "clips" [clip (assoc clip "id" other-id)]}))
        (is (zero? (:appended (store/import! root source))))
        (spit (io/file source "batch.json") (json/write-str (assoc-in clip ["metadata" "control_sliders" "style_weight"] 0.8)))
        (is (= 1 (:appended (store/import! root source))))
        (is (= 0.8 (get-in (store/catalog root) [:clips id :clip/controls "style_weight"])))
        ;; Sparse legacy TXT cannot erase the richer JSON observation.
        (spit (io/file source "legacy.txt") (str "Title: Legacy\nID: " id "\nPrompt:\nold"))
        (store/import! root source)
        (is (= "Same title" (get-in (store/catalog root) [:clips id :clip/title])))
        (store/project! root)
        (let [first-hash (store/sha256 (slurp (io/file root "ledgers/projections/suno-meta-v1.edn")))]
          (store/project! root)
          (is (= first-hash (store/sha256 (slurp (io/file root "ledgers/projections/suno-meta-v1.edn"))))))
        (is (not (.exists (io/file root "tracks/audio.mp3")))))
      (finally (clean! root)))))

(def record {:clip/id id :note "Good choir at the start" :labels ["motif/choir" "salvage/intro"]
             :projects ["world/river" "stems-shortlist"] :disposition :salvage
             :ratings {:enjoyment 4 :technical-quality 0} :span {:start 0 :end 12}})

(deftest repeated-judgments-are-distinct-and-scoped
  (is (= record (review/validate record 120.5)))
  (is (= 83.5 (review/timestamp "1:23.5")))
  (is (= 3723.0 (review/timestamp "1:02:03")))
  (doseq [timestamp ["-1" "1:60" "NaN" "1:2:3:4" "1:" "1:02:"]]
    (is (thrown? clojure.lang.ExceptionInfo (review/timestamp timestamp))))
  (doseq [bad [(assoc-in record [:span :end] 0) (assoc-in record [:span :end] 121)
               (assoc-in record [:span :start] Double/NaN)
               (assoc-in record [:ratings :enjoyment] 6)
               (assoc-in record [:ratings :made-up] 3)
               (assoc record :labels "bad")]]
    (is (thrown? clojure.lang.ExceptionInfo (review/validate bad 120.5))))
  (let [root (temp-dir) source (io/file root "clip.json")]
    (try
      (spit source (json/write-str clip)) (store/import! root source)
      (let [first-event (store/save-review! root record)
            next-record (assoc record :note "Still useful on another listen" :span {:start 42 :end 50})
            second-event (store/save-review! root next-record)
            state (store/reviews root)]
        (is (not= (:event/id first-event) (:event/id second-event)))
        (is (= 2 (count (get-in state [:history id]))))
        (is (= next-record (get-in state [:latest id])))
        (is (= record (get-in state [:nodes (:event/id first-event) :record])))
        (is (thrown? clojure.lang.ExceptionInfo (store/save-review! root (assoc record :clip/id other-id)))))
      (finally (clean! root)))))

(deftest headless-real-ui-search-and-render-selection
  (let [root (temp-dir) image-file (io/file root "panel.png") result (atom nil)
        clips {id (metadata/normalize clip) other-id (metadata/normalize (assoc clip "id" other-id))}]
    (try
      (SwingUtilities/invokeAndWait
       (bound-fn []
         (desktop/theme!)
         (let [{:keys [panel state table search note] :as ui} (desktop/make-panel root clips {:latest {} :history {}})]
           (is (= 2 (.getRowCount table)))
           (.setText search (str "https://suno.com/song/" other-id))
           (is (= 1 (.getRowCount table)))
           (.setRowSelectionInterval table 0 0)
           (is (= other-id (:selected @state)))
           (.setText note "A new thought")
           (is (:dirty? @state))
           ;; Construct and render actual Swing components; no audio/mock player.
           (.setSize panel 1280 850)
           (letfn [(layout! [component]
                     (when (instance? java.awt.Container component)
                       (.doLayout ^java.awt.Container component)
                       (doseq [child (.getComponents ^java.awt.Container component)] (layout! child))))]
             (layout! panel))
           (let [image (BufferedImage. 1280 850 BufferedImage/TYPE_INT_RGB)
                 graphics (.createGraphics image)]
             (.printAll panel graphics) (.dispose graphics)
             (ImageIO/write image "png" image-file))
           (reset! result ui))))
      (is (> (.length image-file) 1000))
      (is (some? @result))
      (finally
        (when @result (SwingUtilities/invokeAndWait #((get-in @result [:actions :dispose]))))
        (clean! root)))))

(deftest ui-save-runs-the-real-ledger-flow
  (let [root (temp-dir) source (io/file root "clip.json") ui (atom nil) completion (promise)]
    (try
      (spit source (json/write-str clip)) (store/import! root source)
      (SwingUtilities/invokeAndWait
       (bound-fn []
         (desktop/theme!)
         (let [{:keys [state note labels projects start end actions] :as panel}
               (desktop/make-panel root (:clips (store/catalog root)) (store/reviews root))]
           (reset! ui panel)
           (.setText note "A phrase for the river motif")
           (.setText labels "motif/river, salvage/intro")
           (.setText projects "world/river, stems-shortlist")
           (.setText start "0:04") (.setText end "0:12")
           (add-watch state :finished
                      (fn [_ _ old next]
                        (when (and (:busy? old) (not (:busy? next)))
                          ;; Queue after the completion callback updates the read model.
                          (SwingUtilities/invokeLater #(deliver completion true)))))
           ((:save actions))
           (is (:busy? @state))
           (is (.isEnabled note)))))
      (is (= true (deref completion 10000 :timeout)))
      (let [record (get-in (store/reviews root) [:latest id])]
        (is (= "A phrase for the river motif" (:note record)))
        (is (= ["world/river" "stems-shortlist"] (:projects record)))
        (is (= {:start 4.0 :end 12.0} (:span record))))
      (SwingUtilities/invokeAndWait
       (bound-fn [] (is (not (:dirty? @(:state @ui))))
         (is (.isEnabled (:note @ui)))
         (is (= "0:04" (.getText (:start @ui))))))
      (finally
        (when @ui (SwingUtilities/invokeAndWait #((get-in @ui [:actions :dispose]))))
        (clean! root)))))
