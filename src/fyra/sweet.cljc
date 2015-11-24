(ns fyra.sweet
  (:refer-clojure :exclude [update extend])
  (:require [clojure.walk :as walk]
            [fyra.core :as f]
            [fyra.relational :as r]
            [fyra.aggs :as a]))

;; fyra.core macros & helpers

(do
  #?@(:clj
      [
       (defn- resolve-def-args [[name db doc-string? & rest-args :as full]]
         (if (string? doc-string?)
           [name `(~doc-string?) db rest-args]
           [name `() db (nthrest full 2)]))

       (defmacro defrelvar
         "Define a relational variable
          Relvars create a stateful group of items
          which can be operated on using fyra.relational"
         [& args]
         (let [[name def-args db [s & {:as opts}]] (resolve-def-args args)]
           `(def ~name ~@def-args (f/relvar ~db ~(str name) ~s ~opts))))

       (defmacro defview
         "Create a derived relvar as a named view"
         [& args]
         (let [[name def-args db [rel & {:as opts}]] (resolve-def-args args)]
           `(def ~name ~@def-args (f/view ~db ~(str name) ~rel ~opts))))]))

;; fyra.core other exports

(defn- make-update-item-f [updates]
  #(reduce (fn [it [k v]] (if (fn? v) (clojure.core/update it k v)
                              (assoc it k v))) % updates))

(defn update
  "Update the items in relation rel with updts
   updts is a map of field keys to either constant values
   or functions of the old value to the new value"
  [db rel updts]
  (f/update db rel (make-update-item-f updts)))

(defn insert [db baserel & items] (f/insert db baserel items))

(def observe f/observe)
(def constrain f/constrain)

(def select f/select)
(def delete f/delete)

;; fyra.relational macros & helpers

(do
  #?@(:clj
      [
       (defn- type->bindings [prefix type]
         (reduce #(assoc %1 (symbol (str prefix (name %2))) %2) {} (keys type)))

       (defn- symbol?->typekey [s ns]
         (and (symbol? s)
              (if ns (= (namespace s) ns) true)
              (keyword (name s))))

       (defn- ensure-typekey [k type]
         (if (type k) k nil))

       (defn- walk-typekey->syms [form ns type]
         (walk/postwalk #(if-let [k (some-> %
                                            (symbol?->typekey ns)
                                            (ensure-typekey type))]
                           `(~k ~'fyra-rel-data)
                           %)
                        form))

       (defn- form->fn [rel ns form]
         (let [s (eval `(r/relschema ~rel))
               form- (walk-typekey->syms form ns s)]
           `(fn [~'fyra-rel-data] ~form-)))

       (defn- rel-binding-form [rbf]
         (cond
           (and (vector? rbf)
                (= 3 (count rbf))
                (= (second rbf) :as)) [(first rbf) (name (last rbf))]
           :else [rbf nil]
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
           `(r/restrict ~rel ~(form->fn rel ns form))))]))

;; fyra.relational other exports

(defn summarize
  "summarization of the tuple set.
   If op is a function, calls summarize-reduce
   If op is an aggregation, calls summarize-aggregate"
  [rel grp op]
  (if (fn? op)
    (r/summarize-reduce rel grp op)
    (r/summarize-aggregate rel grp op)))

(defn project [rel & ks] (r/project rel ks))
(defn project-away [rel & ks] (r/project-away rel ks))

(def join r/join)
(def minus r/minus)
(def relschema r/relschema)


(def sum-key a/sum-key)
(def maximum-key a/maximum-key)
