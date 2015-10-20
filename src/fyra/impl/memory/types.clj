(ns fyra.impl.memory.types
  (:require [clojure.core.typed :as t]
            [clojure.set :as set]
            [fyra.types :as ft]
            [fyra.impl.memory.util :refer [map-values]])
  (:import [clojure.lang IMeta IDeref IAtom IRef]))

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
  (reduce (fn [data ck] (update data ck #(set (conj %1 %2))
                                              (candidate item ck)))
          data candidates))

(t/ann relvar-disj [ft/CandidateKeysList ft/StoredData ft/Tuple -> ft/Data])
(defn relvar-disj [candidates data item]
  (reduce (fn [data ck] (update data ck #(set (disj %1 %2))
                                              (candidate item ck)))
          data candidates))

(t/ann foreign-keys* [ft/IJoinable ft/IJoinable -> (t/U nil ft/ForeignKeys)])
(defn foreign-keys* [rel1 rel2]
  (or (not-empty (ft/foreign-keys rel1 rel2))
      (set/map-invert (ft/foreign-keys rel2 rel1))))

(t/ann foreign-keys-check-type
  [ft/RelType ft/IJoinable ft/IJoinable -> (t/U nil ft/ForeignKeys)])
(t/defn foreign-keys-check-type [type rel1 rel2]
  (let [fk (foreign-keys* rel1 rel2)]
    (assert (every? type fk)
            (format "Cannot join because foreign keys have been removed: %s"
                    (pr-str type)))
    fk))

(t/ann-datatype RelVar [name :- String
                        type :- ft/RelType
                        candidates :- ft/CandidateKeysList
                        foreign :- ft/ForeignKeysMap
                        adata :- (t/Atom1 (t/Map ft/CandidateKeys ft/Data))])
(deftype RelVar [name type candidates foreign adata]
  ft/IRelation
  (reltype [this] type)

  IDeref
  (deref [this] (get @adata []))

  IAtom
  (reset [this new-data] (reset! adata new-data))
  (swap [this f] (swap! adata f))
  (swap [this f x] (swap! adata f x))
  (swap [this f x y] (swap! adata f x y))
  (swap [this f x y more] (apply swap! adata f x y more))
  (compareAndSet [this old new] (compare-and-set! adata old new))

  ft/IInsertable
  (conj! [this items]
    (apply assert-type type items)
    (reset! this (reduce (partial relvar-conj candidates) @adata items)))
  ft/IDeletable
  (del-items! [this items]
    (reset! this (reduce (partial relvar-disj candidates) @adata items)))
  (del! [this] (ft/del-items! this @this))
  ft/IUpdatable
  (update! [this f]
    (reset! this (reduce (partial relvar-conj candidates) {} (map f @this))))

  ft/IJoinable
  (syms [this] [(symbol name)])
  (foreign-keys [this rel]
    (if foreign (reduce #(merge %1 %2) {} (map foreign (ft/syms rel))) {}))

  IMeta
  (meta [_] {:name name
              :type type
              :candidate candidates
              :foreign foreign})

  IRef
  (setValidator [this f] (set-validator! adata f))
  (getValidator [this] (get-validator adata))
  (getWatches [this] (.getWatches adata))
  (addWatch [this key f] (add-watch adata key f))
  (removeWatch [this key] (remove-watch adata key)))

(defn munge-candidates [cks]
  (cond (and (vector? cks) (vector? cks)) (-> cks set (conj []) vec)
        (and (vector? cks) (not-empty cks)) [[] cks]
        :else [[]]))

(defn make-relvar [{:keys [name type candidates foreign]}]
  (->RelVar name type (munge-candidates candidates) foreign (atom {})))

(t/ann-datatype UpdateMapFilterRelation
  [filtf :- ft/FilterFn
   mapf :- ft/UpdateFn
   typef :- TypeFn
   rel :- ft/URelation])
(deftype UpdateMapFilterRelation [filtf mapf typef rel]
  ft/IRelation
  (reltype [this] (typef (ft/reltype rel)))

  IDeref
  (deref [this] (set (map mapf (filter filtf @rel))))

  ft/IUpdatable
  (update! [this g]
    (ft/update! rel #(if (filtf %) (g %) %)))
  ft/IDeletable
  (del-items! [this items]
    (assert (= (ft/reltype this) (ft/reltype rel)) "Cannot delete an item who's type is not base")
    (ft/del-items! rel items))
  (del! [this] (ft/del-items! this @this))

  ft/IJoinable
  (syms [this] (ft/syms rel))
  (foreign-keys [this rel2] (foreign-keys-check-type (ft/reltype this) rel rel2))

  IMeta
  (meta [_] {:view (meta rel)
             :updatable true})

  IRef
  (setValidator [this f] (set-validator! rel f))
  (getValidator [this] (get-validator rel))
  (getWatches [this] (.getWatches rel))
  (addWatch [this key f]
    (add-watch rel (str key "-UMFRel:" (hash this))
      (fn [_ _ old new]
        (f this key (set (map mapf (filter filtf old)))
                    (set (map mapf (filter filtf new)))))))
  (removeWatch [this key]
    (remove-watch rel (str key "-UMFRel:" (hash this)))))

(t/ann-datatype MapFilterRelation
  [filtf :- ft/FilterFn
   mapf :- ft/UpdateFn
   typef :- TypeFn
   rel :- ft/Relation])
(deftype MapFilterRelation [filtf mapf typef rel]
  ft/IRelation
  (reltype [this] (typef (ft/reltype rel)))

  ft/IJoinable
  (syms [this] (ft/syms rel))
  (foreign-keys [this rel2] (foreign-keys-check-type (ft/reltype this) rel rel2))

  IDeref
  (deref [this] (set (map mapf (filter filtf @rel))))

  IMeta
  (meta [_] {:view (meta rel)
             :updatable false})

  IRef
  (setValidator [this f] (set-validator! rel f))
  (getValidator [this] (get-validator rel))
  (getWatches [this] (.getWatches rel))
  (addWatch [this key f]
    (add-watch rel (str key "-MFRel:" (hash this))
      (fn [_ _ old new]
        (f this key (set (map mapf (filter filtf old)))
                    (set (map mapf (filter filtf new)))))))
  (removeWatch [this key]
    (remove-watch rel (str key "-MFRel:" (hash this)))))

(t/ann-datatype SubtractedRelation
  [x :- ft/Relation
   y :- ft/Relation])
(deftype SubtractedRelation [x y]
  ft/IRelation
  (reltype [this]
    (assert (= (ft/reltype x) (ft/reltype y))
            (format "Cannot subtract two different typed relations: %s %s"
                    (pr-str (ft/reltype x))
                    (pr-str (ft/reltype y))))
    (ft/reltype x))

  ft/IJoinable
  (syms [this] (concat (ft/syms x) (ft/syms y)))
  (foreign-keys [this rel2]
    (or (ft/foreign-keys x rel2)
        (ft/foreign-keys y rel2)))

  IDeref
  (deref [this] (set/difference @x @y))

  IMeta
  (meta [_] {:views [(meta x) (meta y)]
             :updatable false})

  IRef
  (setValidator [this f] (set-validator! x f) (set-validator! y f))
  (getValidator [this] (get-validator x))
  (getWatches [this] (merge (.getWatches x) (.getWatches y)))
  (addWatch [this key f]
    (add-watch x (str key "-SubtrRelx:" (hash this))
      (fn [_ _ old new]
        (f this key (set/difference old @y)
                    (set/difference new @y))))
    (add-watch y (str key "-SubtrRely:" (hash this))
      (fn [_ _ old new]
        (f this key (set/difference @x old)
                    (set/difference @x new)))))
  (removeWatch [this key]
    (remove-watch x (str key "-SubtrRelx:" (hash this)))
    (remove-watch y (str key "-SubtrRely:" (hash this)))))


(t/ann-datatype JoinedRelation
  [x :- ft/Relation
   y :- ft/Relation])
(deftype JoinedRelation [x y]
  ft/IRelation
  (reltype [this]
    (let [rx (ft/reltype x)
          ry (ft/reltype y)]
      (assert (every? #(= (get rx %) (get ry %))
                      (set/difference (set (keys rx)) (set (keys ry))))
              (format "Relations do not agree on types on shared keys: %s %s"
                      (pr-str rx) (pr-str ry)))
      (merge rx ry)))

  ft/IJoinable
  (syms [this] (concat (ft/syms x) (ft/syms y)))
  (foreign-keys [this rel2]
    (concat (ft/foreign-keys x rel2) (ft/foreign-keys y rel2)))

  IDeref
  (deref [this]
    (set/join @x @y (foreign-keys* x y)))

  IMeta
  (meta [_] {:views [(meta x) (meta y)]
             :updatable false})

  IRef
  (setValidator [this f] (set-validator! x f) (set-validator! y f))
  (getValidator [this] (get-validator x))
  (getWatches [this] (merge (.getWatches x) (.getWatches y)))
  (addWatch [this key f]
    (add-watch x (str key "-JoinRelx:" (hash this))
      (fn [_ _ old new]
        (let [fk (foreign-keys* x y)]
          (f this key (set/join old @y fk) (set/join new @y fk)))))
    (add-watch y (str key "-JoinRely:" (hash this))
      (fn [_ _ old new]
        (let [fk (foreign-keys* x y)]
          (f this key (set/join @x old fk) (set/join @x new fk))))))
  (removeWatch [this key]
    (remove-watch x (str key "-JoinRelx:" (hash this)))
    (remove-watch y (str key "-JoinRely:" (hash this)))))

(t/ann aggregate [ft/Data (t/Vec t/Kw) (t/Map t/Kw (t/Fn [(t/Coll ft/Tuple) -> Any]))
                  -> ft/Data])
(defn aggregate [data fields extf]
  (->> data
       (reduce #(update-in %1 [(select-keys %2 fields)] conj %2) {})
       (map (fn [[km grp]] (merge km (map-values #(% grp) extf))))
       set))

(t/ann agg-reduce [ft/Data (t/Vec t/Kw) (t/Fn [(t/Coll Tuple) -> (t/Coll Tuple)])
                   -> ft/Data])
(defn agg-reduce [data fields redf]
  (->> data
       (reduce #(update-in %1 [(select-keys %2 fields)] conj %2) {})
       (map (fn [[km grp]] (redf grp)))
       (apply concat)
       set))

(t/ann-datatype AggregatedRelation
  [fields :- (t/Vec t/Kw)
   exttypes :- (t/Map t/Kw t/Type)
   extf :- (t/Map t/Kw (t/Fn [(t/Coll Tuple) -> Any]))
   rel :- ft/Relation])
(deftype AggregatedRelation [fields exttypes extf rel]
  ft/IRelation
  (reltype [this] (merge (select-keys (ft/reltype rel) fields) exttypes))

  ft/IJoinable
  (syms [this] (ft/syms rel))
  (foreign-keys [this rel2] (foreign-keys-check-type (ft/reltype this) rel rel2))

  IDeref
  (deref [this] (aggregate @rel fields extf))

  IMeta
  (meta [_] {:view (meta rel)
             :updatable false})

  IRef
  (setValidator [this f] (set-validator! rel f))
  (getValidator [this] (get-validator rel))
  (getWatches [this] (.getWatches rel))
  (addWatch [this key f]
    (add-watch rel (str key "-AggRel:" (hash this))
      (fn [_ _ old new]
        (f this key (aggregate old fields extf) (aggregate new fields extf)))))
  (removeWatch [this key]
    (remove-watch rel (str key "-AggRel:" (hash this)))))

(t/ann-datatype AggReducedRelation
  [fields :- (t/Vec t/Kw)
   redf :- (t/Fn [(t/Coll Tuple) -> (t/Coll Tuple)])
   rel :- ft/Relation])
(deftype AggReducedRelation [fields redf rel]
  ft/IRelation
  (reltype [this] (ft/reltype rel))

  ft/IJoinable
  (syms [this] (ft/syms rel))
  (foreign-keys [this rel2] (ft/foreign-keys rel rel2))

  IDeref
  (deref [this] (agg-reduce @rel fields redf))

  IMeta
  (meta [_] {:view (meta rel)
             :updatable false})

  IRef
  (setValidator [this f] (set-validator! rel f))
  (getValidator [this] (get-validator rel))
  (getWatches [this] (.getWatches rel))
  (addWatch [this key f]
    (add-watch rel (str key "-AggRel:" (hash this))
      (fn [_ _ old new]
        (f this key (agg-reduce old fields redf)
                    (agg-reduce new fields redf)))))
  (removeWatch [this key]
    (remove-watch rel (str key "-AggRel:" (hash this)))))
