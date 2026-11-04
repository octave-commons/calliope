(ns calliope.companion.desktop
  "Native metadata and listening-notes companion. Suno owns transport and editing."
  (:require [calliope.companion.store :as store]
            [calliope.companion.review :as review]
            [calliope.suno.metadata :as metadata]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str])
  (:import [java.awt BorderLayout Color Desktop Dimension Font GridLayout GraphicsEnvironment]
           [java.awt.event ActionListener WindowAdapter]
           [java.net URI]
           [javax.swing BorderFactory BoxLayout JButton JComboBox DefaultListCellRenderer JFileChooser JFrame JLabel JOptionPane JPanel JScrollPane JSplitPane JTabbedPane JTable JTextArea JTextField JTree KeyStroke SwingUtilities Timer UIManager WindowConstants]
           [javax.swing.event DocumentListener ListSelectionListener TreeSelectionListener]
           [javax.swing.table DefaultTableCellRenderer DefaultTableModel]
           [javax.swing.tree DefaultMutableTreeNode DefaultTreeCellRenderer DefaultTreeModel TreePath]))

(def palette (edn/read-string (slurp (io/resource "companion/monokai.edn"))))
(defn color [key] (Color/decode (get palette key)))
(defn theme! []
  (UIManager/setLookAndFeel "javax.swing.plaf.nimbus.NimbusLookAndFeel")
  (doseq [[key value] {"control" :background "info" :surface "nimbusBase" :surface
                     "nimbusBlueGrey" :input "nimbusLightBackground" :input
                     "text" :foreground "nimbusSelectionBackground" :selection
                     "nimbusFocus" :info}]
    (UIManager/put key (color value)))
  (UIManager/put "defaultFont" (Font. "SansSerif" Font/PLAIN 14))
  (doseq [prefix ["Panel" "Viewport" "ScrollPane" "TabbedPane" "Label" "CheckBox" "ComboBox" "Table" "TableHeader" "Tree"]]
    (UIManager/put (str prefix ".background") (color :background))
    (UIManager/put (str prefix ".foreground") (color :foreground)))
  (doseq [prefix ["TextField" "TextArea" "ComboBox" "Button"]]
    (UIManager/put (str prefix ".background") (color :input))
    (UIManager/put (str prefix ".foreground") (color :foreground))
    (UIManager/put (str prefix ".caretForeground") (color :info)))
  (UIManager/put "Table.selectionBackground" (color :selection))
  (UIManager/put "Table.selectionForeground" (color :info)))

(defn area [editable?]
  (doto (JTextArea.) (.setEditable editable?) (.setLineWrap true) (.setWrapStyleWord true)
    (.setMargin (java.awt.Insets. 12 12 12 12)) (.setFont (Font. "SansSerif" Font/PLAIN 14))))
(defn button [text f]
  (doto (JButton. text) (.addActionListener (reify ActionListener (actionPerformed [_ _] (f))))))
(defn field-row [label component]
  (doto (JPanel. (BorderLayout. 10 4))
    (.add (JLabel. label) BorderLayout/WEST) (.add component BorderLayout/CENTER)
    (.setMaximumSize (Dimension. Integer/MAX_VALUE 38))))
(defn text-change! [component f]
  (.addDocumentListener (.getDocument component)
                        (reify DocumentListener
                          (insertUpdate [_ _] (f)) (removeUpdate [_ _] (f)) (changedUpdate [_ _] (f)))))
(defn pretty [data] (with-out-str (pprint/pprint data)))
(defn field-name [field]
  (get {:disposition "decision" :projects "projects / lists" :marker-input "marker text" :span "marker"}
       field (str/replace (name field) "-" " ")))
(defn review-description [record]
  (str "Decision: " (name (:disposition record))
       "\nLabels: " (if (seq (:labels record)) (str/join ", " (:labels record)) "None")
       "\nProjects / lists: " (if (seq (:projects record)) (str/join ", " (:projects record)) "None")
       "\n\nNote\n" (if (str/blank? (:note record)) "No note." (:note record))
       "\n\nRatings\n"
       (str/join "\n" (for [dimension (sort review/rating-dimensions)]
                         (str (field-name dimension) ": " (get-in record [:ratings dimension] "Unrated"))))
       "\n\nMarker: "
       (if-let [span (:span record)]
         (str (:start span) " s" (when-let [end (:end span)] (str " → " end " s")))
         (if-let [input (:marker-input record)]
           (str "Incomplete · " (:start input) " → " (:end input)) "None"))))

(defn table-renderer []
  (proxy [DefaultTableCellRenderer] []
    (getTableCellRendererComponent [table value selected? focused? row column]
      (let [label (proxy-super getTableCellRendererComponent table value selected? focused? row column)]
        (.setText label (if (nil? value) "—" (str value)))
        (.setOpaque label true)
        (.setForeground label (color (if selected? :info :foreground)))
        (.setBackground label (color (if selected? :selection :background)))
        (.setBorder label (if focused? (BorderFactory/createLineBorder (color :info))
                            (BorderFactory/createEmptyBorder 1 6 1 6)))
        label))))

(defn combo-renderer []
  (proxy [DefaultListCellRenderer] []
    (getListCellRendererComponent [list value index selected? focused?]
      (let [label (proxy-super getListCellRendererComponent list value index selected? focused?)]
        (.setText label (if (nil? value) "—" (str value)))
        (.setOpaque label true)
        (.setForeground label (color (if selected? :info :foreground)))
        (.setBackground label (color (if selected? :selection :input))) label))))

(defn marker-record
  "Preserve partial marker typing; only a complete, valid range becomes a span."
  [record start end duration]
  (let [record (cond-> record
                 (or (not (str/blank? start)) (not (str/blank? end)))
                 (assoc :marker-input {:start start :end end}))]
    (try
      (let [a (review/timestamp start) b (review/timestamp end)]
        (when (and b (nil? a)) (throw (ex-info "A range end requires a start" {})))
        (review/validate (cond-> record a (assoc :span {:start a :end b})) duration))
      (catch clojure.lang.ExceptionInfo _ (review/validate (dissoc record :span) duration)))))

(defn make-panel
  "Construct on the Swing thread. Writes run serially; navigation and ordinary editing stay live."
  [root initial-clips initial-reviews]
  (let [state (atom {:clips initial-clips :reviews (merge review/empty-history initial-reviews)
                    :rows [] :selected nil :loading? false :dirty? false :busy? false
                    :drafts {} :queue clojure.lang.PersistentQueue/EMPTY :history-locks #{}})
        search (JTextField.)
        filter-box (JComboBox. (into-array String ["All renders" "Unreviewed" "Keep" "Salvage" "Reject"]))
        model (proxy [DefaultTableModel] [(into-array String ["Title / render" "Model" "Weirdness" "Style influence" "Enjoyment" "Decision" "Labels"]) 0]
                (isCellEditable [_ _] false))
        table (doto (JTable. model) (.setRowHeight 30) (.setSelectionMode 0)
                (.setAutoCreateRowSorter true) (.setDefaultRenderer Object (table-renderer)))
        heading (JLabel. "Choose a render")
        lyrics (area false) styles (area false) settings (area false)
        history-tree (JTree.) history-preview (area false) tree-ids (atom {})
        note (area true) labels (JTextField.) projects (JTextField.) start (JTextField.) end (JTextField.)
        disposition (JComboBox. (into-array String ["unreviewed" "keep" "salvage" "reject"]))
        ratings (into (sorted-map) (for [dimension review/rating-dimensions]
                                    [dimension (JComboBox. (into-array String ["—" "0" "1" "2" "3" "4" "5"]))]))
        status (JLabel. "Search titles, lyrics, styles, labels, projects, or paste a Suno URL")
        panel (JPanel. (BorderLayout. 12 12)) actions (atom {})
        editor-controls (atom []) toolbar-controls (atom [])
        undo-button (button "Undo · Ctrl+Z" #((:undo @actions)))
        redo-button (button "Redo · Ctrl+Shift+Z" #((:redo @actions)))
        restore-button (button "Restore selected revision" #((:checkout @actions)))
        timer (Timer. 600 (reify ActionListener (actionPerformed [_ _] ((:flush @actions)))))]
    (.setRepeats timer false)
    (.setBorder panel (BorderFactory/createEmptyBorder 16 16 16 16))
    (.setFont heading (Font. "SansSerif" Font/BOLD 19))
    (doseq [[index width] (map-indexed vector [300 90 75 85 65 80 180])]
      (.setPreferredWidth (.getColumn (.getColumnModel table) index) width))
    (.setAutoResizeMode table JTable/AUTO_RESIZE_OFF)
    (.setForeground heading (color :info))
    (.setPreferredSize panel (Dimension. 1360 900))
    (doseq [combo (concat [filter-box disposition] (vals ratings))] (.setRenderer combo (combo-renderer)))
    (.setCellRenderer history-tree
                      (doto (DefaultTreeCellRenderer.)
                        (.setTextSelectionColor (color :info)) (.setTextNonSelectionColor (color :foreground))
                        (.setBackgroundSelectionColor (color :selection)) (.setBackgroundNonSelectionColor (color :background))))
    (letfn [(message! [text] (.setText status text))
            (current-record [id]
              (or (get-in @state [:drafts id :record]) (get-in @state [:reviews :latest id]) (review/empty-review id)))
            (dirty-state! [] (swap! state assoc :dirty? (boolean (seq (:drafts @state)))))
            (enabled! []
              (let [id (:selected @state) editable? (and id (not (:closing @state))
                                                        (not (contains? (:history-locks @state) id)))]
                (doseq [control @editor-controls] (.setEnabled control (boolean editable?)))
                (.setEnabled undo-button (boolean (and editable? (or (get-in @state [:drafts id])
                                                                                    (review/can-undo? (:reviews @state) id)))))
                (.setEnabled redo-button (boolean (and editable? (not (get-in @state [:drafts id]))
                                                      (review/redo-target (:reviews @state) id))))
                (.setEnabled restore-button (boolean (and editable? (.getSelectionPath history-tree))))
                (doseq [control @toolbar-controls] (.setEnabled control (not (boolean (:closing @state)))))))
            (status! []
              (when-not (:error @state)
                (message! (str (cond (:closing @state) "Saving pending edits before closing…"
                                (:dirty? @state) "Saving edits automatically… · changing songs keeps your thought"
                                (:busy? @state) "Working… · you can keep browsing"
                                :else (str (count (:rows @state)) " of " (count (:clips @state))
                                           " renders · saved locally · Suno plays the audio"))
                               (let [r (current-record (:selected @state))]
                                 (when (and (:marker-input r) (nil? (:span r)))
                                   " · marker incomplete; its text is retained"))))))
            (tree! []
              (let [id (:selected @state) history (:reviews @state) head (get-in history [:heads id])
                    baseline (DefaultMutableTreeNode. (str (when-not head "● ") "Before judgments"))
                    ids (atom {baseline nil}) chosen (atom (when-not head baseline))]
                (letfn [(add! [parent revision]
                          (let [node (get-in history [:nodes revision])
                                summary (str/join ", " (map field-name (sort (keys (or (get-in node [:event :review/patch])
                                                                                 {:note true :ratings true})))))
                                label (str (when (= revision head) "● ") (:ts node) " · " summary " · " (subs revision 0 (min 8 (count revision))))
                                item (DefaultMutableTreeNode. label)]
                            (swap! ids assoc item revision) (.add parent item)
                            (when (= revision head) (reset! chosen item))
                            (doseq [child (:children node)] (add! item child))))]
                  (doseq [revision (get-in history [:roots id])] (add! baseline revision)))
                (reset! tree-ids @ids) (.setModel history-tree (DefaultTreeModel. baseline))
                (loop [row 0]
                  (when (< row (.getRowCount history-tree)) (.expandRow history-tree row) (recur (inc row))))
                (when @chosen (.setSelectionPath history-tree (TreePath. (.getPath ^DefaultMutableTreeNode @chosen))))
                (enabled!)))
            (form-record []
              (let [id (:selected @state)]
                (marker-record {:clip/id id :note (.getText note)
                                :labels (review/terms (.getText labels)) :projects (review/terms (.getText projects))
                                :disposition (keyword (.getSelectedItem disposition))
                                :ratings (into {} (for [[dimension combo] ratings :when (pos? (.getSelectedIndex combo))]
                                                    [dimension (dec (.getSelectedIndex combo))]))}
                               (.getText start) (.getText end) (get-in @state [:clips id :clip/duration]))))
            (dirty! []
              (when (and (:selected @state) (not (:loading? @state)))
                (let [id (:selected @state) record (form-record) draft (get-in @state [:drafts id])]
                  (when (not= record (current-record id))
                    (swap! state assoc-in [:drafts id]
                           {:record record :version (inc (or (:version draft) 0))
                            :saved-version (or (:saved-version draft) 0) :enqueued-version (or (:enqueued-version draft) 0)})
                    (swap! state dissoc :error)
                    (dirty-state!) (enabled!) (status!) (.restart timer)))))
            (update-row! [id]
              (when-let [index (first (keep-indexed #(when (= id (:clip/id %2)) %1) (:rows @state)))]
                (let [r (current-record id)]
                  (.setValueAt model (or (get-in r [:ratings :enjoyment]) "—") index 4)
                  (.setValueAt model (name (:disposition r)) index 5)
                  (.setValueAt model (str/join ", " (:labels r)) index 6))))
            (show! [id]
              (when-let [clip (get-in @state [:clips id])]
                (let [latest (current-record id) marker (:marker-input latest) span (:span latest)]
                  (swap! state assoc :selected id :loading? true)
                  (.setText heading (str (:clip/title clip) " · " (subs id 0 8)))
                  (.setText lyrics (or (:clip/lyrics clip) "Lyrics were not supplied by this metadata source."))
                  (.setText styles (str "Styles\n\n" (or (:clip/styles clip) "Unknown")
                                       "\n\nExcluded styles\n\n" (or (:clip/negative-styles clip) "Unknown")))
                  (.setText settings (str "Generation settings (recorded intent)\n\n" (pretty (:clip/controls clip))
                                         "\nSource observation\n" (pretty (:clip/source clip))
                                         "\nAll preserved renderer fields\n\n" (pretty (:clip/raw clip))))
                  (doseq [component [lyrics styles settings]] (.setCaretPosition component 0))
                  (.setText note (:note latest)) (.setText labels (str/join ", " (:labels latest)))
                  (.setText projects (str/join ", " (:projects latest)))
                  (.setText start (or (:start marker) (some-> (:start span) str) ""))
                  (.setText end (or (:end marker) (some-> (:end span) str) ""))
                  (.setSelectedItem disposition (name (:disposition latest)))
                  (doseq [[dimension combo] ratings]
                    (.setSelectedIndex combo (if-let [score (get-in latest [:ratings dimension])] (inc score) 0)))
                  (swap! state assoc :loading? false) (tree!) (enabled!) (status!))))
            (queue-failed! [e]
              (swap! state #(-> % (assoc :queue clojure.lang.PersistentQueue/EMPTY :busy? false :history-locks #{})
                                (dissoc :closing)
                                (update :drafts (fn [drafts] (into {} (map (fn [[id draft]] [id (assoc draft :enqueued-version (:saved-version draft))]) drafts))))
                                (assoc :error (.getMessage e))))
              (.stop timer) (dirty-state!) (enabled!)
              (message! (str "Could not save: " (.getMessage e) " · drafts kept in this window; Reload or Save now to retry")))
            (pump! []
              (when-not (:busy? @state)
                (if-let [{:keys [task done]} (peek (:queue @state))]
                  (do (swap! state #(-> % (assoc :busy? true) (update :queue pop)))
                      (future
                        (let [result (try {:value (task)} (catch Exception e {:error e}))]
                          (SwingUtilities/invokeLater
                           (fn []
                             (if-let [e (:error result)] (queue-failed! e)
                               (try
                                 (done (:value result))
                                 (swap! state assoc :busy? false) (enabled!) (status!) (pump!)
                                 (catch Exception e (queue-failed! e)))))))))
                  (when (and (:closing @state) (not (:dirty? @state)))
                    (let [close (:closing @state)] (swap! state dissoc :closing) (close))))))
            (enqueue! [task done]
              (swap! state update :queue conj {:task task :done done}) (pump!))
            (flush-one! [id]
              (when-let [{:keys [record version enqueued-version]} (get-in @state [:drafts id])]
                (when (> version enqueued-version)
                  (swap! state assoc-in [:drafts id :enqueued-version] version)
                  (enqueue!
                   #(store/save-review! root record (get-in @state [:reviews :heads id]))
                   (fn [event]
                     (when event (swap! state update :reviews review/apply-event event))
                     (when-let [draft (get-in @state [:drafts id])]
                       (if (= version (:version draft)) (swap! state update :drafts dissoc id)
                         (swap! state assoc-in [:drafts id :saved-version] version)))
                     (dirty-state!) (update-row! id)
                     (when (= id (:selected @state)) (tree!)))))))
            (flush! []
              (.stop timer) (swap! state dissoc :error)
              (doseq [id (keys (:drafts @state))] (flush-one! id)) (status!))
            (navigate! [id]
              (when (not= id (:selected @state))
                (flush-one! (:selected @state)) (show! id)))
            (refresh! []
              (let [previous (:selected @state)
                    query (str/lower-case (str/trim (.getText search)))
                    query (or (some-> (re-find #"(?i)suno\.com/song/([0-9a-f-]{36})" query) second) query)
                    filter-value (nth [nil :unreviewed :keep :salvage :reject] (.getSelectedIndex filter-box))
                    rows (->> (vals (:clips @state))
                              (filter (fn [clip]
                                        (let [id (:clip/id clip) latest (current-record id)
                                              notes (map :note (for [[_ node] (get-in @state [:reviews :nodes])
                                                                    :when (= id (:clip/id node))] (:record node)))]
                                          (and (or (nil? filter-value) (= filter-value (:disposition latest)))
                                               (str/includes? (metadata/search-text clip (assoc latest :note (str (:note latest) " " (str/join " " notes)))) query)))))
                              (sort-by (juxt :clip/title :clip/id)) vec)
                    index (or (first (keep-indexed #(when (= previous (:clip/id %2)) %1) rows))
                              (when (seq rows) 0))]
                (swap! state assoc :loading? true :rows rows) (.setRowCount model 0)
                (doseq [clip rows :let [r (current-record (:clip/id clip))]]
                  (.addRow model (into-array Object [(str (:clip/title clip) " · " (subs (:clip/id clip) 0 8))
                                                     (or (:clip/model clip) "Unknown")
                                                     (or (get (:clip/controls clip) "weirdness_constraint") "—")
                                                     (or (get (:clip/controls clip) "style_weight") "—")
                                                     (or (get-in r [:ratings :enjoyment]) "—") (name (:disposition r))
                                                     (str/join ", " (:labels r))])))
                (when index (let [view (.convertRowIndexToView table index)] (.setRowSelectionInterval table view view)))
                (swap! state assoc :loading? false)
                (when index (navigate! (:clip/id (nth rows index))))
                (status!)))
            (history! [action target]
              (when-let [id (:selected @state)]
                (when-not (contains? (:history-locks @state) id)
                  (flush-one! id) (swap! state update :history-locks conj id) (enabled!)
                  (enqueue!
                   #(store/move-history! root id action target (get-in @state [:reviews :heads id]))
                   (fn [event]
                     (when event (swap! state update :reviews review/apply-event event))
                     (swap! state update :history-locks disj id)
                     (update-row! id) (when (= id (:selected @state)) (show! id)))))))
            (checkout! []
              (when-let [selected (.getLastSelectedPathComponent history-tree)]
                (when (contains? @tree-ids selected) (history! :checkout (get @tree-ids selected)))))
            (reload! []
              ;; Read current heads before retrying retained drafts, making conflict recovery explicit.
              (enqueue! #(vector (:clips (store/catalog root)) (store/reviews root))
                        (fn [[clips history]]
                          (swap! state assoc :clips clips :reviews history) (swap! state dissoc :error)
                          (flush!) (refresh!) (when (:selected @state) (tree!)))))
            (import! []
              (let [chooser (doto (JFileChooser.) (.setFileSelectionMode JFileChooser/FILES_AND_DIRECTORIES)
                              (.setDialogTitle "Import Suno metadata JSON / TXT, or a folder"))]
                (when (= JFileChooser/APPROVE_OPTION (.showOpenDialog chooser panel))
                  (flush!)
                  (let [file (.getSelectedFile chooser)]
                    (enqueue! #(let [result (store/import! root file)] [result (:clips (store/catalog root))])
                              (fn [[result clips]]
                                (swap! state assoc :clips clips) (refresh!)
                                (message! (str "Imported " (:appended result) " observations · " (count (:errors result)) " errors"))
                                (when (seq (:errors result))
                                  (JOptionPane/showMessageDialog panel (pretty (:errors result)) "Import errors" JOptionPane/WARNING_MESSAGE))))))))
            (close! [done]
              (flush!) (swap! state assoc :closing done) (enabled!) (status!) (pump!))
            (dispose! [] (.stop timer))]
      (reset! actions {:save flush! :flush flush! :refresh refresh! :show show! :navigate navigate!
                       :undo #(history! :undo nil) :redo #(history! :redo nil) :checkout checkout!
                       :checkout-revision #(history! :checkout %) :reload reload! :close close! :dispose dispose!})
      (let [toolbar (JPanel. (BorderLayout. 8 8)) filters (JPanel.) buttons (JPanel.)
            inspector (JPanel. (BorderLayout. 8 10)) tabs (JTabbedPane.)
            editor (JPanel.) span-panel (JPanel. (GridLayout. 1 2 10 0)) editor-actions (JPanel.)
            ratings-panel (JPanel. (GridLayout. 2 3 6 4)) history-panel (JPanel. (BorderLayout. 6 6))
            history-split (JSplitPane. JSplitPane/VERTICAL_SPLIT (JScrollPane. history-tree) (JScrollPane. history-preview))
            library-scroll (doto (JScrollPane. table) (.setColumnHeaderView (.getTableHeader table)))
            split (JSplitPane. JSplitPane/HORIZONTAL_SPLIT library-scroll inspector)
            save-button (button "Save now · Ctrl+Enter" flush!)]
        (.setToolTipText search "Search the corpus, or paste a Suno song URL to find its exact render")
        (.add filters filter-box) (.add toolbar filters BorderLayout/WEST) (.add toolbar search BorderLayout/CENTER)
        (.add buttons (button "Import metadata…" import!)) (.add buttons (button "Reload" reload!))
        (.add toolbar buttons BorderLayout/EAST) (.add panel toolbar BorderLayout/NORTH)
        (.add inspector heading BorderLayout/NORTH)
        (doseq [[title component] [["Lyrics" lyrics] ["Styles" styles] ["Metadata" settings]]]
          (.addTab tabs title (JScrollPane. component)))
        (.setResizeWeight history-split 0.65) (.setDividerLocation history-split 220)
        (.add history-panel history-split BorderLayout/CENTER) (.add history-panel restore-button BorderLayout/SOUTH)
        (.addTab tabs "History tree" history-panel) (.add inspector tabs BorderLayout/CENTER)
        (.setLayout editor (BoxLayout. editor BoxLayout/Y_AXIS))
        (.add editor (field-row "Labels" labels)) (.add editor (field-row "Projects / lists" projects))
        (.add span-panel (field-row "From" start)) (.add span-panel (field-row "To" end))
        (.setToolTipText start "Seconds or m:ss; incomplete input is saved as text until it is a valid marker")
        (.setToolTipText end "Optional end, later than start") (.add editor span-panel)
        (.add editor (doto (JScrollPane. note) (.setPreferredSize (Dimension. 400 130))))
        (doseq [[dimension combo] ratings]
          (.add ratings-panel (field-row (str/replace (name dimension) "-" " ") combo)))
        (.add editor ratings-panel) (.add editor-actions disposition) (.add editor-actions save-button)
        (.add editor-actions (button "Open in Suno"
                                    #(when-let [id (:selected @state)]
                                       (try (.browse (Desktop/getDesktop) (URI. (str "https://suno.com/song/" id)))
                                            (catch Exception e (message! (.getMessage e)))))))
        (.add editor editor-actions)
        (let [history-actions (JPanel.)]
          (.add history-actions undo-button) (.add history-actions redo-button) (.add editor history-actions))
        (.add inspector editor BorderLayout/SOUTH)
        (reset! editor-controls (concat [note labels projects start end disposition save-button] (vals ratings)))
        (reset! toolbar-controls (concat [search filter-box table] (seq (.getComponents buttons))))
        (.setResizeWeight split 0.44) (.setDividerLocation split 550)
        (.add panel split BorderLayout/CENTER) (.add panel status BorderLayout/SOUTH))
      (.addListSelectionListener
       (.getSelectionModel table)
       (reify ListSelectionListener
         (valueChanged [_ event]
           (when (and (not (.getValueIsAdjusting event)) (not (:loading? @state)) (<= 0 (.getSelectedRow table)))
             (navigate! (:clip/id (nth (:rows @state) (.convertRowIndexToModel table (.getSelectedRow table)))))))))
      (.addTreeSelectionListener history-tree
                                 (reify TreeSelectionListener
                                   (valueChanged [_ _]
                                     (when-let [node (.getLastSelectedPathComponent history-tree)]
                                       (let [id (:selected @state) revision (get @tree-ids node)]
                                         (.setText history-preview
                                                   (str (if revision (str "Revision · " (get-in @state [:reviews :nodes revision :ts]) "\n\n")
                                                          "Before judgments\n\n")
                                                        (review-description (review/record-at (:reviews @state) id revision))))
                                         (.setCaretPosition history-preview 0) (enabled!))))))
      (doseq [field [note labels projects start end]] (text-change! field dirty!))
      (doseq [field (cons disposition (vals ratings))]
        (.addActionListener field (reify ActionListener (actionPerformed [_ _] (dirty!)))))
      (text-change! search #(do (flush!) (refresh!)))
      (.addActionListener filter-box (reify ActionListener (actionPerformed [_ _] (flush!) (refresh!))))
      (doseq [[key action] [["control ENTER" flush!] ["control Z" #((:undo @actions))]
                            ["control shift Z" #((:redo @actions))] ["control Y" #((:redo @actions))]
                            ["control L" #(.requestFocusInWindow search)]]]
        (.registerKeyboardAction panel (reify ActionListener (actionPerformed [_ _] (action)))
                                 (KeyStroke/getKeyStroke key) JPanel/WHEN_IN_FOCUSED_WINDOW))
      (refresh!) (enabled!)
      {:panel panel :state state :table table :search search :note note :labels labels :projects projects
       :start start :end end :disposition disposition :ratings ratings :filter filter-box
       :history-tree history-tree :tree-ids tree-ids :undo undo-button :redo redo-button :restore restore-button
       :status status :timer timer :actions @actions})))

(defn -main [& args]
  (when (seq args) (throw (ex-info "Usage: clojure -M:companion (from Calliope root)" {})))
  (when (GraphicsEnvironment/isHeadless)
    (throw (ex-info "A desktop display is required for the native companion" {})))
  (let [root (.getCanonicalPath (io/file ".")) clips (:clips (store/catalog root)) history (store/reviews root)]
    (SwingUtilities/invokeLater
     (fn []
       (theme!)
       (let [{:keys [panel actions]} (make-panel root clips history) frame (JFrame. "Calliope · Listening companion")]
         (.setDefaultCloseOperation frame WindowConstants/DO_NOTHING_ON_CLOSE)
         (.addWindowListener frame (proxy [WindowAdapter] []
                                     (windowClosing [_] ((:close actions) #(do ((:dispose actions)) (.dispose frame) (shutdown-agents))))))
         (.setContentPane frame panel) (.pack frame) (.setLocationByPlatform frame true) (.setVisible frame true))))))
