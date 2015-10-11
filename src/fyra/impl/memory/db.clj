(ns fyra.impl.memory.db
  (:refer-clojure :exclude [swap! deref])
  (:require [fyra.impl.memory.meta :as meta]))

(def ^:private -db "database" (atom nil))
(defn swap! [f] (clojure.core/swap! -db f))
(defn deref [] @-db)

(defn set-conj [item] #(-> % set (conj item)))
(defn set-disj [item] #(-> % set (disj item)))

(defn candidate [item ck]
  (if (empty? ck) item (select-keys item ck)))

(defn add-item [db {relid :_rel :as item}]
  (reduce
    (fn [db ck]
      (let [s (get-in db [relid ck])
            citem (candidate item ck)]
        (if (get s citem) (throw (ex-info "Candidate uniquness violated"
                                          {:relvar relid :candidate-keys ck})))
        (update-in db [relid ck] (set-conj citem))))
    db (meta/candidate-keys {:id relid})))

(defn remove-item [db {relid :_rel :as item}]
  (reduce #(update-in %1 [relid %2] (-> item (candidate %2) set-disj))
          db
          (meta/candidate-keys {:id relid})))

(defn update-item [db item f]
  (-> db
      (remove-item item)
      (add-item (f item))))

(defn get-items [db relid]
  (get-in db [relid []]))

(defn execute-rel [rel db]
  (if-let [sid (meta/storage-id rel)]
    (get-items db sid)
    (rel db)))

(defn test-constraints [db]
  (doall (for [{:keys [expl f]} (meta/constraints)]
    (if (not (f #(execute-rel % db)))
      (throw (ex-info "Violated constraint" {:explanation expl})))))
  db)