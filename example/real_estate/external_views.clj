(ns real-estate.external-views
  (:require [fyra.core :refer (defview)]
            [fyra.relational :as r]
            [real-estate.types :refer :all]
            [real-estate.user-defined-functions :refer :all]
            [real-estate.base-relvars :refer :all]
            [real-estate.internal-views :refer :all]))

; OpenOffers :: {:address Address :offer-price Price
;                :offer-date DateTime :bidder-name NAme
;                :bidder-address Address}
(defview OpenOffer
  "Offers that have not been decided on"
  (r/join CurrentOffer
          (r/minus (r/project-away CurrentOffer :offer-price)
                   (r/project-away Decision :accepted :decision-date))))

; PropertyForWebSite :: {:address Address :price Price
;                        :photo Filename :num-rooms Integer
;                        :sq-ft Double}
(defview PropertiesForWebSite
  "Properties to show on web site"
  (r/project (r/join UnsoldProperty PropertyInfo)
             :address :price :photo :num-rooms :sq-ft))

; CommissionDue :: {:agent Agent :total-commission Double}
(defview CommissionDue
  "Total commission due to an agent"
  (r/project (r/summarize SalesCommissions
                          [:agent]
                          {:total-commission #(r/sum :commission %)})
             :agent :total-commission))