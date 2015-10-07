(ns real-estate.base-relvars
  (:require [fyra.core :refer (defrelvar DateTime)]
            [real-estate.types :refer :all]))

(defrelvar "Basic property information"
  Property {:address Address
            :price Price
            :photo Filename
            :agent Agent
            :date-registered DateTime}
  :candidate [:address])

(defrelvar "Offers by bidders on Properties"
  Offer {:address Address
         :bidder-name Name
         :bidder-address Address
         :offer-date DateTime
         :offer-price Price}
  :candidate [:address :bidder-name :bidder-address :offer-date]
  :foreign {Property {:address :address}})

(defrelvar "Decisions by sellers on Offers"
  Decision {:address Address
            :bidder-name Name
            :bidder-address Address
            :offer-date DateTime
            :decision-date DateTime
            :accepted Boolean}
  :candidate [:address :bidder-name :bidder-address :offer-date]
  :foreign {Offer {:address :address
                   :bidder-name :bidder-name
                   :bidder-address :bidder-address
                   :offer-date :offer-date}})

(defrelvar "Room inside of Properties and Floors"
  Room {:address Address
        :room-name String
        :width Double
        :breadth Double
        :type RoomType}
  :candidate [:address :room-name]
  :foreign {Property {:address :address}})

(defrelvar "Floor inside of Properties containing Rooms"
  Floor {:address Address
         :room-name string
         :floor int}
  :candidate [:address :room-name]
  :foreign {Property {:address :address}})

(defrelvar "Commission definitions"
  Commission {:price-band PriceBand
              :area-code AreaCode
              :sale-speed SpeedBand
              :commission double}
  :candidate [:price-band :area-code :sale-speed])