(defproject wordsmith "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.6.0"]
                 [com.facebook/react "0.8.0.1"]]

  :plugins [[lein-cljsbuild "1.0.1"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "wordsmith"
              :source-paths ["src"]
              :compiler {
                :output-to "wordsmith.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
