(ns fyra.test-app
  (:require [fyra.core :as f]
            [fyra.relational :as r]
            [clojure.set :as set]))

(def TodoList
  (f/relvar "TodoList"
    {:id String
     :title String
     :color String}
    :candidate [[:id] [:title :color]]))

(def TodoItem
  (f/relvar "TodoItem"
    {:list String
     :content String
     :done? Boolean}
    :foreign {'TodoList {:list :id}}))

(def Unrelated (f/relvar "Unrelated" {:stuff String}))

(def ListId (f/view "ListId" (r/project TodoList :id)))
(def ColoredItems
  (f/view "ColoredItems"
    (r/project (r/join TodoList TodoItem)
               :content :color)))

(f/constrain "No uncolored lists"
  (r/restrict TodoList #(empty? (:color %)))
  #(= 0 (count %)))

(f/constrain "No more than 2 done items in a list"
  (r/restrict (r/summarize (r/restrict TodoItem :done?)
                           [:list]
                           {:num-items ^Integer #(count %)})
              (fn [{:keys [num-items]}] (< 2 num-items)))
  #(= 0 (count %)))

(f/constrain "No items without lists"
 (r/minus TodoItem (r/project (r/join TodoItem TodoList)
                              :list :content :done?))
 #(= 0 (count %)))

(def home-list
  {:id "home" :title "TODO at home" :color "red"})
(def home-items
  #{{:list "home" :content "Buy a new sattelite dish" :done? false}
    {:list "home" :content "Reformat router" :done? false}
    {:list "home" :content "Do laundry" :done? true}
    {:list "home" :content "Take down Christmas lights" :done? false}})

(def project-1-list
  {:id "project-1" :title "Project 1's List" :color "blue"})
(def project-1-items
  #{{:list "project-1" :content "Burn it with fire" :done? true}
    {:list "project-1" :content "Rewrite everything" :done? false}})

(def project-2-list
  {:id "project-2" :title "Project 2's List" :color "blue"})
(def project-2-items
  #{{:list "project-2" :content "Add tests" :done? false}
    {:list "project-2" :content "Add type checking" :done? false}
    {:list "project-2" :content "Deploy to production" :done? true}})

(def all-lists #{home-list project-1-list project-2-list})
(def all-items (set/union home-items project-1-items project-2-items))

(def all-colored-items (->> all-items
                            (map #(assoc % :color (case (:list %) "home" "red" "blue")))
                            (map #(select-keys % [:content :color]))
                            set))

(defn reset-app []
  (f/delete TodoItem)
  (f/delete TodoList)
  (f/delete Unrelated)

  ;; Insert some data
  (f/insert TodoList home-list project-1-list project-2-list)

  (apply f/insert TodoItem home-items)
  (apply f/insert TodoItem project-1-items)
  (apply f/insert TodoItem project-2-items))
