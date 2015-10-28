(defproject fyra "0.1.0-SNAPSHOT"
  :description "An example implementation of Functional Relational Programming as described in Out of the Tar Pit"
  :url "https://github.com/yanatan16/fyra"
  :license {:name "MIT"
            :url "https://github.com/yanatan16/fyra/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-time "0.11.0"]]
  :profiles {:dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]]
                   :injections [(use 'midje.repl)]}})
