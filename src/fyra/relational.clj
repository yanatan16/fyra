(ns fyra.relational
  (:refer-clojure :exclude [extend max])
  (:require [fyra.impl.memory.relational :as mem]
            [fyra.types :as ft]
            [clojure.core.typed :refer [ann Kw U Fn Map Num]]))

(ann project [ft/Relation Kw * -> ft/Relation])
(defn project
  "set-projection of the relation. equivalent to mapping select-keys"
  [rel & ks]
  (apply mem/project rel ks))

(ann project-away [ft/Relation Kw * -> ft/Relation])
(defn project-away
  "the dual of project. equivalent to mapping dissoc-keys"
  [rel & ks]
  (apply mem/project-away rel ks))

(ann extend [ft/Relation (Map Kw (Fn [ft/Tuple -> Any])) * -> ft/Relation])
(defn extend
  "extend the relation with more fields.
  map over items, adding fields by calling
  each of exts values as a function of the item

  example: (extend rel :new-field #(* (:width %) (:height %)))"
  [rel exts]
  (mem/extend rel exts))

(ann restrict [ft/Relation (Fn [ft/Tuple -> Boolean]) -> ft/Relation])
(defn restrict
  "restrict the relation by a condition
  equivalent to a filter on the items"
  [rel f]
  (mem/restrict rel f))

(ann summarize [ft/Relation (t/Vec t/Kw) (t/U [(t/Seqable ft/Tuple) -> ft/Tuple]
                                         (t/Map t/Kw [(t/Seqable ft/Tuple) -> t/Any])) -> ft/Relation])
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

(ann join [ft/Relation ft/Relation -> ft/Relation])
(defn join
  "set-join two relations"
  [rel-1 rel-2]
  (mem/join rel-1 rel-2))

(ann minus [ft/Relation ft/Relation -> ft/Relation])
(defn minus
  "set-minus two relations"
  [rel-1 rel-2]
  (mem/minus rel-1 rel-2))

;; Summarizers
(ann sum-key [t/Kw (t/Seqable ft/Tuple) -> t/AnyNum])
(defn sum-key
  "Sum a key in a collection"
  [k coll] (apply + (map k coll)))

(ann maximum-key [t/Kw (t/Seqable ft/Tuple) -> ft/Tuple])
(defn maximum-key
  "Find the item in a collection with maximum value at key k"
  [k coll] (vector (reduce #(if (> (k %1) (k %2)) %1 %2) coll)))