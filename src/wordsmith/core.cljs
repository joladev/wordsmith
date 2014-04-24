(ns wordsmith.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]
            [wordsmith.editor :as e]
            [cljs.core.async :refer [put! chan <!]]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.events :as events])
  (:import [goog.events EventType]))

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
     :last-title ""
     :channel (chan)}))

;; Title field

(defn handle-title-change [event app]
  (let [new-title (.. event -target -value)]
    (om/update! app :title new-title)))

(defn handle-title-blur [event app]
  (let [new-title (.. event -target -value)]
    (p/set-document new-title (:input @app))
    (p/remove-document (:last-title @app))
    (om/update! app :last-title new-title)
    (om/update! app :titles (p/get-all-titles))))

(defn title-field [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "title-field"}
        (dom/input #js {:type "text"
                        :onChange #(handle-title-change % app)
                        :onBlur #(handle-title-blur % app)
                        :value (:title app)})))))

;; Left menu

(defn update-current [event app]
  (let [title (.. event -target -textContent)]
    (om/update! app :input (p/get-document title))
    (om/update! app :title title)
    (om/update! app :last-title title)))

(defn delete-click [title app]
  (p/remove-document title)
  (om/update! app :titles (p/get-all-titles)))

(defn left-menu [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "left-menu"}
        (apply dom/ul nil
          (map #(dom/li 
                  #js {:key (str %)}
                  (dom/span #js {:id "delete-button"
                                 :onClick (fn [e] (delete-click % app))} "x")
                  (dom/span #js {:onClick (fn [e] (update-current e app))}
                    (om/value %)))
               (:titles app)))))))

;; Save button

(defn format-time [date]
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
            (str "Last saved at: " (format-time last-saved))))))))

;; The main app

(defn save-document [app]
  (om/update! app :last-saved (js/Date.))
  (om/update! app :last-title (:title @app))
  (p/set-document (:title @app) (:input @app))
  (om/update! app :titles (p/get-all-titles)))

(defn listen [el type app]
  (events/listen el type
    #(when 
       (and (.-metaKey %)
            (= 83 (.-keyCode %)))
       (.preventDefault %)
       (put! (:channel @app) [:save nil]))))

(defn wordsmith-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :titles (p/get-all-titles))
      (let [channel (:channel app)]
        (go-loop []
          (let [[action params] (<! channel)]
            (save-document app)
            (recur))))
      (listen js/document EventType/KEYDOWN app))
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
        (om/build title-field app)
        (om/build save-button app)
        (om/build left-menu app)
        (om/build e/editor (:input app))))))

(om/root
  wordsmith-app
  app-state
  {:target (. js/document (getElementById "app"))})
