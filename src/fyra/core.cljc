(ns fyra.core
  (:refer-clojure :exclude [update]))

(defprotocol FRPDb
  "An instance of a db implementing Functional Relational Programming"
  (relvar [db name s {:keys [candidate foreign]}]
    "Create a base relvar with name name and schema s.
     Optionally, also the candidate keys, or a foreign key map.
     Implementations may take additional options or
     ignore some options.")
  (view [db name rel {:keys [cache]}]
    "Create a named view from a derived relvar.
     Optionally cache the view.
     Implementations may take additional options or ignore some.")
  (constrain [db expl rel f]
    "Create an instance constraint that must always be valid.
     f is a function of an exercised relation rel.
     If f returns falsy, an error is thrown with explanation.")
  (observe [db key rel f]
    "Create an observation callback (on key) on a relation.
     If the relation changes, f will be caled with the
     executed relation in previous state and new state")
  (stop-observe [db rel key] "Stop observing on key")
  (select [db rel] "Execute a relation against current db state")
  (insert [db baserel items]
    "Insert items into a base relvar. Items will be typechecked against schema.")
  (delete [db rel] "Delete all items in the relation.")
  (update [db rel f]
    "Update the items in relation rel with updts
     f is a map of the tuple to a new tuple."))
