(ns fyra.impl.memory.util)


(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))