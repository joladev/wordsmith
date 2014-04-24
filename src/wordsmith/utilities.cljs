(ns wordsmith.utilities
  (:require [goog.string :as gstring]
            [goog.string.format]))

(defn format-time [date]
  (let [hours (.getHours date)
        minutes (.getMinutes date)
        seconds (.getSeconds date)]
    (gstring/format "%02d:%02d:%02d" hours minutes seconds)))
