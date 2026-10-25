(ns calliope.studio.session-index
  "GPL-3.0-or-later. Disposable SQLite projection over admitted session history."
  (:require [calliope.studio.session-domain :as domain]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import [java.nio.file Files Path CopyOption StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def queries
  {:sessions {:columns [:session/id :work/id :session/title :session/revision]
              :sql "SELECT session_id, work_id, title, revision FROM sessions ORDER BY session_id"
              :keys [:session_id :work_id :title :revision]}
   :events {:columns [:event/seq :event/id :request/id :command/type]
            :sql "SELECT seq_num, event_id, request_id, command_type FROM events ORDER BY seq_num"
            :keys [:seq_num :event_id :request_id :command_type]}
   :sources {:columns [:source/id :source/kind :source/sha256]
             :sql "SELECT source_id, kind, sha256 FROM sources ORDER BY source_id"
             :keys [:source_id :kind :sha256]}
   :takes {:columns [:take/id :render/id :source/id :audio/sha256 :midi/sha256]
           :sql "SELECT take_id, render_id, midi_id, audio_sha, midi_sha FROM takes ORDER BY take_id"
           :keys [:take_id :render_id :midi_id :audio_sha :midi_sha]}})

(defn- datasource [file]
  (jdbc/get-datasource {:dbtype "sqlite" :dbname (str file)}))

(defn rebuild!
  "Build beside the old index and replace it only after its transaction closes.
   This is a cache replacement, not source/event storage or a durability claim."
  [^Path file state]
  (let [temporary (Files/createTempFile (.getParent file) ".index-" ".sqlite" (make-array FileAttribute 0))]
    (try
      (jdbc/with-transaction [connection (datasource temporary)]
        (doseq [sql ["CREATE TABLE sessions (session_id TEXT PRIMARY KEY, work_id TEXT, title TEXT, revision INTEGER)"
                     "CREATE TABLE events (seq_num INTEGER PRIMARY KEY, event_id TEXT UNIQUE, request_id TEXT UNIQUE, command_type TEXT)"
                     "CREATE TABLE sources (source_id TEXT PRIMARY KEY, kind TEXT, sha256 TEXT)"
                     "CREATE TABLE takes (take_id TEXT PRIMARY KEY, render_id TEXT, midi_id TEXT, audio_sha TEXT, midi_sha TEXT)"]]
          (jdbc/execute! connection [sql]))
        (when (:session/id state)
          (jdbc/execute! connection ["INSERT INTO sessions VALUES (?, ?, ?, ?)"
                                     (:session/id state) (:work/id state) (:session/title state) (:session/revision state)]))
        (doseq [event (:events state)
                :let [command (get-in event [:event/data :record/command])]]
          (jdbc/execute! connection ["INSERT INTO events VALUES (?, ?, ?, ?)"
                                     (:event/seq event) (:event/id event) (:request/id command) (str (:command/type command))]))
        (doseq [[id source] (domain/sources state)]
          (jdbc/execute! connection ["INSERT INTO sources VALUES (?, ?, ?)"
                                     id (name (:source/kind source)) (:source/sha256 source)]))
        (doseq [[id take] (:takes state)]
          (jdbc/execute! connection ["INSERT INTO takes VALUES (?, ?, ?, ?, ?)" id
                                     (get-in take [:take/audio :render/id]) (get-in take [:take/midi :source/id])
                                     (get-in take [:take/audio :sha256]) (get-in take [:take/midi :sha256])])) )
      (Files/move temporary file (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
      state
      (finally (Files/deleteIfExists temporary)))))

(defn query-rows [file query]
  (let [{:keys [columns sql keys]} (or (get queries query)
                                     (throw (ex-info "Unknown session projection query" {:error/code :query/invalid})))
        rows (jdbc/execute! (datasource file) [sql] {:builder-fn rs/as-unqualified-lower-maps})]
    {:columns columns :rows (mapv (fn [row] (mapv row keys)) rows)}))
