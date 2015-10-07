(ns real-estate.core
  (:require [fyra.types :refer (enum)]))

(def Address String)
(def Agent String)
(def Name String)
(def Price Double)
(def Filename String)

(def RoomType (enum :kitchen :bathroom :living-room))
(def PriceBand (enum :low :med :high :premium))
(def AreaCode (enum :city :suburban :rural))
(def SpeedBand (enum :very-fast :fast :medium :slow :very-slow))