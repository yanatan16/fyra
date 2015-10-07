(ns fyra.test-app
  (:require [fyra.core :as f]
            [fyra.relational :as r]
            [clojure.set :as set]))

(f/defrelvar TodoList
  {:id String
   :title String
   :color String})

(f/defrelvar TodoItem
    {:list String
     :content String
     :done? Boolean}
    :foreign {TodoList {:list :id}})

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

(defn reset-app []
  (f/delete TodoList)
  (f/delete TodoItem)

  ;; Insert some data
  (f/insert TodoList home-list)
  (apply f/insert TodoItem home-items)

  (f/insert TodoList project-1-list)
  (apply f/insert TodoItem project-1-items)

  (f/insert TodoList project-2-list)
  (apply f/insert TodoItem project-2-items))