(ns fyra.aggs)

;; Summarizers
(defn sum-key
  "Sum a key in a collection"
  [k coll] (apply + (map k coll)))

(defn maximum-key
  "Find the item in a collection with maximum value at key k"
  [k coll] (vector (reduce #(if (> (k %1) (k %2)) %1 %2) coll)))
