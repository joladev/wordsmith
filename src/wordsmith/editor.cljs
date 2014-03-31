(ns wordsmith.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn handle-input [event owner]
  (let [input (.. event -target -value)
        output (.marked js/window input)
        output-area (om/get-node owner "output-area")]
    (om/set-state! owner :input (.. event -target -value))
    (set! (.-innerHTML output-area) output)))

(defn editor [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [input] :as state}]
      (dom/div nil
        (dom/textarea #js {:id "input-area" :onInput #(handle-input % owner)})
        (dom/div #js {:ref "output-area" :id "output-area"})))))
