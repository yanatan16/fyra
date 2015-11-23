(ns fyra.impl.memory.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.types :as memt]
            [fyra.impl.memory.relational :as memr])
  (:import [fyra.core FRPDb]))

(defn- swap-db! [db rel f]
  (swap! db #(let [olddb % newdb (f %)]
               (memt/notify-observers rel :constraint olddb newdb)
               ;; TODO use core async
               (memt/notify-observers rel :observer olddb newdb)
               newdb)))

(defn create-db []
  (let [db (atom {})]
    (reify FRPDb
      (relvar [_ name s {:keys [candidate foreign]}]
        (memr/reify-rel
         (memt/make-relvar {:name name
                            :schema s
                            :candidates candidate
                            :foreign foreign})))
      (view [_ name rel {:keys [cache]}]
        ;; TODO implement caching
        rel)
      (constrain [_ expl rel f]
        (memt/add-observer (memt/unwrap-rel rel) :constraint (gensym)
                           (fn [_ data _]
                             (if-not (f data)
                               (throw (ex-info "Contraint violated"
                                               {:explanation expl}))))))
      (observe [_ key rel f]
        (memt/add-observer (memt/unwrap-rel rel) :observer key
                           (fn [old new _] (f old new))))
      (stop-observe [_ rel key]
        (memt/remove-observer (memt/unwrap-rel rel) :observer key))
      (select [_ rel]
        (memt/exec (memt/unwrap-rel rel) @db))
      (insert [_ wbaserel items]
        (let [baserel (memt/unwrap-rel wbaserel)]
          (swap-db! db baserel #(memt/insert baserel % items))))
      (delete [_ wrel]
        (let [rel (memt/unwrap-rel wrel)]
          (swap-db! db rel #(memt/del rel %))))
      (update [_ wrel f]
        (let [rel (memt/unwrap-rel wrel)]
          (swap-db! db rel #(memt/update rel % f)))))))
