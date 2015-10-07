(ns real-estate.internal-views
  (:require [fyra.core :refer (defview)]
            [fyra.relational :as r]
            [real-estate.types :refer :all]
            [real-estate.user-defined-functions :refer :all]
            [real-estate.base-relvars :refer :all]))

; RoomInfo => {:address Address :room-name String
;              :width Double :breadth Double
;              :type RoomType :room-size Double}
(defview RoomInfo
  "Room with :room-size calculated"
  (r/extend Room :room-size (* width breadth)))

; Acceptance => {:address Address :offer-date DateTime
;                :bidder-name Name :bidder-address Address
;                :decision-date DateTime}
(defview Acceptance
  "Accepted Decisions"
  (r/project-away (r/restrict Decision #(= (:accepted %) true))
                  :accepted))


; Rejection => {:address Address :offer-date DateTime
;               :bidder-name Name :bidder-address Address
;               :decision-date DateTime}
(defview Rejection
  "Rejected Decisions"
  (r/project-away (r/restrict Decision #(= (:accepted %) false))
                  :accepted))


; PropertyInfo => {:address Address :price: Price
;                  :photo Filename :agent Agent
;                  :date-registered DateTime :price-band PriceBand
;                  :area-code AreaCode :num-rooms Integer
;                  :sq-ft Double}
(defview PropertyInfo
  "Property with calculated information"
  (r/extend Property
    :price-band #(price->price-band (:price %))
    :area-code #(address->area-code (:address %))
    :num-rooms (fn [p] (r/count (r/restrict RoomInfo
                                            #(= (:address p) (:address %)))))
    :sq-ft (fn [p] (r/sum room-size
                          (r/restrict RoomInfo
                                      #(= (:address p) (:address %)))))))

; CurrentOffer :: {:address Address :offer-price Price
;                  :offer-date DateTime :bidder-name Name
;                  :bidder-address Address}
(defview CurrentOffer
  "Latest offers from individual bidders"
  (r/summarize Offer
               [:address :bidder-name :bidder-address]
               #(r/maximum-key :offer-date %)))

; RawSales :: {:address Address :offer-price Price
;              :decision-date DateTime :agent Agent
;              :date-registered DateTime}
(defview RawSales
  "Sold properties with offer and decision information"
  (r/project-away (r/join Acceptance
                          (r/join CurrentOffer
                                  (r/project Property :address :agent :date-registered)))
                  :offer-date :bidder-name :bidder-address))

; SoldProperty :: {:address Address}
(defview SoldProperty
  "Sold properties' addresses"
  (r/project RawSales :address))

; UnsoldProperty :: {:address Address}
(defview UnsoldProperty
  "Unsold properties' addresses"
  (r/minus (r/project Property :address)
           SoldProperty))

; SalesInfo :: {:address Address :agent agent :area-code AreaCode
;               :sale-speed SpeedBand :price-band PriceBand}
(defview SalesInfo
  "Sales information for agents"
  (r/project (r/extend RawSales
                       :area-code #(address->area-code (:address %))
                       :sale-speed #(dates->speed-band (:date-registered %) (:decision-date %))
                       :price-band #(price->price-band (:offer-price %)))
             :address :agent :area-code :sale-speed :price-band))

; SalesCommissions :: {:address Address :agent Agent
;                      :commission Double}
(defview SalesCommissions
  "Sales commissions"
  (r/project (r/join SalesInfo Commission)
             :address :agent :commission))
