(defproject fyra "0.1.0-SNAPSHOT"
  :description "An example implementation of Functional Relational Programming as described in Out of the Tar Pit"
  :url "https://github.com/yanatan16/fyra"
  :license {:name "MIT"
            :url "https://github.com/yanatan16/fyra/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.7.0"]

                 ;; clj/cljs
                 [prismatic/schema "1.0.3"]]
  :profiles {:dev {:dependencies [[midje "1.8.2"]
                                  [org.clojure/clojurescript "1.7.170"]]
                   :plugins [[lein-midje "3.2"]
                             [lein-cljsbuild "1.1.1"]]
                   :injections [(use 'midje.repl)]
                   :cljsbuild {:builds [{:source-paths ["src"]
                                         :compiler {:output "target/test.js"
                                                    :optimizations [:whitespace]
                                                    :pretty-print true}}]}}})
