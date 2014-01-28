(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))

(defn handle-input [event app owner]
  (let [input-area (om/get-node owner "input-area")
        output-area (om/get-node owner "output-area")
        text (.-value input-area)
        markdown (.marked js/window text)]
    (set! (.-innerHTML output-area) markdown)))

(defn wordsmith-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
        (dom/textarea #js {:ref "input-area"
                           :onInput #(handle-input % app owner)})
        (dom/div #js {:ref "output-area"})))))

(om/root
  app-state
  wordsmith-app  
  (. js/document (getElementById "app")))
