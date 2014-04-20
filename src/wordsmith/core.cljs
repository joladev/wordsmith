(ns wordsmith.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]
            [wordsmith.editor :as e]
            [cljs.core.async :refer [put! chan <!]]
            [goog.string :as gstring]
            [goog.string.format]))

(enable-console-print!)

(extend-type string
  ICloneable
  (-clone [s] (js/String. s)))

(extend-type js/String
  ICloneable
  (-clone [s] (js/String. s))
  om/IValue
  (-value [s] (str s)))

(def app-state 
  (atom 
    {:input ""
     :titles []
     :title ""
     :last-saved nil
     :channel (chan)}))

;; Title field

(defn handle-title-change [event app]
  (let [new-title (.. event -target -value)]
    (om/update! app :title new-title)))

(defn title-field [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "title-field"}
        (dom/input #js {:type "text"
                        :onChange #(handle-title-change % app)
                        :value (:title app)})))))

;; Left menu

(defn update-current [event app]
  (let [title (.. event -target -textContent)]
    (om/update! app :input (p/get-document title))
    (om/update! app :title title)))

(defn left-menu [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "left-menu"}
        (apply dom/ul nil
          (map #(dom/li 
                  #js {:key (str %) 
                       :onClick (fn [e] (update-current e app))}
                  (om/value %))
               (:titles app)))))))

;; Save button

(defn get-time [date]
  (let [hours (.getHours date)
        minutes (.getMinutes date)
        seconds (.getSeconds date)]
    (gstring/format "%02d:%02d:%02d" hours minutes seconds)))

(defn button-click [app]
  (put! (:channel @app) [:save nil]))

(defn save-button [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/button #js {:id "save-button" 
                         :onClick #(button-click app)} "Save")
        (dom/span #js {:id "last-saved"} 
          (when-let [last-saved (:last-saved app)]
            (str "Last saved at: " (get-time last-saved))))))))

;; The main app

(defn save-document [app]
  (println (p/get-all-titles))
  (om/update! app :last-saved (js/Date.))
  (p/set-document (:title @app) (:input @app))
  (om/update! app :titles (p/get-all-titles)))

(defn wordsmith-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :titles (p/get-all-titles))
      (let [channel (:channel app)]
        (go-loop []
          (let [[action params] (<! channel)]
            (save-document app)
            (recur)))))
    om/IRender
    (render [_]
            (println (:titles app))
      (dom/div #js {:className "container"}
        (om/build title-field app)
        (om/build save-button app)
        (om/build left-menu app)
        (om/build e/editor (:input app))))))

(om/root
  wordsmith-app
  app-state
  {:target (. js/document (getElementById "app"))})
