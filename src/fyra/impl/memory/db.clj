(ns fyra.impl.memory.db
  (:refer-clojure :exclude [swap! deref])
  (:require [fyra.impl.memory.meta :as meta]))

(def ^:private -db "database" (atom nil))
(defn swap! [f] (clojure.core/swap! -db f))
(defn deref [] @-db)

(defn add-item [db {relid :_rel :as item}]
  (update-in db [relid] #(-> % set (conj item))))

(defn remove-item [db {relid :_rel :as item}]
  (update-in db [relid] #(-> % set (disj item))))

(defn update-item [db item f]
  (let [new-item (f item)]
    (-> db
        (remove-item item)
        (add-item new-item))))

(defn execute-rel [rel db]
  (if-let [sid (meta/storage-id rel)]
    (get db sid)
    (rel db)))

(defn test-constraints [db]
  (doall (for [{:keys [expl f]} (meta/constraints)]
    (if (not (f #(execute-rel % db)))
      (throw (ex-info "Violated constraint" {:explanation expl})))))
  db)