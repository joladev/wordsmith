(ns wordsmith.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [markdown.core :as md]))

(enable-console-print!)

(def app-state (atom {:text "Hello world!"}))

(defn wordsmith-app [state owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
        (dom/div nil)
        (dom/div nil)))))

(om/root
  app-state
  wordsmith-app  
  (. js/document (getElementById "app")))
