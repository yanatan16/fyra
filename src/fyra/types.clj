(ns fyra.types)

(defrecord EnumType [vs])

(defn enum
  "An single element of possible enumerations"
  [& vs]
  (EnumType. vs))

(def Milliseconds Long)
(def DateTime Milliseconds)