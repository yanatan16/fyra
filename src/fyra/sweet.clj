(ns fyra.sweet
  (:refer-clojure :exclude [update extend])
  (:require [clojure.walk :as walk]
            [fyra.core :as f]
            [fyra.relational :as r]))

(defn- resolve-def-args [[name doc-string? & rest-args :as full]]
  (if (string? doc-string?)
    [name `(~doc-string?) rest-args]
    [name `() (rest full)]))

(defmacro defrelvar
  "Define a relational variable
  Relvars create a stateful group of items
  which can be operated on using fyra.relational"
  [args]
  (let [[name def-args relvar-args] (resolve-def-args args)]
    `(def ~name ~@def-args (f/relvar ~(str name) ~@relvar-args))))

(defmacro defview
  "Create a derived relvar as a named view"
  [args]
  (let [[name def-args view-args] (resolve-def-args args)]
    `(def ~name ~@def-args (f/view ~(str name) ~@view-args))))

(def constraint f/constrain)
(def reltype f/reltype)

(defmacro declare-store
  "Declare some storage hints for performance reasons"
  [type & args]
  `()) ; Not implemented

;; fyra.core synonyms

(def observe f/observe)
(def constrain f/constrain)

(def select f/select)
(def update f/update)
(def insert f/insert)
(def delete f/delete)

;; Relational Macros

(defn- type->bindings [prefix type]
  (reduce #(assoc %1 (symbol (str prefix (name %2))) %2) {} (keys type)))

(defn- symbol?->typekey [s ns]
  (and (symbol? s)
       (if ns (= (namespace s) ns) true)
       (keyword (name s))))

(defn- ensure-typekey [k type]
  (if (type k) k nil))

(defn walk-typekey->syms [form ns type]
  (walk/postwalk #(if-let [k (some-> %
                                     (symbol?->typekey ns)
                                     (ensure-typekey type))]
                    `(~k ~'fyra-rel-data)
                    %)
                 form))

(defn- form->fn [rel ns form]
  (let [type (eval `(f/reltype ~rel))
        form- (walk-typekey->syms form ns type)]
    `(fn [~'fyra-rel-data] ~form-)))

(defn- rel-binding-form [rbf]
  (cond
    (symbol? rbf) [rbf nil]
    (and (vector? rbf)
         (= 3 (count rbf))
         (= (second rbf) :as)) [(first rbf) (name (last rbf))]
    :else (assert false (str "Cannot recognize relation binding form"
                             (pr-str rbf)))))

(defmacro extend
  "extend the relation with more fields.
  map over items, adding fields by calling
  each of exts values as a function of the item

  example: (extend rel :area (* width height))"
  [rel & {:as exts}]
  (let [[rel ns] (rel-binding-form rel)]
    `(r/extend ~rel
       ~(reduce-kv #(assoc %1 %2 [(first %3) (form->fn rel ns (second %3))])
                   {} exts))))

(defmacro restrict
  "restrict the relation by a condition
  equivalent to a filter on the items

  example: (restrict rel (> area 10))"
  [rel form]
  (let [[rel ns] (rel-binding-form rel)]
    `(r/restrict ~rel ~(form->fn rel ns form))))

(def project r/project)
(def project-away r/project-away)
(def summarize r/summarize)
(def join r/join)
(def minus r/minus)
(def sum-key r/sum-key)
(def maximum-key r/maximum-key)
