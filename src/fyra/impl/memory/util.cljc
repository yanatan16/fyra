(ns fyra.impl.memory.util
  (:refer-clojure :exclude [format])
  #?(:cljs (:require [goog.string :as gstring])))


#?(:cljs (def format gstring/format)
   :clj (def format clojure.core/format))

(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))
