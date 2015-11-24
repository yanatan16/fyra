(ns fyra.test-app
  (:require [fyra.sweet :as f :refer (defrelvar defview)
             #?@(:cljs [:include-macros true])]
            [fyra.impl.memory.core :refer [create-db]]
            [schema.core :as s]
            [clojure.set :as set]))

(def db (create-db))


(defrelvar TodoList db
  {:id s/Str
   :title s/Str
   :color s/Str}
  :candidate [[:id] [:title :color]])

(defrelvar TodoItem db
  {:list s/Str
   :content s/Str
   :done? s/Bool}
  :foreign {'TodoList {:list :id}})

(defrelvar Unrelated db
  {:stuff s/Str})

(defview ListId db (f/project TodoList :id))
(defview ColoredItems db
  (f/project (f/join TodoList TodoItem)
             :content :color))

(f/constrain db "No uncolored lists"
  (f/restrict TodoList (empty? color))
  #(= 0 (count %)))

(f/constrain db "No more than 2 done items in a list"
  (f/restrict (f/summarize (f/restrict TodoItem done?)
                           [:list]
                           {:num-items [s/Int #(count %)]})
              (< 2 num-items))
  #(= 0 (count %)))

(f/constrain db "No items without lists"
 (f/minus TodoItem (f/project (f/join TodoItem TodoList)
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
    {:list "project-2" :content "Deploy to production" :done? false}})

(def all-lists #{home-list project-1-list project-2-list})
(def all-items (set/union home-items project-1-items project-2-items))

(def all-colored-items (->> all-items
                            (map #(assoc % :color (case (:list %) "home" "red" "blue")))
                            (map #(select-keys % [:content :color]))
                            set))

(defn reset-app []
  (f/delete db TodoItem)
  (f/delete db TodoList)
  (f/delete db Unrelated)

  ;; Insert some data
  (f/insert db TodoList home-list project-1-list project-2-list)

  (apply f/insert db TodoItem home-items)
  (apply f/insert db TodoItem project-1-items)
  (apply f/insert db TodoItem project-2-items))
