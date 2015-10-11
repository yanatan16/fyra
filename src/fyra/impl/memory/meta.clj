(ns fyra.impl.memory.meta
  (:require [clojure.set :as set]))

(def ^:private -mb "metadatabase" (atom nil))

;; declares

(defn munge-candidate-keys [cks]
  (cond (and (vector? cks) (vector? (first cks))) (conj cks [])
        (vector? cks) [cks []]
        :else [[]]))

(defn declare-relvar [{:keys [name candidate] :as rel}]
  (let [id name
        cks (munge-candidate-keys candidate)]
    (swap! -mb assoc-in [:relvar id] (assoc rel :candidate cks))
    {:name name :id id :view? false}))

(defn relvar-info [{:keys [id]}]
  (get-in @-mb [:relvar id]))

(defn foreign-keys [rel-1 rel-2]
  (or (get-in (relvar-info rel-1) [:foreign rel-2])
      (if-let [m (get-in (relvar-info rel-2) [:foreign rel-1])] (set/map-invert m))
      (throw (ex-info "No foreign keys on relations" {:rels [rel-1 rel-2]}))))

(defn candidate-keys [rel]
  (:candidate (relvar-info rel)))

(defn storage-id [{:keys [id] :as rel}]
  (and (map? rel) id))
(defn relvar? [rel] (some? (storage-id rel)))
(defn view? [{:keys [view?]}] view?)

;; TODO implement storage-required views
(defn declare-view [name rel] rel)

(defn declare-constraint [explanation f]
  (swap! -mb update-in [:constraints] conj {:expl explanation :f f}))
(defn constraints [] (:constraints @-mb))