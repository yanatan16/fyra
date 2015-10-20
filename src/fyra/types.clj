(ns fyra.types
  (:require [clojure.core.typed :refer [defalias] :as t])
  (:import [clojure.lang IDeref IRef IMeta]))

(defalias RelType (t/Map t/Kw t/Type))


(defalias CandidateKeys (t/Vec t/Kw))
(defalias CandidateKeysList (t/Vec CandidateKeyList))
(defalias CandidateKeysInput (t/U CandidateKeys CandidateKeysList))

(defalias ForeignKeys (t/Map t/Kw t/Kw))
(defalias ForeignKeysMap (t/Map t/Sym ForeignKeys))

(defalias RelVarCombined '{:fields RelType
                           :name String
                           :candidate CandidateKeysInput
                           :foriegn ForeignKeysMap})

(defalias Tuple (t/Map t/Kw t/Any))
(defalias Data (t/Set Tuple))

(defalias UpdateFn (t/Fn [Tuple -> Tuple]))
(defalias FilterFn (t/Fn [Tuple -> Boolean]))

(defalias AggExtendFn (t/Fn [(t/Coll Tuple) -> Tuple]))
(defalias AggReduceFn (t/Fn [(t/Coll Tuple) -> (t/Coll Tuple)]))

(defalias ConstraintFn [ft/Data * -> Boolean])


(t/defalias StoredData (t/Map CandidateKeys Data))
(t/defalias TypeFn (t/Fn [RelType -> RelType]))

(t/defprotocol IRelation
  (reltype [this] :- RelType))
(t/defprotocol IInsertable
  (conj! [this items :- (t/Coll Tuple)] :- Data))
(t/defprotocol IDeletable
  (del-items! [this items :- (t/Coll Tuple)] :- Data)
  (del! [this] :- nil))
(t/defprotocol IUpdatable
  (update! [this f :- UpdateFn] :- Data))
(t/defprotocol IJoinable
  (syms [this] :- (t/Vec t/Sym))
  (foreign-keys [this rel :- IJoinable] :- ForeignKeys))
(t/defprotocol IConstrainable
  (constrain [this f :- (t/Fn)] :- nil))

(t/defalias Relation (t/I IRelation IJoinable IDeref IMeta IRef))
(t/defalias URelation (t/I IRelation IUpdatable IDeletable))
(t/defalias UIRelation (t/I IRelation IInsertable))

(t/ann updatable? [IRelation -> Boolean])
(defn updatable? [rel] (satisfies? IUpdatable rel))
