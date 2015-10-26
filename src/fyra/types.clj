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

(defalias ConstraintFn [Data * -> Boolean])
(defalias Constraint1Fn [Data -> Boolean])
(defalias ObserverFn [Data * -> Any])
