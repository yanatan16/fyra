(ns fyra.impl.memory.meta
  (:require [clojure.set :as set]
            [clojure.core.typed :refer (ann-form) :as t]))

;; types
(def RelvarInfo (t/HMap :mandatory {:name String
                                    }))

(def ^:private -mb "metadatabase"
  (t/atom> (t/HMap :mandatory {:relvar (t/Map String )})
           {:relvar {} :constraints '()}))
(ann -mb (t/Atom1 (t/HMap :optional {:relvar })))

(defn- munge-candidate-keys [cks]
  (cond (vector? (first cks)) (conj cks [])
        (vector? cks) [cks []]))
(ann-form munge-candidate-keys [CandidateKeysInput -> CandidateKeys])

(defn declare-relvar [{:keys [name candidate] :as rel}]
  (let [id name
        cks (munge-candidate-keys (or candidate [[]]))]
    (swap! -mb assoc-in [:relvar id] (assoc rel :candidate cks))
    {:name name :id id :view? false}))
(ann-form declare-relvar [(t/HMap :mandatory {:name String
                                              :fields (t/HMap)}
                                  :optional {:candidate CandidateKeysInput
                                             :foreign (t/Map Relation (t/))})])

(defn relvar-info [{:keys [id]}]
  (get-in @-mb [:relvar id]))

(defn relvar-spec [rel]
  (:fields (relvar-spec rel)))

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