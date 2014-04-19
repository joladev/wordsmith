(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]
            [wordsmith.editor :as e]))

(enable-console-print!)

(extend-type string
  ICloneable
  (-clone [s] (js/String. s)))

(def app-state (atom {:input "" :titles [] :current "" :title ""}))

(defn handle-title-change [event app]
  (let [new-title (.. event -target -value)]
    (when-not (= "" new-title)
      (om/update! app :title new-title))))

(defn title-field [app owner]
  (reify
    om/IRender
    (render [_]
      (let [title (:title app)]
        (dom/div #js {:id "title-field"}
          (dom/input #js {:type "text" :onBlur #(handle-title-change % title)}
                     (:current app)))))))

(defn update-current [event app]
  (om/update! app :current (.. event -target -textContent)))

(defn left-menu [app owner]
  (reify
    om/IRender
    (render [_]
      (let [current (:current app)]
        (dom/div #js {:id "left-menu"}
          (apply dom/ul nil
            (map #(dom/li 
                    #js {:key (str %) 
                         :onClick (fn [e] (update-current e current))}
                    (om/value %))
                 (:titles app))))))))

(defn wordsmith-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! (:titles app) :titles (p/get-all-titles)))
    om/IRender
    (render [_]
      (print app)
      (dom/div #js {:className "container"}
        (om/build title-field app)
        (om/build left-menu app)
        (om/build e/editor (:input app))))))

(om/root
  app-state
  wordsmith-app
  (. js/document (getElementById "app")))
