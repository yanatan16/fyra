(ns fyra.impl.memory.relational
  (:refer-clojure :exclude [extend])
  (:require [clojure.set :as set]
            [fyra.impl.memory.types :as memt]
            [fyra.impl.memory.util :refer [map-values]])
  (:import [fyra.relational FRPRelation]
           [fyra.impl.memory.types WrappedRelation]))

;; Some helpers

(defn- type-of-fn [f]
  (if (vector? f) (first f)
      (let [rt (:tag (meta f))]
        (assert rt (str "Can't determine the return type of " f))
        rt)))

(defn- setmap [f x] (set (map f x)))
(defn- setfilter [f x] (set (filter f x)))

(defn- make-extend-mapf [exts]
  (fn [item]
    (merge item
           (map-values #((second %) item) exts))))

(defn- aggregate [data fields extf]
  (->> data
       (reduce #(update-in %1 [(select-keys %2 fields)] conj %2) {})
       (map (fn [[km grp]] (merge km (map-values #((second %) grp) extf))))
       set))

(defn- agg-reduce [data fields redf]
  (->> data
       (reduce #(update-in %1 [(select-keys %2 fields)] conj %2) {})
       (map (fn [[km grp]] (redf grp)))
       (apply concat)
       set))

(defn reify-rel [rel]
  (reify FRPRelation
    (project [_ ks]
      (reify-rel
       (memt/->DerivedRelation
        (fn [data] (setmap #(select-keys % ks) data))
        (select-keys (memt/relschema rel) ks)
        [rel])))
    (project-away [_ ks]
      (reify-rel
       (memt/->DerivedRelation
        (fn [data] (setmap #(apply dissoc % ks) data))
        (apply dissoc (memt/relschema rel) ks)
        [rel])))
    (extend [_ {:as exts}]
      (reify-rel
       (memt/->DerivedRelation
        (let [mapf (make-extend-mapf exts)] #(setmap mapf %))
        (merge (memt/relschema rel) (map-values first exts))
        [rel])))
    (restrict [_ f]
      (reify-rel
       (if (satisfies? memt/UpdatableRelation rel)
         (memt/->UpdatableDerivedRelation #(setfilter f %) rel)
         (memt/->DerivedRelation #(setfilter f %) (memt/relschema rel) [rel]))))
    (summarize-reduce [_ grp f]
      (reify-rel
       (memt/->DerivedRelation #(agg-reduce % grp f)
                               (memt/relschema rel)
                               [rel])))
    (summarize-aggregate [_ grp agg]
      (reify-rel
       (memt/->DerivedRelation #(aggregate % grp agg)
                               (->> agg
                                    (map-values first)
                                    (merge (select-keys (memt/relschema rel) grp)))
                               [rel])))
    (join [_ rel-wrapped]
      (reify-rel
       (let [rel- (memt/unwrap-rel rel-wrapped)
             fk (memt/foreign-keys* rel rel-)
             sx (memt/relschema rel)
             sy (memt/relschema rel-)]
         (assert (every? #(= (get sx %) (get sy %))
                         (set/intersection (set (keys sx)) (set (keys sy))))
                 (format "Cannot join relations: do not agree on schema of shared key names: %s %s" (pr-str sx) (pr-str sy)))

         (memt/->DerivedRelation
          #(set/join %1 %2 fk)
          (merge sx sy)
          [rel rel-]))))
    (minus [_ rel-wrapped]
      (reify-rel
       (let [rel- (memt/unwrap-rel rel-wrapped)
             sx (memt/relschema rel)
             sy (memt/relschema rel-)]
         (assert (= sx sy)
                 (format "Cannot subtract relations: do not agree on schema: %s %s" (pr-str sx) (pr-str sy)))
         (memt/->DerivedRelation set/difference sx [rel rel-]))))
    (relschema [_] (memt/relschema rel))

    WrappedRelation
    (unwrap-rel [_] rel)))
