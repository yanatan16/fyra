(ns fyra.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.core :as mem]
            [fyra.impl.memory.meta :as meta]
            [fyra.types :refer [RelVarType CandidateKeysInput Constraint
                                ForeignKeysInput Relation]]
            [clojure.core.typed :refer [ann Kw U Fn Map Num]]))

(ann relvar [String RelVarType & :optional {:candidate CandidateKeysInput
                                            :foreign ForeignKeysInput}
             -> RelVar])
(defn relvar
  "Create a base relvar with name, fields in m, and extra arguments
  Extra arguments can be :foreign or :candidate"
  [name m & {:as extra}]
  (meta/declare-relvar (merge (select-keys extra [:foreign :candidate])
                         {:fields m :name name})))

(ann view [String Relation -> Relation])
(defn view
  "Create a view (named derived relvar)"
  [name rel]
  (meta/declare-view name rel))

(ann constrain [String Constraint -> Any])
(defn constrain
  "Create a system constraint that must always be valid.
  f is a function of a relation that executes it in the
  interim db state"
  [explanation f]
  (meta/declare-constraint explanation f))

(ann select ExecuteRelation)
(defn select
  "Execute a selection operation on a relation"
  [rel] (mem/select rel))

(ann insert [Relation Tuple * -> Any])
(defn insert
  "Execute a insertion operation on a base relation"
  [baserel & items] (apply mem/insert baserel items))

(ann delete [Relation -> Any])
(defn delete
  "Execute a deletion operation on a relation."
  [rel] (mem/delete rel))

(ann update [Relation (Map Kw (Fn [Tuple -> Any])) -> Any])
(defn update
  "Execute a update operation on a relation.
  Applies f to each item."
  [rel updts] (mem/update rel updts))