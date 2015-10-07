(ns real-estate.user-defined-functions
  (:require [fyra.core :refer (Date)]
            [real-estate.types :refer :all]))

(defn ^PriceBand price->price-band [^Price price]
  (condp >= price
    300000 :low
    650000 :med
    1000000 :high
    :premium))

(defn ^AreaCode address->area-code [^Address address]
  (second (re-find #"[ \t\n](\d{5})$" address)))

(def days-in-millis (* 1000 60 60 24))
(defn ^SpeedBand dates->speed-band [^Date begin ^Date end]
  (condp >= (/ (- end begin) days-in-millis)
    10 :very-fast
    20 :fast
    30 :medium
    60 :slow
    :very-slow))
