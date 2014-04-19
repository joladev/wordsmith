(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]
            [wordsmith.editor :as e]))

(enable-console-print!)

(extend-type string
  ICloneable
  (-clone [s] (js/String. s)))

(extend-type js/String
  ICloneable
  (-clone [s] (js/String. s))
  om/IValue
  (-value [s] (str s)))

(def app-state (atom {:input "" :titles [] :title ""}))

;; Title field

(defn handle-title-change [event app]
  (let [new-title (.. event -target -value)]
    (when-not (= "" new-title)
      (om/update! app :title new-title))))

(defn title-field [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "title-field"}
        (dom/input #js {:type "text"
                        :onBlur #(handle-title-change % app)
                        :value (:title app)})))))

;; Left menu

(defn update-current [event app]
  (om/update! app :input (p/get-document (.. event -target -textContent)))
  (om/update! app :title (.. event -target -textContent)))

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

;; The main app

(defn wordsmith-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :titles (p/get-all-titles)))
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
        (om/build title-field app)
        (om/build left-menu app)
        (om/build e/editor (:input app))))))

(om/root
  wordsmith-app
  app-state
  {:target (. js/document (getElementById "app"))})
