(ns fyra.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.core :as mem]
            [fyra.types :as ft]
            [clojure.core.typed :as t]))

(t/ann relvar [String ft/RelType & :optional {:candidate ft/CandidateKeysInput
                                              :foreign ft/ForeignKeysInput}
             -> ft/UIRelation])
(defn relvar
  "Create a base relvar with name, fields in m, and extra arguments
  Extra arguments can be :foreign or :candidate"
  [name m & {:as extra}]
  (mem/declare-relvar (merge (select-keys extra [:foreign :candidate])
                      {:fields m :name name})))

(t/ann view [String ft/IRelation -> ft/IRelation])
(defn view
  "Create a view (named derived relvar)"
  [name rel]
  (mem/declare-view name rel))

(t/ann constrain [String ft/IConstrainable ft/ConstraintFn -> t/Any])
(defn constrain
  "Create a system constraint that must always be valid.
  f is a function of a relation that executes it in the
  interim db state"
  [explanation rel f]
  (mem/declare-constraint explanation rel f))

(t/ann observe [String memt/Observable ft/ObserveFn -> t/Any])
(defn observe
  "Create an observation callback on a relation"
  [key rel f]
  (mem/declare-observer key rel f))

(t/ann select [ft/Relation -> ft/Data])
(defn select
  "Execute a selection operation on a relation"
  [rel] (mem/select rel))

(t/ann insert [ft/UIRelation ft/Tuple * -> t/Any])
(defn insert
  "Execute a insertion operation on a base relation"
  [baserel & items] (apply mem/insert baserel items))

(t/ann delete [ft/URelation -> t/Any])
(defn delete
  "Execute a deletion operation on a relation."
  [rel] (mem/delete rel))

(t/ann update [ft/URelation (t/Map t/Kw [ft/Tuple -> t/Any]) -> t/Any])
(defn update
  "Execute a update operation on a relation.
  Applies f to each item."
  [rel updts] (mem/update rel updts))
