(ns wordsmith.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn handle-input [event app owner]
  (let [new-input (.. event -target -value)
        output (.marked js/window new-input)
        output-area (om/get-node owner "output-area")]
    (om/update! app :input (.. event -target -value))
    (set! (.-innerHTML output-area) output)))

(defn editor [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/textarea #js {:id "input-area" :onInput #(handle-input % app owner)})
        (dom/div #js {:ref "output-area" :id "output-area"})))))
