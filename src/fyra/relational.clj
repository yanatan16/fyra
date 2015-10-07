(ns fyra.relational
  (:refer-clojure :exclude [extend max])
  (:require [fyra.impl.memory.core :as db]
            [fyra.impl.memory.meta :refer (relvar?)]
            [fyra.impl.memory.relational :as mem]))

(defn project
  "set-projection of the relation. equivalent to mapping select-keys"
  [rel & ks]
  (apply mem/project rel ks))

(defn project-away
  "the dual of project. equivalent to mapping dissoc-keys"
  [rel & ks]
  (apply mem/project-away rel ks))

(defn extend
  "extend the relation with more fields.
  map over items, adding fields by calling
  each of exts values as a function of the item

  example: (extend rel :new-field #(* (:width %) (:height %)))"
  [rel & exts]
  (apply mem/extend rel exts))


(defn restrict
  "restrict the relation by a condition
  equivalent to a filter on the items"
  [rel f]
  (mem/restrict rel f))

(defn summarize
  "aggregation of the relation
  first, it aggregates the items in the relation
  by the fields of the items defined in agg-keys
  Then, if agg-op is a function, it calls the function
  on each of the aggregated groups.
  If agg-op is a map of keywords to functions, it calls each
  value function on the aggregated group and merges this
  aggregated map with the aggregated keys map

  example 1: (summarize rel [:group] #(maximum-key :priority %))
  example 2: (summarize rel [:group] {:members count})"
  [rel agg-keys agg-op]
  (mem/summarize rel agg-keys agg-op))

(defn join
  "set-join two relations"
  [rel-1 rel-2]
  (mem/join rel-1 rel-2))

(defn minus
  "set-minus two relations"
  [rel-1 rel-2]
  (mem/minus rel-1 rel-2))

;; Summarizers
(defn sum-key
  "Sum a key in a collection"
  [k coll] (apply + (map k coll)))
(defn maximum-key
  "Find the item in a collection with maximum value at key k"
  [k coll] (reduce #(if (> (k %1) (k %2)) %1 %2) coll))