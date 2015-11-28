(ns fyra.run-tests
  (:require #?(:clj [clojure.test :refer [run-tests]]
               :cljs [cljs.test :refer-macros [run-tests]])))

(defn run []
  (run-tests 'fyra.core-test))
