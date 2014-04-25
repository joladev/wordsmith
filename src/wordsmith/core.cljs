(ns wordsmith.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]
            [wordsmith.editor :as e]
            [wordsmith.utilities :as utilities]
            [cljs.core.async :refer [put! chan <!]]
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
    (put! (:channel @app) [:rename new-title])))

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
  (put! (:channel @app) [:remove title]))

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
            (str "Last saved at: " (utilities/format-time last-saved))))))))

;; The main app

(defn save-document [app]
  (om/update! app :last-saved (js/Date.))
  (om/update! app :last-title (:title @app))
  (p/set-document (:title @app) (:input @app))
  (om/update! app :titles (p/get-all-titles)))

(defn rename-document [app new-title]
  (p/rename-document (:last-title @app) new-title)
  (save-document app))
  
(defn remove-document [app title]
  (p/remove-document title)
  (om/update! app :titles (p/get-all-titles)))

(defn dispatch [command params app]
  (case command
    :save   (save-document app)
    :rename (rename-document app params)
    :remove (remove-document app params)))

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
          (let [[command params] (<! channel)]
            (dispatch command params app)
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
