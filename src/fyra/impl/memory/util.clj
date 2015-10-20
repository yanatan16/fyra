(ns fyra.impl.memory.util
  (:require [clojure.core.typed :as t]))


(t/ann map-values
  (t/All [k v1 v2]
    (t/Fn [(t/Fn [v1 -> v2]) (t/Map k v1) -> (t/Map k v2)])))
(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))