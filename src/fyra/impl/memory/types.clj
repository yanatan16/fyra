(ns fyra.impl.memory.types
  (:refer-clojure :exclude [update])
  (:require [clojure.core.typed :as t]
            [clojure.set :as set]
            [clojure.string :as string]
            [fyra.types :as ft]
            [fyra.impl.memory.util :refer [map-values]])
  (:import [clojure.lang IMeta IDeref IAtom IRef]))

(t/defalias Db (t/Map t/String (t/Map ft/CandidateKeys ft/Data)))
(t/defalias DbObserverFn [ft/Data ft/Data (t/Vector Db) -> t/Any])
(t/defprotocol Relation
  (reltype [this] :- ft/RelType)
  (exec [this db :- Db] :- ft/Data)
  (syms [this] :- (t/Vec t/Sym))
  (foreign-keys [this rel :- Relation] :- ft/ForeignKeys))
(t/defprotocol UpdatableRelation
  (insert [this db :- Db items :- (t/Coll ft/Tuple)] :- Db)
  (update [this db :- Db f :- [ft/Tuple -> ft/Tuple]] :- Db)
  (del-items [this db :- Db items :- (t/Coll ft/Tuple)] :- Db)
  (del [this db :- Db] :- Db))
(t/defprotocol Observable
  (notify-observers [this kind :- t/Kw olddb :- Db newdb :- Db])
  (add-observer [this kind :- t/Kw key f :- DbObserverFn])
  (remove-observer [this kind :- t/Kw key]))

(t/ann validate-type [ft/RelType ft/Tuple -> Boolean])
(defn validate-type [type item]
  (and (= (set (keys type)) (set (keys item)))
       (every? (fn [[k v]] (instance? (get type k) v)) item)))

(t/ann candidate [ft/Tuple ft/CandidateKeys -> ft/Tuple])
(defn candidate [item ck]
  (if (empty? ck) item (select-keys item ck)))

(t/ann assert-type [ft/RelType ft/Tuple * -> Boolean])
(defn assert-type [type & items]
  (if-not (every? (partial validate-type type) items)
    (throw (ex-info "Item does not match type" {:type type}))))

(t/ann relvar-conj [ft/CandidateKeysList ft/StoredData ft/Tuple -> ft/Data])
(defn relvar-conj [candidates data item]
  (run! (fn [ck] (if-not (nil? (get-in data [ck (candidate item ck)]))
                    (throw (ex-info "Item is not unique under candidate" {:candidate ck}))))
        candidates)
  (reduce (fn [data ck] (clojure.core/update
                         data ck #(set (conj %1 %2))
                         (candidate item ck)))
          data candidates))

(t/ann relvar-disj [ft/CandidateKeysList ft/StoredData ft/Tuple -> ft/Data])
(defn relvar-disj [candidates data item]
  (reduce (fn [data ck] (clojure.core/update
                         data ck #(set (disj %1 %2))
                         (candidate item ck)))
          data candidates))

(t/ann foreign-keys* [ft/IJoinable ft/IJoinable -> (t/U nil ft/ForeignKeys)])
(defn foreign-keys* [rel1 rel2]
  (or (not-empty (foreign-keys rel1 rel2))
      (set/map-invert (foreign-keys rel2 rel1))))

(t/ann foreign-keys-check-type
  [ft/RelType ft/IJoinable ft/IJoinable -> (t/U nil ft/ForeignKeys)])
(defn foreign-keys-check-type [type rel1 rel2]
  (let [fk (foreign-keys* rel1 rel2)]
    (assert (every? type fk)
            (format "Cannot join because foreign keys have been removed: %s"
                    (pr-str type)))
    fk))

(defn relvar-all [data] (or (get data []) #{}))

(t/ann-datatype RelVar [name :- String
                        type :- ft/RelType
                        candidates :- ft/CandidateKeysList
                        foreign :- ft/ForeignKeysMap
                        aobv :- (t/Atom1 (t/Map t/Any ft/DbObserverFn))])
(deftype RelVar [name type candidates foreign aobv]
  Relation
  (reltype [this] type)
  (exec [this db] (relvar-all (get db name)))
  (syms [this] [(symbol name)])
  (foreign-keys [this rel]
    (if foreign (reduce #(merge %1 %2) {} (map foreign (syms rel))) {}))

  UpdatableRelation
  (insert [this db items]
    (apply assert-type type items)
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
                               (reduce (partial relvar-conj candidates) {}))))

  IMeta
  (meta [_] {:name name
             :type type
             :candidate candidates
             :foreign foreign})

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

(defn make-relvar [{:keys [name type candidates foreign]}]
  (->RelVar name type (munge-candidates candidates) foreign (atom {})))

(deftype UpdatableDerivedRelation [execf rel]
  Relation
  (reltype [this] (reltype rel))
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

  IMeta
  (meta [_] {:view (meta rel)
             :updatable true})

  Observable
  (notify-observers [this kind olddb newdb]
    (notify-observers rel kind olddb newdb))
  (add-observer [this kind key f]
    (add-observer
     rel kind  (str key "-UDF:" (hash this))
     (fn [old new dbs] (f (execf old) (execf new) dbs))))
  (remove-observer [this kind key]
    (remove-observer rel kind (str key "-UDF:" (hash this)))))

(defn collate-data* [rels db data i]
  (map-indexed #(if (= %1 i) data (exec %2 db)) rels))

(deftype DerivedRelation [execf type rels]
  Relation
  (reltype [this] type)
  (exec [this db] (apply execf (map #(exec % db) rels)))
  (syms [this] (apply concat (map syms rels)))
  (foreign-keys [this rel2]
    (apply merge (map #(foreign-keys-check-type (reltype this) % rel2) rels)))

  IMeta (meta [_] {:views (map meta rels) :updatable false})

  Observable
  (notify-observers [this kind olddb newdb]
    (run! #(notify-observers % kind olddb newdb) rels))
  (add-observer [this kind key f]
    (run! (fn [[i rel]]
            (add-observer
             rel kind (str key "-DF:" (hash this))
             (fn [old new [olddb newdb]]
               (f (apply execf (collate-data* rels olddb old i))
                  (apply execf (collate-data* rels newdb new i))
                  [olddb newdb]))))
          (map-indexed vector rels)))
  (remove-observer [this kind key]
    (run! #(remove-observer % kind (str key "-UDF:" (hash this))) rels)))
