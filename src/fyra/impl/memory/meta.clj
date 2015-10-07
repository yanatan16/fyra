(ns fyra.impl.memory.meta
  (:require [clojure.set :as set]))

(def ^:private -mb "metadatabase" (atom nil))

;; declares

(defn declare-relvar [{:keys [name] :as rel}]
  (let [id name]
    (swap! -mb assoc-in [:relvar id] rel)
    {:name name :id id :view? false}))

(defn relvar-info [{:keys [id]}]
  (get-in @-mb [:relvar id]))

(defn foreign-keys [rel-1 rel-2]
  (or (get-in (relvar-info rel-1) [:foreign rel-2])
      (set/map-invert (get-in (relvar-info rel-2) [:foreign rel-1]))))

(defn storage-id [{:keys [id] :as rel}]
  (and (map? rel) id))
(defn relvar? [rel] (some? (storage-id rel)))
(defn view? [{:keys [view?]}] view?)

;; TODO implement storage-required views
(defn declare-view [name rel] rel)

(defn declare-constraint [explanation f]
  (swap! -mb update-in [:constraints] conj {:expl explanation :f f}))
(defn constraints [] (:constraints @-mb))