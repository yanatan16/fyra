(ns real-estate.base-relvars
  (:require [fyra.sweet :refer (defrelvar)]
            [real-estate.types :refer :all]))

(defrelvar Property
  "Basic property information"
  {:address Address
   :price Price
   :photo Filename
   :agent Agent
   :date-registered DateTime}
  :candidate [:address])

(defrelvar Offer
  "Offers by bidders on Properties"
  {:address Address
   :bidder-name Name
   :bidder-address Address
   :offer-date DateTime
   :offer-price Price}
  :candidate [:address :bidder-name :bidder-address :offer-date]
  :foreign {'Property {:address :address}})

(defrelvar Decision
  "Decisions by sellers on Offers"
  {:address Address
   :bidder-name Name
   :bidder-address Address
   :offer-date DateTime
   :decision-date DateTime
   :accepted Boolean}
  :candidate [:address :bidder-name :bidder-address :offer-date]
  :foreign {'Offer {:address :address
                    :bidder-name :bidder-name
                    :bidder-address :bidder-address
                    :offer-date :offer-date}})

(defrelvar Room
  "Room inside of Properties and Floors"
  {:address Address
   :room-name String
   :width Double
   :breadth Double
   :type RoomType}
  :candidate [:address :room-name]
  :foreign {'Property {:address :address}})

(defrelvar Floor
  "Floor inside of Properties containing Rooms"
  {:address Address
   :room-name string
   :floor int}
  :candidate [:address :room-name]
  :foreign {'Property {:address :address}})

(defrelvar Commission
  "Commission definitions"
  {:price-band PriceBand
   :area-code AreaCode
   :sale-speed SpeedBand
   :commission double}
  :candidate [:price-band :area-code :sale-speed])
