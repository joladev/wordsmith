(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {}))

(defn handle-input [event app owner]
  (om/set-state! owner :input (.. event -target -value))
  (let [input (om/get-state owner :input)
        output (.marked js/window input)
        output-area (om/get-node owner "output-area")]
    (set! (.-innerHTML output-area) output)))

(defn wordsmith-app [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:input ""})
    om/IRenderState
    (render-state [_ {:keys [input]}]
      (dom/div #js {:className "container"}
        (dom/textarea #js {:onInput #(handle-input % app owner)
                           :value input})
        (dom/div #js {:ref "output-area"})))))

(om/root
  app-state
  wordsmith-app  
  (. js/document (getElementById "app")))
