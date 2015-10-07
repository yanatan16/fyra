(ns fyra.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.core :as mem]
            [fyra.impl.memory.meta :as meta]))

(defn relvar
  "Create a base relvar with name, fields in m, and extra arguments
  Extra arguments can be :foreign or :candidate"
  [name m & {:as extra}]
  (meta/declare-relvar (merge (select-keys extra [:foreign :candidate])
                         {:fields m :name name})))

(defn view
  "Create a view (named derived relvar)"
  [name rel]
  (meta/declare-view name rel))

(defn constrain
  "Create a system constraint that must always be valid.
  f is a function of a relation that executes it in the
  interim db state"
  [explanation f]
  (meta/declare-constraint explanation f))

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
  [rel f] (mem/update rel f))