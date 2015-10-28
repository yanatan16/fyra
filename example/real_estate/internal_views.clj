(ns real-estate.internal-views
  (:require [fyra.sweet :refer (defview) :as f]
            [fyra.relational :as r]
            [real-estate.types :refer :all]
            [real-estate.user-defined-functions :refer :all]
            [real-estate.base-relvars :refer :all]))

; RoomInfo => {:address Address :room-name String
;              :width Double :breadth Double
;              :type RoomType :room-size Double}
(defview RoomInfo
  "Room with :room-size calculated"
  (f/extend Room :room-size [Integer (* width breadth)]))

; Acceptance => {:address Address :offer-date DateTime
;                :bidder-name Name :bidder-address Address
;                :decision-date DateTime}
(defview Acceptance
  "Accepted Decisions"
  (f/project-away (f/restrict Decision (= accepted true))
                  :accepted))


; Rejection => {:address Address :offer-date DateTime
;               :bidder-name Name :bidder-address Address
;               :decision-date DateTime}
(defview Rejection
  "Rejected Decisions"
  (f/project-away (f/restrict Decision (= accepted false))
                  :accepted))


; PropertyInfo => {:address Address :price: Price
;                  :photo Filename :agent Agent
;                  :date-registered DateTime :price-band PriceBand
;                  :area-code AreaCode :num-rooms Integer
;                  :sq-ft Double}
(defview PropertyInfo
  "Property with calculated information"
  (f/summarize
   (f/extend (f/join Property RoomInfo)
     :price-band [Keyword (price->price-band price)]
     :area-code [String (address->area-code address)])
   [:address :price :photo :agent :date-registered :price-band :area-code]
   {:num-rooms [Integer count]
    :sq-ft [Integer #(reduce + (map :room-size %))]}))

; CurrentOffer :: {:address Address :offer-price Price
;                  :offer-date DateTime :bidder-name Name
;                  :bidder-address Address}
(defview CurrentOffer
  "Latest offers from individual bidders"
  (f/summarize Offer
               [:address :bidder-name :bidder-address]
               #(f/maximum-key :offer-date %)))

; RawSales :: {:address Address :offer-price Price
;              :decision-date DateTime :agent Agent
;              :date-registered DateTime}
(defview RawSales
  "Sold properties with offer and decision information"
  (f/project-away
   (f/join Acceptance
           (f/join CurrentOffer
                   (f/project Property :address :agent :date-registered)))
                  :offer-date :bidder-name :bidder-address))

; SoldProperty :: {:address Address}
(defview SoldProperty
  "Sold properties' addresses"
  (f/project RawSales :address))

; UnsoldProperty :: {:address Address}
(defview UnsoldProperty
  "Unsold properties' addresses"
  (f/minus (f/project Property :address)
           SoldProperty))

; SalesInfo :: {:address Address :agent agent :area-code AreaCode
;               :sale-speed SpeedBand :price-band PriceBand}
(defview SalesInfo
  "Sales information for agents"
  (f/project (f/extend RawSales
               :area-code (address->area-code address)
               :sale-speed (dates->speed-band date-registered decision-date)
               :price-band (price->price-band offer-price))
             :address :agent :area-code :sale-speed :price-band))

; SalesCommissions :: {:address Address :agent Agent
;                      :commission Double}
(defview SalesCommissions
  "Sales commissions"
  (f/project (f/join SalesInfo Commission)
             :address :agent :commission))
