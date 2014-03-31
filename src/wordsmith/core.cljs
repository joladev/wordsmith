(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]))

(enable-console-print!)

(def app-state (atom {}))

(defn handle-input [event owner]
  (let [input (.. event -target -value)
        output (.marked js/window input)
        output-area (om/get-node owner "output-area")]
    (om/set-state! owner :input (.. event -target -value))
    (set! (.-innerHTML output-area) output)))

(defn handle-title-change [event owner]
  (let [title (.. event -target -value)
        old-title (om/get-state owner :title)]
    (when-not (= "" title)
      (om/set-state! owner :title title)
      (p/remove-document old-title)
      (p/set-document title (om/get-state owner :input)))))

(defn menu [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:title ""})
    om/IRenderState
    (render-state [_ {:keys [title]}]
      (dom/div #js {:id "wordsmith-menu"}
       (dom/input #js {:type "text" :onBlur #(handle-title-change % owner)}
                  title)))))

(defn markdown-field [app owner]
  (reify
    om/IInitState
    (init-state [_]
      (:input ""))
    om/IRenderState
    (render-state [_ {:keys [input]}]
      (dom/div nil (.marked js/window input)))))

(defn wordsmith-app [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:input ""})
    om/IRenderState
    (render-state [_ state]
      (dom/div nil
        (om/build menu app {:init-state state})
        (dom/div #js {:className "container"}
          (dom/textarea #js {:onInput #(handle-input % owner)})
          (dom/div #js {:ref "output-area"}))))))

(om/root
  app-state
  wordsmith-app
  (. js/document (getElementById "app")))
