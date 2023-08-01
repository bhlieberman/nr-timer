(ns bhlie.fcc.timer
  (:require [goog.dom :as gdom]
            [goog.events :refer [listen]])
  (:import [goog Timer]
           [goog.events EventType]))

(def session (gdom/getElement "time-left"))
(def session-length (gdom/getElement "session-length"))
(def break-length (gdom/getElement "break-length"))
(def time-left (gdom/getElement "time-left"))
(def minutes-timer (Timer. 60000))
(def seconds-timer (Timer. 1000))

;; break buttons
(def break-buttons
  (let [children (gdom/getChildren (gdom/getElement "break-label"))]
    [(.namedItem children "break-decrement") (.namedItem children "break-increment")]))

;; session buttons 
(def session-buttons
  (let [children (gdom/getChildren (gdom/getElement "session-label"))]
    [(.namedItem children "session-decrement") (.namedItem children "session-increment")]))

(defn shorten [el]
  (listen el EventType.CLICK
          (fn [e] (let [el (.-target e)
                        sib (gdom/getNextElementSibling el)
                        current (js/parseInt (gdom/getTextContent sib))]
                    (gdom/setTextContent sib (if (< 1 current) (dec current) current))
                    (when (= "session-length" (.-id sib))
                      (gdom/setTextContent time-left (str (gdom/getTextContent sib) ":00")))))))

(defn lengthen [el]
  (listen el EventType.CLICK
          (fn [e] (let [el (.-target e)
                        sib (gdom/getPreviousElementSibling el)
                        current (js/parseInt (gdom/getTextContent sib))]
                    (gdom/setTextContent sib (if (< current 60) (inc current) current))
                    (when (= "session-length" (.-id sib))
                      (gdom/setTextContent time-left (str (gdom/getTextContent sib) ":00")))))))

(defn start-timer []
  (let [time (js/parseInt (.-innerText session-length))
        minutes (atom time :validator pos-int?)
        seconds (atom 60)]
    (.listen minutes-timer Timer.TICK
             (fn [_] (swap! minutes dec)
               (gdom/setTextContent time-left (str @minutes ":" (if (< @seconds 10)
                                                                  (str "0" @seconds)
                                                                  @seconds)))))
    (.listen seconds-timer Timer.TICK
             (fn [_]
               (cond
                 (= @minutes time) (swap! minutes dec)
                 (zero? @seconds) (do (reset! seconds 60) (gdom/setTextContent session (str @minutes ":00")))
                 :else (gdom/setTextContent session (str @minutes ":" (if (< @seconds 10)
                                                                        (str "0" @seconds)
                                                                        @seconds))))
               (swap! seconds dec)))
    (.start minutes-timer)
    (.start seconds-timer)))

(defn pause-timer []
  (listen (gdom/getElement "start_stop") EventType.CLICK (fn [_] 
                                                           (when (and (.-enabled minutes-timer)
                                                                      (.-enabled seconds-timer))
                                                             (.stop minutes-timer)
                                                             (.stop seconds-timer)))))

(defn do-listen-start []
  (listen (gdom/getElement "start_stop") EventType.CLICK (fn [_] (start-timer) (pause-timer))))

(defn reset-timer []
  (listen (gdom/getElement "reset") EventType.CLICK
          (fn [_]
            (gdom/setTextContent session-length 25)
            (gdom/setTextContent break-length 5)
            (gdom/setTextContent session "25:00")
            (.stop minutes-timer)
            (.stop seconds-timer))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^:dev/after-load run []
  (js/console.log "starting timer"))

(defn ^:export play []
  (let [buttons (into [] cat [break-buttons session-buttons
                              [(gdom/getElement "start_stop") (gdom/getElement "reset")]])]
    (doseq [button buttons]
      (condp = (.-innerText button)
        "Dec" (shorten button)
        "Inc" (lengthen button)
        "Start/Stop" (do-listen-start)
        "Reset" (reset-timer)))))

(comment
  (gdom/getElementsByClassName "button"))