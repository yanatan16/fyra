(ns fyra.impl.memory.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.types :as memt]
            [clojure.core.typed :as t]
            [fyra.types :as ft]))


(t/defn ^:private make-update-item-f
  [updates :- (t/Map t/Kw (t/Fn [ft/Tuple -> Any]))]
  #(reduce (fn [it [k v]] (if (fn? v) (clojure.core/update it k v)
                                      (assoc it k v))) % updates))

(t/ann insert [memt/IInsertable ft/Tuple *])
(defn insert [rel & items]
  (memt/conj! items))

(t/ann select [memt/IRelation])
(defn select [rel] @rel)

(t/ann update [memt/IUpdatable (t/Map t/Kw (t/Fn [ft/Tuple -> Any]))])
(defn update [rel {:as updates}]
  (memt/update! rel (make-update-item-f updates)))

(t/ann delete [memt/IDeletable])
(defn delete [rel]
  (memt/del! rel))




