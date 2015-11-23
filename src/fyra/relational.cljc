(ns fyra.relational
  (:refer-clojure :exclude [extend]))

(defprotocol FRPRelation
  "A relation for an instance of FRP"
  (project [rel ks]
    "set-projection (akin to select-keys) of the relation.")
  (project-away [rel ks]
    "dual of project (akin to dissoc)")
  (extend [rel {:as exts}]
    "extend the relation with more fields.
     exts is a map of new fields keywords to
     a pair [s f], where f is a function of the tuple
     returning the new value for that key of type s.")
  (restrict [rel f]
    "restrict a relation by a boolean condition (akin to filter)")
  (summarize-reduce [rel grp f]
    "Reduction of the relation
     grp is a vector of tuple fields
     f is a function of tuple sets to a reduced tuple set

     First, it aggregates the items in the relation
     by the fields in the vector grp.
     Then, it calls f on each group, with f
     returning a set of tuples.

     example: (summarize rel [:group] #(maximum-key :priority %))")
  (summarize-aggregate [rel grp {:as agg}]
    "Aggregation of the relation.
     grp is vector of tuple fields
     agg is map of new fields to [s f] schema/function pairs
     with f being a function of a set of tuples to a value defined
     by schema s.

     First, it groups the tuples by the fields in grp.
     Then we merge the grouping fields map to the output of agg's
     application.

     example: (summarize rel [:group] {:members [Integer count]})")
  (join [rx ry]
    "set-join two relations, using foreign keymaps if defined.")
  (minus [rx ry]
    "set-difference of two relations. They must have the same schema.")
  (relschema [rel] "Get the schema of tuples of the rel"))
