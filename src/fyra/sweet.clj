(ns fyra.sweet
  (:require [fyra.core :as f]))

(defn- resolve-args [[doc-string? & rest :as full]]
  (if (string? doc-string?)
      (into [[doc-string?]] rest)
      (into [[]] full)))

(defmacro defrelvar
  "Define a relational variable
  Relvars create a stateful group of items
  which can be operated on using fyra.relational"
  [name & args]
  (let [[docs m & extra] (resolve-args args)]
    `(def ~name ~@docs (f/relvar ~(str name) ~m ~@extra))))

(defmacro defview
  "Create a derived relvar as a named view"
  [name & args]
  (let [[docs rel] (resolve-args args)]
    `(def ~name ~@docs (f/view ~(str name) ~rel))))

(defmacro constrain
  "Ensure a condition is never violated"
  [explanation expr]
  `(throw (Exception. "Not Implemented")))

(defmacro declare-store
  "Declare some storage hints for performance reasons"
  [type & args] `(throw (Exception. "Not Implemented")))
