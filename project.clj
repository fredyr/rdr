(defproject rdr "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156"]
                 [secretary "0.4.0"]
                 [om "0.5.0"]]

  :plugins [[lein-cljsbuild "1.0.1"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "rdr"
              :source-paths ["src"]
              :compiler {
                :output-to "rdr.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
