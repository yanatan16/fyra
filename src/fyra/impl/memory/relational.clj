(ns fyra.impl.memory.relational
  (:refer-clojure :exclude [extend])
  (:require [clojure.core.typed :as t]
            [fyra.impl.memory.types :as memt]
            [fyra.impl.memory.util :refer [map-values]]))

(defn- type-of-fn [f]
  (let [rt (:tag (meta f))]
    (assert rt (str "Can't determine the return type of " %))
    rt))

(defn project [rel & ks]
  (let [skf #(select-keys % ks)]
    (memt/->MapFilterRelation identity skf skf rel)))

(defn project-away [rel & ks]
  (let [skf #(apply dissoc % ks)]
    (memt/->MapFilterRelation identity skf skf rel)))

(defn extend [rel {:as exts}]
  (let [mapf (fn [item] (merge item (map-values #(% item) exts)))
        typef (fn [type] (merge type (map-values type-of-fn exts)))]
    (memt/->MapFilterRelation identity mapf typef rel)))

(defn restrict [rel f]
  (if (memt/updatable? rel)
      (memt/->UpdateMapFilterRelation f identity identity rel)
      (memt/->MapFilterRelation f identity identity rel)))

(defn join [rel-1 rel-2]
  (let [jrel (memt/->JoinedRelation rel-1 rel-2)]
    (memt/foreign-keys jrel) ; force an assertion to be called
    jrel))

(defn minus [rel-1 rel-2]
  (let [srel (memt/->SubtractedRelation rel-1 rel-2)]
    (memt/reltype srel) ; force assertion
    srel))

(defn summarize [rel ks acc]
  (if (fn? acc)
      (->AggReducedRelation ks acc rel)
      (->AggregatedRelation ks (map-values type-of-fn acc) acc rel)))