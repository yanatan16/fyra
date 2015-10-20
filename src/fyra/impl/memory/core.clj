(ns fyra.impl.memory.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.types :as memt]
            [clojure.core.typed :as t]
            [fyra.types :as ft]))

;;;;;;;;;;;;;;;;
;;; Declares ;;;
;;;;;;;;;;;;;;;;

(t/ann declare-relvar [(t/HMap :mandatory {:name String
                                           :fields (t/Map t/Kw t/Type)}
                               :optional {:candidate ft/CandidateKeysList
                                          :foreign ft/ForeignKeysMap})
                       -> RelVar])
(defn declare-relvar [{:keys [name candidate foreign fields]}]
  (memt/make-relvar {:name name
                     :type fields
                     :candidates candidate
                     :foreign foreign}))

(t/ann declare-view [String ft/IRelation -> ft/IRelation])
(defn declare-view [name rel] rel)

(t/ann declare-constraint [String (t/Coll ft/IConstrainable) ft/ConstraintFn -> t/Any])
(defn declare-constraint [explanation rels f]
  (throw (Exception. "not implemented"))
  #_(memt/constrain! explanation rels f))

;;;;;;;;;;;;;;;;;;
;;; Operations ;;;
;;;;;;;;;;;;;;;;;;

(t/ann make-update-item-f [(t/Map t/Kw [ft/Tuple -> Any]) ->
                           [(t/Map t/Kw t/Any) -> (t/Map t/Kw t/Any)]])
(defn ^:private make-update-item-f [updates]
  #(reduce (fn [it [k v]] (if (fn? v) (clojure.core/update it k v)
                                      (assoc it k v))) % updates))

(t/ann insert [ft/IInsertable ft/Tuple * -> Any])
(defn insert [rel & items]
  (ft/conj! rel items))

(t/ann select [ft/IRelation -> (t/Set ft/Tuple)])
(defn select [rel] @rel)

(t/ann update [ft/IUpdatable (t/Map t/Kw (t/Fn [ft/Tuple -> Any])) -> Any])
(defn update [rel {:as updates}]
  (ft/update! rel (make-update-item-f updates)))

(t/ann delete [ft/IDeletable -> Any])
(defn delete [rel]
  (ft/del! rel))




