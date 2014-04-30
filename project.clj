(defproject wordsmith "0.0.1"
  :description "A markdown editor with live preview and localStorage
                saving. Built using ClojureScript and Om."
  :url "https://github.com/eakron/wordsmith"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.6.2"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "release"
              :source-paths ["src"]
              :compiler {
                :output-to "resources/main.js"
                :optimizations :advanced
                :pretty-print false
                :preamble ["react/react.min.js"]
                :externs ["react/externs/react.js"
                          "marked.js"]
                :closure-warnings {:externs-validation :off}}}
             {:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "resources/main.dev.js"
                :output-dir "resources/dev"
                :optimizations :none}}]})
