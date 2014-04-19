(ns wordsmith.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn handle-input [event input owner]
  (om/update! input (.. event -target -value)))

(defn editor [input owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/textarea #js {:id "input-area"
                           :onInput #(handle-input % input owner)
                           :value input})
        (dom/div #js {:ref "output-area"
                      :id "output-area"
                      :dangerouslySetInnerHTML #js {:__html (.marked js/window input)}})))))
