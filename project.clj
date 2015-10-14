(defproject fyra "0.1.0-SNAPSHOT"
  :description "An example implementation of Functional Relational Programming as described by Marks and Moseley"
  :url "https://github.com/yanatan16/fyra"
  :license {:name "MIT"
            :url "https://github.com/yanatan16/fyra/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-time "0.11.0"]]
  :core.typed {:check [fyra.core fyra.relational fyra.core_test]}
  :profiles {:dev {:dependencies [[midje "1.7.0"]
                                  [org.clojure/core.typed "0.3.11"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-typed "0.3.5"]]
                   :injections [(use 'midje.repl)]}})
