(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))

(defn handle-keydown [e app owner]
  (let [i (om/get-node owner "input-area")
        o (om/get-node owner "output-area")
        t (.-value i)
        md (.marked js/window (str t "\n"))]
    (set! (.-innerHTML o) md)))

(defn wordsmith-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
        (dom/textarea #js {:ref "input-area"
                           :onKeyUp #(handle-keydown % app owner)})
        (dom/div #js {:ref "output-area"})))))

(om/root
  app-state
  wordsmith-app  
  (. js/document (getElementById "app")))
