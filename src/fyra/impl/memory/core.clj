(ns fyra.impl.memory.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.types :as memt]
            [clojure.core.typed :as t]
            [fyra.types :as ft]))

(def ^:private db (atom {}))

;;;;;;;;;;;;;;;;
;;; Declares ;;;
;;;;;;;;;;;;;;;;

(t/ann declare-relvar [(t/HMap :mandatory {:name String
                                           :fields (t/Map t/Kw t/Type)}
                               :optional {:candidate ft/CandidateKeysList
                                          :foreign ft/ForeignKeysMap})
                       -> memt/RelVar])
(defn declare-relvar [{:keys [name candidate foreign fields]}]
  (memt/make-relvar {:name name
                     :type fields
                     :candidates candidate
                     :foreign foreign}))

(t/ann declare-view [String memt/Relation -> memt/Relation])
(defn declare-view [name rel] rel)

(t/ann declare-constraint [String memt/Observable ft/ConstraintFn -> t/Any])
(defn declare-constraint [explanation rel f]
  (memt/add-observer rel
   :constraint (subs explanation 0 (min (count explanation) 20))
   (fn [_ data _]
     (if-not (f data)
       (throw (ex-info "Constraint violated"
                       {:explanation explanation}))))))

(defn declare-observer [key rel f]
  (memt/add-observer rel :observer key (fn [old new _] (f old new))))

;;;;;;;;;;;;;;;;;;
;;; Operations ;;;
;;;;;;;;;;;;;;;;;;

(t/ann select [memt/Relation -> ft/Data])
(defn select [rel] (memt/exec rel @db))

(defn- swap-db [rel f]
  (swap! db #(let [olddb % newdb (f %)]
               (memt/notify-observers rel :constraint olddb newdb)
               ;; TODO use core async
               (memt/notify-observers rel :observer olddb newdb)
               newdb)))

(t/ann make-update-item-f [(t/Map t/Kw [ft/Tuple -> Any]) ->
                           [(t/Map t/Kw t/Any) -> (t/Map t/Kw t/Any)]])
(defn ^:private make-update-item-f [updates]
  #(reduce (fn [it [k v]] (if (fn? v) (clojure.core/update it k v)
                                      (assoc it k v))) % updates))

(t/ann insert [memt/UpdatableRelation ft/Tuple * -> Any])
(defn insert [rel & items]
  (swap-db rel #(memt/insert rel % items)))

(t/ann update [memt/UpdatableRelation (t/Map t/Kw (t/Fn [ft/Tuple -> Any])) -> Any])
(defn update [rel {:as updates}]
  (swap-db rel #(memt/update rel % (make-update-item-f updates))))

(t/ann delete [memt/UpdatableRelation -> Any])
(defn delete [rel]
  (swap-db rel #(memt/del rel %)))
