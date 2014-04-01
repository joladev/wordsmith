(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]
            [wordsmith.editor :as e]))

(enable-console-print!)

(extend-type string
  ICloneable
  (-clone [s] (js/String. s)))

(def app-state (atom {:input ""}))

(defn handle-title-change [event owner]
  (let [new-title (.. event -target -value)
        old-title (om/get-state owner :title)]
    (when-not (= "" new-title)
      (om/set-state! owner :title new-title))))

(defn title-field [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:title ""})
    om/IRenderState
    (render-state [_ {:keys [title]}]
      (dom/div #js {:id "title-field"}
       (dom/input #js {:type "text" :onBlur #(handle-title-change % owner)}
                  title)))))

(defn update-current [event owner]
  (om/set-state! owner :current (.. event -target -text)))

(defn left-menu [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [titles]}]
      (dom/div #js {:id "left-menu"}
        (apply dom/ul nil
          (map #(dom/li 
                  #js {:key (str %) :onClick (fn [e] (update-current e owner))} %)
               titles))))))

(defn wordsmith-app [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:input (:input app) :titles (p/get-all-titles)})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "container"}
        (om/build title-field app {:init-state state})
        (om/build left-menu app {:init-state state})
        (om/build e/editor app)))))

(om/root
  app-state
  wordsmith-app
  (. js/document (getElementById "app")))
