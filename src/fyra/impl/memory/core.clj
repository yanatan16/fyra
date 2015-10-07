(ns fyra.impl.memory.core
  (:refer-clojure :exclude [update])
  (:require [fyra.impl.memory.relational :refer (execute-rel)]
            [fyra.impl.memory.meta :refer (view?)]))

(def ^:private -db "database" (atom nil))

;; db operations
(defn- add-item [db {relid :_rel :as item}]
  (update-in db [relid] #(-> % set (conj item))))

(defn- remove-item [db {relid :_rel :as item}]
  (update-in db [relid] #(-> % set (disj item))))

(defn- update-item [db item f]
  (let [new-item (f item)]
    (assert (:_rel new-item) "Update must keep the :_rel key")
    (-> db
        (remove-item item)
        (add-item new-item))))

;; TODO
;; assure candidate uniqity
;; ensure no conditions violated
;; update stored views
;; verify item matches schema
(defn insert [{:keys [name id] :as rel} & items]
  {:pre [(not (view? rel))]}
  (swap! -db (fn [db]
    (->> items
         (map #(assoc % :_rel id))
         (reduce #(add-item %1 %2) db)))))

(defn select [rel]
  (->> (execute-rel rel @-db)
       (map #(dissoc % :_rel))
       set))

;; TODO: verify item matches schema
;; TODO: (update rel :field fn :field fn)
(defn update [rel f]
  (swap! -db
    (fn [db]
      (->> (execute-rel rel db)
           (filter :_rel)
           (reduce #(update-item %1 %2 f) db)))))

;; TODO: accept a complex relation
(defn delete [rel]
  (swap! -db
    (fn [db] (->> (execute-rel rel db)
                  (reduce #(remove-item %1 %2) db)))))




