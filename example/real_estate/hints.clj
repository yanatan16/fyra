(ns real-estate.hints
  (:require [fyra.core :refer (declare-store)]
            [real-estate.base-relvars :refer :all]
            [real-estate.internal-views :refer :all]))

; Tell fyra to store the derived view PropertyInfo
(declare-store :view PropertyInfo)

; Tell fyra to store the base relvars Room and Floor together (denormalized)
(declare-store :shared Room Floor)

; Tell fyra to store photo's separately because they are rarely used
(declare-store :separate Property [:photo])