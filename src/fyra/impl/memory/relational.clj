(ns fyra.impl.memory.relational
  (:refer-clojure :exclude [extend])
  (:require [clojure.set :as set]
            [fyra.impl.memory.meta :refer (foreign-keys)]
            [fyra.impl.memory.db :refer (execute-rel)]))

(defn- map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn project [rel & ks]
  (fn [db] (->> (execute-rel rel db)
                (map #(select-keys % (conj ks :_id)))
                set)))

(defn project-away [rel & ks]
  (fn [db] (->> (execute-rel rel db)
                (map #(apply dissoc % ks))
                set)))

(defn extend [rel & {:as exts}]
  (fn [db] (->> (execute-rel rel db)
                (map (fn [item]
                       (merge item (map-values #(% item) exts))))
                set)))

(defn restrict [rel f]
  (fn [db] (->> (execute-rel rel db)
                (filter f)
                set)))

(defn join [rel-1 rel-2]
  (let [foreign (foreign-keys rel-1 rel-2)]
    (fn [db] (let [it-1 (execute-rel rel-1 db)
                   it-2 (execute-rel rel-2 db)]
               (set/join it-1 it-2 foreign)))))

(defn minus [rel-1 rel-2]
  (fn [db] (->> [rel-1 rel-2]
                (map #(execute-rel % db))
                (#(set/difference (first %) (second %))))))

(defn summarize-group [acc km grp]
  (if (fn? acc) (acc grp)
      (merge km (map-values #(% grp) acc))))

(defn summarize [rel ks acc]
  (fn [db]
    (->> (execute-rel rel db)
         (reduce #(update-in %1 [(select-keys %2 ks)] conj %2) {})
         (map (fn [[km grp]] (summarize-group acc km grp)))
         set)))