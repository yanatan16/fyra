(ns fyra.types
  (:require [clojure.core.typed :refer [defalias U Map Vec Set Fn Kw Type Sym Coll Any]]))

(defalias RelType (Map Kw Type))


(defalias CandidateKeys (Vec Kw))
(defalias CandidateKeysList (Vec CandidateKeyList))
(defalias CandidateKeysInput (U CandidateKeys CandidateKeysList))

(defalias ForeignKeys (Vec Kw))
(defalias ForeignKeysInput (Map Sym ForeignKeys))

(defalias RelVarCombined '{:fields RelType
                            :name String
                            :candidate CandidateKeysInput
                            :foriegn ForeignKeysInput})

(defalias Tuple (Map Kw Any))
(defalias Data (Set Tuple))

(defalias UpdateFn (Fn [Tuple -> Tuple]))
(defalias FilterFn (Fn [Tuple -> Boolean]))

(defalias AggExtendFn (Fn [(Coll Tuple) -> Tuple]))
(defalias AggReduceFn (Fn [(Coll Tuple) -> (Coll Tuple)]))
