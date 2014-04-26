(ns wordsmith.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [wordsmith.persistence :as p]
            [cljs.core.async :refer [put! chan <!]]
            [goog.events :as events])
  (:import [goog.events EventType]))

(enable-console-print!)

(extend-type string
  ICloneable
  (-clone [s] (js/String. s)))

(extend-type js/String
  ICloneable
  (-clone [s] (js/String. s))
  om/IValue
  (-value [s] (str s)))

(def app-state 
  (atom 
    {:input ""
     :titles []
     :title ""
     :last-title ""
     :last-input ""
     :channel (chan)}))

;; Title field

(defn handle-title-change
  "Updates the app state with the latest text from the title input."
  [event app]
  (let [new-title (.. event -target -value)]
    (om/update! app :title new-title)))

(defn title-field
  "Component that renders title input and handles updating of title app state."
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "title-field"}
        (dom/input #js {:type "text"
                        :onChange #(handle-title-change % app)
                        :value (:title app)})))))

;; New document

(defn new-document-click
  "Sends a :new command on the app channel."
  [app]
  (put! (:channel @app) [:new nil]))

(defn new-button
  "Component that renders the 'add new' button and handles triggering new
   document state on app."
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/span #js {:id "new-document"
                     :onClick #(new-document-click app)} "+"))))

;; Left menu

(defn update-current
  "Sends a :change command on the app channel."
  [event app]
  (let [title (.. event -target -textContent)]
    (put! (:channel @app) [:change title])))

(defn delete-click
  "Shows a prompt asking the user for confirmation. If confirmation
   is true, sends a :remove command on the app channel."
  [title app]
  (let [response (js/confirm "Are you sure?")]
    (when response
      (put! (:channel @app) [:remove title]))))

(defn left-menu
  "Component that renders the left-menu containing all the documents
   available in localStorage. Clicking on a title triggers an app state
   change by sending a :change command on the app channel. Clicking on
   a delete button does that same with the :remove command."
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "left-menu"}
        (apply dom/ul nil
          (map #(dom/li 
                  #js {:key (str %)}
                  (dom/span #js {:className "delete-button"
                                 :onClick (fn [e] (delete-click % app))} "x")
                  (dom/span #js {:className "left-menu-title"
                                 :onClick (fn [e] (update-current e app))}
                    (om/value %)))
               (:titles app)))))))

;; Save button

(defn button-click
  "Sends a :save command on the app channel."
  [app]
  (put! (:channel @app) [:save nil]))

(defn saved?
  "Determines whether or not the app state has changed since last
   save."
  [{:keys [input last-input title last-title]}]
  (and 
    (= input last-input)
    (= title last-title)))

(defn save-button
  "Component that renders the save button. The button is disabled
   when there are no changes to title or input. Clicking the save
   button triggers the app state to update and saves to localStorage."
  [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/button #js {:id "save-button"
                       :disabled (saved? app)
                       :className (when (saved? app) "disabled")
                       :onClick #(button-click app)}
                  (if (saved? app)
                    "Saved"
                    "Save")))))

;; Editor

(defn handle-input
  "Handles input change and updates the input app state."
  [event input]
  (om/update! input (.. event -target -value)))

(defn marked
  "Calls out to the marked.js library to compile input."
  [md]
  (.marked js/window md))

(defn editor
  "Component that renders the editor and preview. Compiles the app state input
   to markdown and dangerously sets it to output-area. Editing the input-area 
   triggers an update to the app state."
  [input owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/textarea #js {:id "input-area"
                           :onChange #(handle-input % input)
                           :value input})
        (dom/div 
          #js {:ref "output-area"
               :id "output-area"
               :dangerouslySetInnerHTML #js {:__html (marked input)}})))))

;; The main app

(defn update-many!
  "Sequentially updates multiple keys on Om cursor to the given value."
  [app ks v]
  (doseq [k ks]
    (om/update! app k v)))

(defn reset-document-state!
  "Sets the document related keys in the app state to a 'clean' state."
  [app]
  (update-many! app [:input :title :last-title :last-input] ""))

(defn new-document
  "A new document simply means resetting the document state."
  [app]
  (reset-document-state! app))

(defn change-document
  "Changing between documents requires fetching the new document
   from localStorage and updating both input/title and last-input/last-title
   to the new document state."
  [app title]
  (let [input (p/get-document title)]
    (update-many! app [:input :last-input] input)
    (update-many! app [:title :last-title] title)))

(defn save-document
  "If title is the same as last-title it renames the document. Then
   updates :last-input and :last-title to the current input and title.
   Finally refreshes the :titles with the latest results from localStorage."
  [app]
  (let [{:keys [title input last-title last-input]} @app]
    (when-not (= title last-title)
      (p/rename-document last-title title))
    (om/update! app :last-input input)
    (om/update! app :last-title title)
    (p/set-document title input)
    (om/update! app :titles (p/get-all-titles))))

(defn remove-document
  "Removes a document from localStorage and updates app state. If the
   removed document was the current document, also triggers a document
   state reset."
  [app title]
  (p/remove-document title)
  (om/update! app :titles (p/get-all-titles))
  (when (= (:title @app) title)
    (reset-document-state! app)))

(defn dispatch
  "Dispatches the incoming commands on the app channel."
  [command params app]
  (case command
    :save   (save-document app)
    :remove (remove-document app params)
    :change (change-document app params)
    :new    (new-document app)))

(defn listen
  "Listens to KEYDOWN events using goog.events and checks for Ctrl+S or 
   Cmd+S. When identified, sends a :save command on app channel."
  [el type app]
  (events/listen el type
    #(when 
       (and (or (.-metaKey %) (.-ctrlKey %))
            (= 83 (.-keyCode %)))
       (.preventDefault %)
       (put! (:channel @app) [:save nil]))))

(defn wordsmith-app
  "The starting point of the wordsmith app. Implements will-mount and 
   render."
  [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      "Fetches document titles and starts the asynchronous command event 
       dispatch loop which handles all major app state changing events.
       Also attaches a KEYDOWN event listener to the page, for hot keys."
      (om/update! app :titles (p/get-all-titles))
      (let [channel (:channel app)]
        (go-loop []
          (let [[command params] (<! channel)]
            (dispatch command params app)
            (recur))))
      (listen js/document EventType/KEYDOWN app))
    om/IRender
    (render [_]
      "Renders the app components in a container."
      (dom/div #js {:className "container"}
        (om/build title-field app)
        (om/build new-button app)
        (om/build save-button app)
        (om/build left-menu app)
        (om/build editor (:input app))))))

(om/root
  wordsmith-app
  app-state
  {:target (. js/document (getElementById "app"))})
