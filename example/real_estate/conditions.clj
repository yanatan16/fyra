(ns real-estate.conditions
  (:require [fyra.core :refer (ensure)]
            [fyra.relational :as r]
            [real-estate.types :refer :all]
            [real-estate.user-defined-functions :refer :all]
            [real-estate.base-relvars :refer :all]
            [real-estate.internal-views :refer :all]
            [real-estate.external-views :refer :all]))

(ensure "No properties with no rooms"
  (= (count (r/restrict PropertyInfo #(< (:num-rooms %) 1))) 0))

(ensure "No bidders bidding on their own property"
  (= (count (r/restrict Offer
                        #(= (:bidder-address %) (:address %)))) 0))

(ensure "No offers on sold properties"
  (= (count (r/restrict (r/join Offer
                                (r/project Acceptance
                                           :address :decision-date))
                        #(> (:offer-date %) (:decision-date %))))
     0))

(ensure "No more than 50 premium price band properties on web site"
  (< (count (r/restrict (r/extend PropertyForWebSite
                                  :price-band #(price->price-band (:price %)))
                        #(= (:price-band %) :premium)))
     50))

(ensure "No more than 10 offers by a single bidder on a single property"
  (= (count (r/restrict (r/summarize Offer
                                     [:address :bidder-name :bidder-address]
                                     {:num-offers #(count %)})
                        (> (:num-offers %) 10))))
     0)
