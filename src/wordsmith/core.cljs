(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {}))

(defn handle-input [event owner]
  (let [input (.. event -target -value)
        output (.marked js/window input)
        output-area (om/get-node owner "output-area")]
    (om/set-state! owner :input (.. event -target -value))
    (set! (.-innerHTML output-area) output)))

(defn wordsmith-app [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:input ""})
    om/IRenderState
    (render-state [_ _]
      (dom/div #js {:className "container"}
        (dom/textarea #js {:onInput #(handle-input % owner)
                           :value input})
        (dom/div #js {:ref "output-area"})))))

(om/root
  app-state
  wordsmith-app  
  (. js/document (getElementById "app")))
