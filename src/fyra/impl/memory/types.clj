(ns fyra.impl.memory.types
  (:require [clojure.core.typed :as t]
            [clojure.set :as set]
            [fyra.types :as ft]
            [fyra.impl.memory.util :refer [map-values]]))

(t/defalias StoredData (t/Map ft/CandidateKeys ft/Data))
(t/defalias TypeFn (t/Fn [ft/RelType -> ft/RelType]))

(t/defprotocol IRelation
  (reltype [this] :- ft/RelType))
(t/defprotocol IInsertable
  (conj! [this items :- (t/Coll Tuple)] :- ft/Data))
(t/defprotocol IDeletable
  (disj! [this items :- (t/Coll Tuple)] :- ft/Data)
  (del! [this] :- nil))
(t/defprotocol IUpdatable
  (update! [this f :- UpdateFn] :- ft/Data))
(t/defprotocol IJoinable
  (syms [this] :- (t/Vec t/Sym))
  (foreign-keys [this rel :- IJoinable] :- (t/U ft/ForeignKeys nil)))

(t/defn updatable? [rel :- IRelation] (instance? IUpdatable rel))

(t/defn validate-type [type :- ft/RelType
                       item :- ft/Tuple]
  (and (= (set (keys type)) (set (keys item)))
       (every? (fn [[k v]] (instance? (get type k) v)) item)))

(t/defn candidate [item :- ft/Tuple
                   ck :- ft/CandidateKeys]
  (if (empty? ck) item (select-keys item ck)))

(t/defn relvar-conj [relvar :- RelVar
                     data :- StoredData
                     item :- ft/Tuple]
  (let [{:keys [type candidates]} relvar]
    (assert (validate-type type item) (str "Item does not match type: " (pr-str type)))
    (run! (fn [ck] (assert (nil? (get-in data [ck (candidate item ck)]))
                           (str "Item is not unique under candidate: " (pr-str ck))))
          candidates)
    (reduce (fn [data ck] (update data ck #(set (conj %1 %2))
                                                (candidate item ck)))
            data candidates)))

(t/defn relvar-disj [relvar :- RelVar
                     data :- StoredData
                     item :- Tuple]
  (let [{:keys [candidates]} relvar]
    (reduce (fn [data ck] (update data ck #(set (disj %1 %2))
                                                (candidate item ck)))
            data candidates)))

(t/defn foreign-keys* [rel1 :- IJoinable rel2 :- IJoinable]
  (or (foreign-keys rel1 rel2)
      (if-let [fk (foreign-keys rel2 rel1)] (set/map-invert fk))))

(t/defn foreign-keys-check-type [type rel1 rel2]
  (let [fk (foreign-keys rel1 rel2)]
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
  IRelation
  (reltype [this] type)

  IDeref
  (-deref [this] (get @data []))

  IReset
  (-reset [this new-data]
    (reset! @adata new-data))

  IInsertable
  (conj! [this items]
    (-reset this (reduce (partial relvar-conj this) @adata items)))
  IDeletable
  (disj! [this items]
    (-reset this (reduce (partial relvar-disj) this) @adata items))
  (del! [this] (disj! this @this))
  IUpdatable
  (update! [this f]
    (-reset this (reduce (partial relvar-conj this) {} (map f @this))))

  IJoinable
  (syms [this] [(symbol name)])
  (foreign-keys [this rel]
    (apply concat (map foreign (syms rel))))

  IMeta
  (-meta [_] {:name name
              :type type
              :candidate candidate
              :foreign foreign})

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer (format "#<RelVar %s: " name))
    (pr-writer type writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval]
    (-notify-watches adata oldval newval))
  (-add-watch [this key f]
    (-add-watch adata key f))
  (-remove-watch [this key]
    (-remove-watch adata key))

  IHash
  (-hash [this] (hash this)))


(t/ann-datatype UpdateMapFilterRelation
  [filtf :- ft/FilterFn
   mapf :- ft/UpdateFn
   typef :- TypeFn
   rel :- (t/I IRelation IUpdatable IJoinable IDeref IMeta IPrintWithWriter IWatchable)])
(deftype UpdateMapFilterRelation [filtf mapf typef rel]
  IRelation
  (reltype [this] (typef (reltype rel)))

  IDeref
  (-deref [this] (set (map mapf (filter filtf @rel))))

  IUpdatable
  (update! [this f]
    (update! relvar #(if (filtf %) (g %) %)))
  IDeletable
  (disj! [this items]
    (assert (= (reltype this) (reltype rel)) "Cannot delete an item who's type is not base")
    (disj! rel items))
  (del! [this] (disj! this @this))

  IJoinable
  (syms [this] (syms rel))
  (foreign-keys [this rel2] (foreign-keys-check-type (reltype this) rel rel2))

  IMeta
  (-meta [_] (assoc :view (meta rel)
                    :updatable true))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<UpdateMapFilterRelation: ")
    (pr-writer rel writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval] nil)
  (-add-watch [this key f]
    (-add-watch rel (str key "-UMFRel:" (hash this))
      (fn [_ _ old new]
        (f this key (set (map mapf (filter filtf old)))
                    (set (map mapf (filter filtf new)))))))
  (-remove-watch [this key]
    (-remove-watch rel (str key "-UMFRel:" (hash this))))

  IHash
  (-hash [this] (hash this)))

(t/ann-datatype MapFilterRelation
  [filtf :- ft/FilterFn
   mapf :- ft/UpdateFn
   typef :- TypeFn
   rel :- (t/I IRelation IJoinable IDeref IMeta IPrintWithWriter IWatchable)])
(deftype MapFilterRelation [filtf mapf typef rel]
  IRelation
  (reltype [this] (typef (reltype rel)))

  IJoinable
  (syms [this] (syms rel))
  (foreign-keys [this rel2] (foreign-keys-check-type (reltype this) rel rel2))

  IDeref
  (-deref [this] (set (map mapf (filter filtf @rel))))

  IMeta
  (-meta [_] (assoc :view (meta rel)
                    :updatable false))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<MapFilterRelation: ")
    (pr-writer rel writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval] nil)
  (-add-watch [this key f]
    (-add-watch rel (str key "-MFRel:" (hash this))
      (fn [_ _ old new]
        (f this key (set (map mapf (filter filtf old)))
                    (set (map mapf (filter filtf new)))))))
  (-remove-watch [this key]
    (-remove-watch rel (str key "-MFRel:" (hash this))))

  IHash
  (-hash [this] (hash this)))

(t/ann-datatype SubtractedRelation
  [x :- (t/I IRelation IJoinable IDeref IMeta IPrintWithWriter IWatchable)
   y :- (t/I IRelation IJoinable IDeref IMeta IPrintWithWriter IWatchable)])
(deftype SubtractedRelation [x y]
  IRelation
  (reltype [this]
    (assert (= (reltype x) (reltype y))
            (format "Cannot subtract two different typed relations: %s %s"
                    (pr-str (reltype x))
                    (pr-str (reltype y))))
    (reltype x))

  IJoinable
  (syms [this] (syms rel))
  (foreign-keys [this rel2]
    (or (foreign-keys x rel2)
        (foreign-keys y rel2)))

  IDeref
  (-deref [this] (set/difference @x @y))

  IMeta
  (-meta [_] (assoc :views [(meta x) (meta y)]
                    :updatable false))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<SubtractedRelation: ")
    (pr-writer x writer opts)
    (-write writer " - ")
    (pr-writer y writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval] nil)
  (-add-watch [this key f]
    (-add-watch x (str key "-SubtrRelx:" (hash this))
      (fn [_ _ old new]
        (f this key (set/difference old @y)
                    (set/difference new @y))))
    (-add-watch y (str key "-SubtrRely:" (hash this))
      (fn [_ _ old new]
        (f this key (set/difference @x old)
                    (set/difference @x new)))))
  (-remove-watch [this key f]
    (-remove-watch x (str key "-SubtrRelx:" (hash this)))
    (-remove-watch y (str key "-SubtrRely:" (hash this))))

  IHash
  (-hash [this] (hash this)))


(t/ann-datatype JoinedRelation
  [x :- (t/I IRelation IJoinable IDeref IMeta IPrintWithWriter IWatchable)
   y :- (t/I IRelation IJoinable IDeref IMeta IPrintWithWriter IWatchable)])
(deftype JoinedRelation [x y]
  IRelation
  (reltype [this]
    (let [rx (reltype x)
          ry (reltype y)]
      (assert (every? #(= (get rx %) (get ry %))
                      (set/difference (set (keys rx)) (set (keys ry))))
              (format "Relations do not agree on types on shared keys: %s %s"
                      (pr-str rx) (pr-str ry)))
      (merge rx ry)))

  IJoinable
  (syms [this] (concat (syms x) (syms y)))
  (foreign-keys [this rel2]
    (concat (foreign-keys x rel2) (foreign-keys y rel2)))

  IDeref
  (-deref [this]
    (set/join @x @y (foreign-keys* x y)))

  IMeta
  (-meta [_] (assoc :views [(meta x) (meta y)]
                    :updatable false))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<JoinedRelation: ")
    (pr-writer x writer opts)
    (-write writer " - ")
    (pr-writer y writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval] nil)
  (-add-watch [this key f]
    (-add-watch x (str key "-JoinRelx:" (hash this))
      (fn [_ _ old new]
        (let [fk (foreign-keys* x y)]
          (f this key (set/join old @y fk) (set/join new @y fk)))))
    (-add-watch y (str key "-JoinRely:" (hash this))
      (fn [_ _ old new]
        (let [fk (foreign-keys* x y)]
          (f this key (set/join @x old fk) (set/join @x new fk))))))
  (-remove-watch [this key f]
    (-remove-watch x (str key "-JoinRelx:" (hash this)))
    (-remove-watch y (str key "-JoinRely:" (hash this))))

  IHash
  (-hash [this] (hash this)))

(defn aggregate [data fields extf]
  (->> data
       (reduce #(update-in %1 [(select-keys %2 fields)] conj %2) {})
       (map (fn [[km grp]] (merge km (map-values #(% grp) extf))))
       set))

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
   rel :- (t/I IRelation IJoinable IDeref IMeta IPrintWithWriter IWatchable)])
(deftype AggregatedRelation [fields exttypes extf rel]
  IRelation
  (reltype [this] (merge (select-keys (reltype rel) fields) exttypes))

  IJoinable
  (syms [this] (syms rel))
  (foreign-keys [this rel2] (foreign-keys-check-type (reltype this) rel rel2))

  IDeref
  (-deref [this] (aggregate @rel fields extf))

  IMeta
  (-meta [_] (assoc :view (meta rel)
                    :updatable false))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<AggregatedRelation: ")
    (pr-writer rel writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval] nil)
  (-add-watch [this key f]
    (-add-watch rel (str key "-AggRel:" (hash this))
      (fn [_ _ old new]
        (f this key (aggregate old fields extf) (aggregate new fields extf)))))
  (-remove-watch [this key]
    (-remove-watch rel (str key "-AggRel:" (hash this))))

  IHash
  (-hash [this] (hash this)))

(t/ann-datatype AggReducedRelation
  [fields :- (t/Vec t/Kw)
   redf :- (t/Fn [(t/Coll Tuple) -> (t/Coll Tuple)])
   rel :- (t/I IRelation IJoinable IDeref IMeta IPrintWithWriter IWatchable)])
(deftype AggReducedRelation [fields redf rel]
  IRelation
  (reltype [this] (reltype rel))

  IJoinable
  (syms [this] (syms rel))
  (foreign-keys [this rel2] (foreign-keys rel rel2))

  IDeref
  (-deref [this] (agg-reduce @rel fields redf))

  IMeta
  (-meta [_] (assoc :view (meta rel)
                    :updatable false))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<AggReducedRelation: ")
    (pr-writer rel writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval] nil)
  (-add-watch [this key f]
    (-add-watch rel (str key "-AggRel:" (hash this))
      (fn [_ _ old new]
        (f this key (agg-reduce old fields redf)
                    (agg-reduce new fields redf)))))
  (-remove-watch [this key]
    (-remove-watch rel (str key "-AggRel:" (hash this))))

  IHash
  (-hash [this] (hash this)))