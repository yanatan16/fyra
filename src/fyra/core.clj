(ns fyra.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.core :as mem]
            [fyra.impl.memory.types :as memt]))

(defn relvar
  "Create a base relvar with name, fields in m, and extra arguments
  Extra arguments can be :foreign or :candidate"
  [name m & {:keys [candidate foreign]}]
  (mem/declare-relvar (cond-> {:name name
                               :fields m}
                        candidate (assoc  :candidate candidate)
                        foreign (assoc  :foreign foreign))))

(defn view
  "Create a view (named derived relvar)"
  [name rel]
  (mem/declare-view name rel))

(defn constrain
  "Create a system constraint that must always be valid.
  f is a function of a relation that executes it in the
  interim db state"
  [explanation rel f]
  (mem/declare-constraint explanation rel f))

(defn observe
  "Create an observation callback on a relation"
  [key rel f]
  (mem/declare-observer key rel f))

(defn select
  "Execute a selection operation on a relation"
  [rel] (mem/select rel))

(defn insert
  "Execute a insertion operation on a base relation"
  [baserel & items] (apply mem/insert baserel items))

(defn delete
  "Execute a deletion operation on a relation."
  [rel] (mem/delete rel))

(defn update
  "Execute a update operation on a relation.
  Applies f to each item."
  [rel updts] (mem/update rel updts))


(defn reltype
  "Get the type of a relation"
  [rel]
  (memt/reltype rel))
