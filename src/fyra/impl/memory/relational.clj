(ns fyra.impl.memory.relational
  (:refer-clojure :exclude [extend])
  (:require [clojure.set :as set]
            [fyra.impl.memory.types :as memt]
            [fyra.impl.memory.util :refer [map-values]]))

(defn- type-of-fn [f]
  (if (vector? f) (first f)
      (let [rt (:tag (meta f))]
        (assert rt (str "Can't determine the return type of " f))
        rt)))

(defn- setmap [f x] (set (map f x)))
(defn- setfilter [f x] (set (filter f x)))

(defn project [rel & ks]
  (memt/->DerivedRelation (fn [data] (setmap #(select-keys % ks) data))
                          (select-keys (memt/reltype rel) ks)
                          [rel]))

(defn project-away [rel & ks]
  (memt/->DerivedRelation (fn [data] (setmap #(apply dissoc % ks) data))
                          (apply dissoc (memt/reltype rel) ks)
                          [rel]))

(defn extend [rel {:as exts}]
  (let [mapf (fn [item]
               (merge item
                      (map-values #((if (fn? %) (% item) (second %)) item)
                                  exts)))]
    (memt/->DerivedRelation
     #(setmap mapf %)
     (merge (memt/reltype rel) (map-values type-of-fn exts))
     [rel])))

(defn restrict [rel f]
  (if (satisfies? memt/UpdatableRelation rel)
    (memt/->UpdatableDerivedRelation #(setfilter f %) rel)
    (memt/->DerivedRelation #(setfilter f %) (memt/reltype rel) [rel])))

(defn join [rel-1 rel-2]
  (let [fk (memt/foreign-keys* rel-1 rel-2)]
    (memt/->DerivedRelation
     #(set/join %1 %2 fk)
     (let [rx (memt/reltype rel-1) ry (memt/reltype rel-2)]
       (assert (every? #(= (get rx %) (get ry %))
                       (set/intersection (set (keys rx)) (set (keys ry))))
               (format "Cannot join relations: do not agree on shared key types: %s %s"
                       (pr-str rx) (pr-str ry)))
       (merge rx ry))
     [rel-1 rel-2])))

(defn minus [rel-1 rel-2]
  (let [rx (memt/reltype rel-1) ry (memt/reltype rel-2)]
    (assert (= rx ry)
            (format "Cannot subtract two differently typed relations: %s %s"
                   (pr-str rx) (pr-str ry))))
  (memt/->DerivedRelation set/difference (fn [rx ry] rx) [rel-1 rel-2]))

(defn- aggregate [data fields extf]
  (->> data
       (reduce #(update-in %1 [(select-keys %2 fields)] conj %2) {})
       (map (fn [[km grp]] (merge km (map-values #(% grp) extf))))
       set))

(defn- agg-reduce [data fields redf]
  (->> data
       (reduce #(update-in %1 [(select-keys %2 fields)] conj %2) {})
       (map (fn [[km grp]] (redf grp)))
       (apply concat)
       set))

(defn summarize [rel ks acc]
  (if (fn? acc)
    (memt/->DerivedRelation #(agg-reduce % ks acc)
                            (memt/reltype rel)
                            [rel])
    (memt/->DerivedRelation #(aggregate % ks acc)
                            (->> acc
                                 (map-values type-of-fn)
                                 (merge (select-keys (memt/reltype rel) ks)))
                            [rel])))
