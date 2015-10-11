(ns fyra.impl.memory.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.meta :refer (view?)]
            [fyra.impl.memory.db :as db]))


(defn- make-update-item-f [updates]
  #(reduce (fn [it [k v]] (if (fn? v) (clojure.core/update it k v)
                                      (assoc it k v))) % updates))

;; TODO
;; update stored views
;; verify item matches schema
(defn insert [{:keys [name id] :as rel} & items]
  {:pre [(not (view? rel))]}
  (db/swap! (fn [db]
    (->> items
         (map #(assoc % :_rel id))
         (reduce #(db/add-item %1 %2) db)
         db/test-constraints))))

(defn select [rel]
  (->> (db/execute-rel rel (db/deref))
       (map #(dissoc % :_rel))
       set))

;; TODO: verify item matches schema
(defn update [rel & {:as updates}]
  (let [f (make-update-item-f updates)]
    (db/swap! (fn [db]
      (->> (db/execute-rel rel db)
           (filter :_rel)
           (reduce #(db/update-item %1 %2 f) db)
           db/test-constraints)))))

;; TODO: accept a complex relation
(defn delete [rel]
  (db/swap! (fn [db]
    (->> (db/execute-rel rel db)
         (reduce #(db/remove-item %1 %2) db)
         db/test-constraints))))




