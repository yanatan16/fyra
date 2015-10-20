(ns fyra.impl.memory.relational
  (:refer-clojure :exclude [extend])
  (:require [clojure.core.typed :as t]
            [fyra.types :as ft]
            [fyra.impl.memory.types :as memt]
            [fyra.impl.memory.util :refer [map-values]]))

(t/ann type-of-fn [ft/UpdateFn -> t/Any])
(defn- type-of-fn [f]
  (let [rt (:tag (meta f))]
    (assert rt (str "Can't determine the return type of " f))
    rt))

(t/ann project [Relation Kw * -> Relation])
(defn project [rel & ks]
  (let [skf #(select-keys % ks)]
    (memt/->MapFilterRelation identity skf skf rel)))

(t/ann project-away [Relation Kw * -> Relation])
(defn project-away [rel & ks]
  (let [skf #(apply dissoc % ks)]
    (memt/->MapFilterRelation identity skf skf rel)))

(t/ann extend [Relation (Map Kw (Fn [Tuple -> Any])) * -> Relation])
(defn extend [rel {:as exts}]
  (let [mapf (fn [item] (merge item (map-values #(% item) exts)))
        typef (fn [type] (merge type (map-values type-of-fn exts)))]
    (memt/->MapFilterRelation identity mapf typef rel)))

(t/ann restrict [Relation (Fn [Tuple -> Boolean]) -> Relation])
(defn restrict [rel f]
  (if (ft/updatable? rel)
      (memt/->UpdateMapFilterRelation f identity identity rel)
      (memt/->MapFilterRelation f identity identity rel)))

(t/ann join [Relation Relation -> Relation])
(defn join [rel-1 rel-2]
  (let [jrel (memt/->JoinedRelation rel-1 rel-2)]
    (ft/foreign-keys jrel jrel) ; force an assertion to be called
    jrel))

(t/ann minus [Relation Relation -> Relation])
(defn minus [rel-1 rel-2]
  (let [srel (memt/->SubtractedRelation rel-1 rel-2)]
    (ft/reltype srel) ; force assertion
    srel))

(t/ann summarize [Relation (Vec Kw) (U (Fn [(Seqable Tuple) -> Tuple])
                                     (Map Kw (Fn [(Seqable Tuple) -> Any]))) -> Relation])
(defn summarize [rel ks acc]
  (if (fn? acc)
      (memt/->AggReducedRelation ks acc rel)
      (memt/->AggregatedRelation ks (map-values type-of-fn acc) acc rel)))
