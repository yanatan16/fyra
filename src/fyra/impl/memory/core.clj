(ns fyra.impl.memory.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.types :as memt]))

(def ^:private db (atom {}))

;;;;;;;;;;;;;;;;
;;; Declares ;;;
;;;;;;;;;;;;;;;;

(defn declare-relvar [{:keys [name candidate foreign fields]}]
  (memt/make-relvar {:name name
                     :type fields
                     :candidates candidate
                     :foreign foreign}))

(defn declare-view [name rel] rel)

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

(defn select [rel] (memt/exec rel @db))

(defn- swap-db [rel f]
  (swap! db #(let [olddb % newdb (f %)]
               (memt/notify-observers rel :constraint olddb newdb)
               ;; TODO use core async
               (memt/notify-observers rel :observer olddb newdb)
               newdb)))

(defn ^:private make-update-item-f [updates]
  #(reduce (fn [it [k v]] (if (fn? v) (clojure.core/update it k v)
                                      (assoc it k v))) % updates))

(defn insert [rel & items]
  (swap-db rel #(memt/insert rel % items)))

(defn update [rel {:as updates}]
  (swap-db rel #(memt/update rel % (make-update-item-f updates))))

(defn delete [rel]
  (swap-db rel #(memt/del rel %)))
