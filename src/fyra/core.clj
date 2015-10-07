(ns fyra.core
  (:refer-clojure :exclude [ensure update])
  (:require [fyra.impl.memory.core :as mem]
            [fyra.impl.memory.meta :refer (declare-relvar)]))

(defn- resolve-args [[doc-string? & rest :as full]]
  (if (string? doc-string?)
      (into [[doc-string?]] rest)
      (into [[]] full)))

(defn relvar
  "Create a base relvar with name, fields in m, and extra arguments
  Extra arguments can be :foreign or :candidate"
  [name m & {:as extra}]
  (declare-relvar (merge (select-keys extra [:foreign :candidate])
                         {:fields m :name name})))

(defmacro defrelvar
  "Define a relational variable
  Relvars create a stateful group of items
  which can be operated on using fyra.relational"
  [name & args]
  (let [[docs m & extra] (resolve-args args)]
    `(def ~name ~@docs (relvar ~(str name) ~m ~@extra))))

(defmacro defview
  "Create a derived relvar as a named view"
  [name & args] `(throw (Exception. "Not Implemented")))
(defmacro ensure
  "Ensure a condition is never violated"
  [expr] `(throw (Exception. "Not Implemented")))
(defmacro declare-store
  "Declare some storage hints for performance reasons"
  [type & args] `(throw (Exception. "Not Implemented")))

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