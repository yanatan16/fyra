(ns fyra.impl.memory.types
  (:refer-clojure :exclude [update format])
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [schema.core :as s]
            [fyra.impl.memory.util :refer [map-values format]]))


(defprotocol WrappedRelation
  (unwrap-rel [this] "Get the underlying relation from a reify'd type"))

(defprotocol Relation
  (relschema [this])
  (exec [this db])
  (syms [this])
  (foreign-keys [this rel]))
(defprotocol UpdatableRelation
  (insert [this db items])
  (update [this db f])
  (del-items [this db items])
  (del [this db ]))
(defprotocol Observable
  (notify-observers [this kind  olddb  newdb])
  (add-observer [this kind  key f])
  (remove-observer [this kind  key]))

(defn validate-schema [schema item]
  (not (s/check schema item)))

(defn candidate [item ck]
  (if (empty? ck) item (select-keys item ck)))

(defn assert-schema [schema items]
  (if-not (every? (partial validate-schema schema) items)
    (throw (ex-info "Item does not match schema" {:schema schema})))
  items)

(defn relvar-conj [candidates data item]
  (run! (fn [ck] (if-not (nil? (get-in data [ck (candidate item ck)]))
                    (throw (ex-info "Item is not unique under candidate" {:candidate ck}))))
        candidates)
  (reduce (fn [data ck] (clojure.core/update
                         data ck #(set (conj %1 %2))
                         (candidate item ck)))
          data candidates))

(defn relvar-disj [candidates data item]
  (reduce (fn [data ck] (clojure.core/update
                         data ck #(set (disj %1 %2))
                         (candidate item ck)))
          data candidates))

(defn foreign-keys* [rel1 rel2]
  (or (not-empty (foreign-keys rel1 rel2))
      (set/map-invert (foreign-keys rel2 rel1))))

(defn foreign-keys-check-schema [schema rel1 rel2]
  (let [fk (foreign-keys* rel1 rel2)]
    (assert (every? schema fk)
            (format "Cannot join because foreign keys have been removed: %s"
                    (pr-str schema)))
    fk))

(defn relvar-all [data] (or (get data []) #{}))

(deftype RelVar [name schema candidates foreign aobv]
  Relation
  (relschema [this] schema)
  (exec [this db] (relvar-all (get db name)))
  (syms [this] [(symbol name)])
  (foreign-keys [this rel]
    (if foreign (reduce #(merge %1 %2) {} (map foreign (syms rel))) {}))

  UpdatableRelation
  (insert [this db items]
    (assert-schema schema items)
    (update-in db [name]
               (partial  reduce (partial relvar-conj candidates))
               items))
  (del-items [this db items]
    (update-in db [name]
               (partial reduce (partial relvar-disj candidates))
               items))
  (del [this db]
    (assoc db name {}))
  (update [this db f]
    (update-in db [name] #(->> (or (get % []) #{})
                               (map f)
                               (assert-schema schema)
                               (reduce (partial relvar-conj candidates) {}))))

  Observable
  (notify-observers [this kind olddb newdb]
    (let [old (exec this olddb)
          new (exec this newdb)
          watchers (get @aobv kind)]
      (run! #(% old new [olddb newdb]) (vals watchers))))
  (add-observer [this kind key f]
    (swap! aobv assoc-in [kind key] f))
  (remove-observer [this kind key]
    (swap! aobv update-in [kind] dissoc key)))

(defn munge-candidates [cks]
  (cond (and (vector? cks) (vector? cks)) (-> cks set (conj []) vec)
        (and (vector? cks) (not-empty cks)) [[] cks]
        :else [[]]))

(defn make-relvar [{:keys [name schema candidates foreign]}]
  (->RelVar name schema (munge-candidates candidates) foreign (atom {})))

(deftype UpdatableDerivedRelation [execf rel]
  Relation
  (relschema [this] (relschema rel))
  (exec [this db] (execf (exec rel db)))
  (syms [this] (syms rel))
  (foreign-keys [this rel2] (foreign-keys rel rel2))

  UpdatableRelation
  (insert [this db items]
    (insert rel db items))
  (del-items [this db items]
    (del-items rel db items))
  (del [this db] (del-items this db (exec this db)))
  (update [this db f]
    (update rel db #(if (not-empty (execf #{%})) (f %) %)))

  Observable
  (notify-observers [this kind olddb newdb]
    (notify-observers rel kind olddb newdb))
  (add-observer [this kind key f]
    (add-observer
     rel kind  (str key "-UDF:" (hash this))
     (fn [old new dbs]
       (let [old (execf old)
             new (execf new)]
         (if (not= old new)
           (f old new dbs))))))
  (remove-observer [this kind key]
    (remove-observer rel kind (str key "-UDF:" (hash this)))))

(defn collate-data* [rels db data i]
  (map-indexed #(if (= %1 i) data (exec %2 db)) rels))

(deftype DerivedRelation [execf schema rels]
  Relation
  (relschema [this] schema)
  (exec [this db] (apply execf (map #(exec % db) rels)))
  (syms [this] (apply concat (map syms rels)))
  (foreign-keys [this rel2]
    (apply merge (map #(foreign-keys-check-schema (relschema this) % rel2) rels)))

  Observable
  (notify-observers [this kind olddb newdb]
    (run! #(notify-observers % kind olddb newdb) rels))
  (add-observer [this kind key f]
    (run! (fn [[i rel]]
            (add-observer
             rel kind (str key "-DF:" (hash this))
             (fn [old new [olddb newdb]]
               (let [old (apply execf (collate-data* rels olddb old i))
                     new (apply execf (collate-data* rels newdb new i))]
                 (if (not= old new)
                   (f old new [olddb newdb]))))))
          (map-indexed vector rels)))
  (remove-observer [this kind key]
    (run! #(remove-observer % kind (str key "-UDF:" (hash this))) rels)))
