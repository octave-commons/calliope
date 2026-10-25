(ns calliope.studio.midi
  "Pure bounded MIDI interpretation. Raw packet order is never rewritten.
   Physical key and sounding spans are separate derived observations."
  (:require [calliope.law.capture :as law]))

(defn- refuse! [code message]
  (throw (ex-info message {:error/code code})))

(defn- nat? [value]
  (law/valid? :calliope.capture/nat-v1 value))

(defn- product [values]
  #?(:clj (reduce *' 1 values)
     :cljs (reduce (fn [a b]
                     (let [n (* a b)]
                       (when-not (js/Number.isSafeInteger n)
                         (refuse! :capture/clock-overflow "Clock product exceeds exact host arithmetic"))
                       n)) 1 values)))

(defn- floor-ratio [numerator denominator]
  #?(:clj (quot numerator denominator)
     :cljs (js/Math.floor (/ numerator denominator))))

(defn- bounded-sample [sample]
  (when-not (nat? sample)
    (refuse! :capture/clock-overflow "Sample is outside the supported exact integer range"))
  #?(:clj (long sample) :cljs sample))

(defn tick->sample [clock tick]
  (when-not (and (law/valid? :calliope.capture/clock-v1 clock) (nat? tick))
    (refuse! :capture/invalid-clock "Invalid declared constant-tempo clock or raw tick"))
  (bounded-sample
   (+ (:sample-origin clock)
      (floor-ratio (product [tick (:sample-rate clock) (:tempo-us-per-quarter clock)])
                   (product [(:ppq clock) 1000000])))))

(defn sample-range->ms [sample-rate start end]
  (when-not (and (integer? sample-rate) (<= 8000 sample-rate 192000)
                 (nat? start) (nat? end) (< start end))
    (refuse! :capture/invalid-range "Expected a nonempty bounded sample range"))
  (let [scaled-start (product [1000 start])
        scaled-end (product [1000 end])]
    {:start-ms (bounded-sample (floor-ratio scaled-start sample-rate))
     :end-ms (bounded-sample (+ (floor-ratio scaled-end sample-rate)
                               (if (zero? (rem scaled-end sample-rate)) 0 1)))}))

(defn- at! [state sample end]
  (when-not (and (nat? sample) (<= (:last-sample state) sample end))
    (refuse! :capture/backward-or-outside-time "Time moved backward or outside the audio source"))
  sample)

(defn- interrupt [state sample reason]
  (let [indices (keep-indexed (fn [i note] (when (nil? (:sound/end-sample note)) i))
                              (:notes state))
        observation {:reason reason :sample/offset sample :device/epoch (:epoch state)
                     :voice/ids (mapv #(get-in state [:notes % :voice/id]) indices)
                     :pedal/channels (vec (sort (:pedals state)))}]
    (-> (reduce (fn [s i]
                  (update-in s [:notes i]
                             #(assoc % :key/end-sample (or (:key/end-sample %) sample)
                                       :sound/end-sample sample :interrupted? true)))
                state indices)
        (cond-> (or (seq indices) (seq (:pedals state)))
          (update :interruptions conj observation))
        (assoc :active {} :sustained #{} :pedals #{}))))

(defn- note-on [state packet sample]
  (let [{:keys [channel pitch]} (:midi packet)
        i (count (:notes state))]
    (-> state
        (update :notes conj {:voice/id [(:device/epoch packet) (:packet/seq packet)]
                             :device/id (:device/id packet) :channel channel :pitch pitch
                             :key/start-sample sample :key/end-sample nil
                             :sound/end-sample nil :interrupted? false})
        (update-in [:active [channel pitch]] (fnil conj []) i))))

(defn- note-off [state packet sample]
  (let [{:keys [channel pitch]} (:midi packet)
        queue (get-in state [:active [channel pitch]])
        i (first queue)]
    (when (nil? i) (refuse! :capture/orphan-note-off "Note-off has no physical onset"))
    (-> state
        (assoc-in [:active [channel pitch]] (subvec queue 1))
        (assoc-in [:notes i :key/end-sample] sample)
        (cond-> (contains? (:pedals state) channel)
          (update :sustained conj i)
          (not (contains? (:pedals state) channel))
          (assoc-in [:notes i :sound/end-sample] sample)))))

(defn- controller [state packet sample]
  (let [{:keys [channel controller value]} (:midi packet)]
    (cond
      (not= controller 64) state
      (>= value 64) (update state :pedals conj channel)
      :else
      (let [released (filter #(= channel (get-in state [:notes % :channel])) (:sustained state))]
        (-> (reduce #(assoc-in %1 [:notes %2 :sound/end-sample] sample) state released)
            (update :sustained #(apply disj % released))
            (update :pedals disj channel))))))

(defn- midi-packet [state packet end]
  (when-not (and (:connected? state) (= (:epoch state) (:device/epoch packet)))
    (refuse! :capture/device-disconnected "Packet requires the declared connected epoch"))
  (when-not (and (> (:packet/seq packet) (:last-seq state))
                 (>= (:raw/tick packet) (:last-tick state)))
    (refuse! :capture/packet-order "Duplicate/backward packet sequence or raw tick"))
  (let [sample (at! state (tick->sample (:clock state) (:raw/tick packet)) end)
        {:keys [kind velocity]} (:midi packet)
        interpreted (case kind
                      :note-on (if (zero? velocity) (note-off state packet sample)
                                   (note-on state packet sample))
                      :note-off (note-off state packet sample)
                      :control-change (controller state packet sample))]
    (-> interpreted
        (assoc :last-seq (:packet/seq packet) :last-tick (:raw/tick packet) :last-sample sample)
        (update :events conj (assoc packet :sample/offset sample)))))

(defn- step [state packet end device-id]
  (when (:stopped? state)
    (refuse! :capture/after-stop "No packets are admitted after stop"))
  (when-not (law/valid? :calliope.capture/input-v1 packet)
    (refuse! :capture/invalid-packet "Malformed packet or lifecycle observation"))
  (when (and (contains? packet :device/id) (not= device-id (:device/id packet)))
    (refuse! :capture/undeclared-device "Input belongs to an undeclared device"))
  (case (:kind packet)
    :disconnect
    (do
      (when-not (and (:connected? state) (= (:epoch state) (:device/epoch packet)))
        (refuse! :capture/device-disconnected "Disconnect requires the connected epoch"))
      (let [sample (at! state (:sample/offset packet) end)]
        (-> (interrupt state sample :disconnect)
            (assoc :connected? false :last-sample sample)
            (update :events conj packet))))
    :reconnect
    (do
      (when-not (and (not (:connected? state)) (= (inc (:epoch state)) (:device/epoch packet)))
        (refuse! :capture/invalid-reconnect "Reconnect must start the next disconnected epoch"))
      (let [sample (at! state (:sample-origin packet) end)]
        (-> state
            (assoc :connected? true :epoch (:device/epoch packet)
                   :last-tick -1 :last-seq -1 :last-sample sample)
            (assoc-in [:clock :sample-origin] sample)
            (update :events conj (assoc packet :sample/offset sample)))))
    :stop
    (let [sample (at! state (:sample/offset packet) end)]
      (-> (interrupt state sample :stop)
          (assoc :stopped? true :last-sample sample)
          (update :events conj packet)))
    (midi-packet state packet end)))

(defn normalize
  "Interpret one declared device and bounded source atomically.
   Failure exposes no partially normalized events/notes. No effects or model calls."
  [clock device-id packets end-sample]
  (try
    (when-not (and (law/valid? :calliope.capture/clock-v1 clock)
                   (law/valid? :calliope.studio/non-blank-string device-id)
                   (vector? packets) (nat? end-sample)
                   (< (:sample-origin clock) end-sample))
      (refuse! :capture/invalid-take "Invalid clock, device, packet vector or audio bound"))
    (let [initial {:clock clock :epoch 0 :connected? true :stopped? false
                   :last-seq -1 :last-tick -1 :last-sample (:sample-origin clock)
                   :active {} :pedals #{} :sustained #{} :notes [] :events [] :interruptions []}
          state (reduce #(step %1 %2 end-sample device-id) initial packets)
          final (interrupt state end-sample :end-of-take)]
      {:ok? true :clock clock :device/id device-id :events (:events final)
       :notes (:notes final) :interruptions (:interruptions final)})
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs ExceptionInfo) e
      {:ok? false :error/code (:error/code (ex-data e))
       :error/message #?(:clj (.getMessage e) :cljs (ex-message e))})))
