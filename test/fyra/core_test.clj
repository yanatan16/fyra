(ns fyra.core-test
  (:require [midje.sweet :refer :all]
            [fyra.sweet :as f]
            [fyra.test-app :refer (reset-app) :as app]
            [clojure.set :as set]
            [clojure.string :as string]))

(facts "about basic use of relval and insert/select operations"
  (reset-app)
  (fact "select TodoList gives all lists"
    (f/select app/TodoList) => app/all-lists)
  (fact "select TodoItem gives all items"
    (f/select app/TodoItem) => app/all-items))

(facts "about dynamic use of insert, update and delete"
  (fact "inserting works"
    (let [item {:list "project-2" :content "Get approval" :done? false}]
      (f/insert app/TodoItem item)
      (f/select app/TodoItem) => (conj app/all-items item)))
  (fact "updating all in a relation works"
    (let [updtr #(update-in % [:title] str "UPDATED")]
      (f/update app/TodoList {:title #(str % "UPDATED")})
      (f/select app/TodoList) => (set (map #(update % :title str "UPDATED") app/all-lists))))
  (fact "deleting all in a relation works"
    (f/delete app/TodoItem)
    (f/select app/TodoItem) => #{}))

(facts "about violating candidate uniqueness"
  (reset-app)
  (fact "inserting fails when candidate keys aren't unique"
    (f/insert app/TodoList {:id "home" :title "Home2" :color "red"})
      => (throws #"Item is not unique under candidate" #(-> % ex-data :candidate (= [:id])))
    (f/select app/TodoList) => app/all-lists)
  (fact "updating fails when candidate keys aren't unique"
    (f/update app/TodoList {:title "same" :color "purple"})
      => (throws #"Item is not unique under candidate" #(-> % ex-data :candidate (= [:title :color])))
    (f/select app/TodoList) => app/all-lists))

(facts "about selecting using relational algebra"
  (reset-app)
  (fact "project works for a single key"
    (f/select (f/project app/TodoList :id))
      => (set (map #(select-keys % [:id]) app/all-lists)))
  (fact "project works for multiple keys"
    (f/select (f/project app/TodoList :title :color))
      => (set (map #(select-keys % [:title :color]) app/all-lists)))
  (fact "project-away works for multiple keys"
    (f/select (f/project-away app/TodoList :title :color))
    => (set (map #(select-keys % [:id]) app/all-lists)))
  (fact "extend creates new keys"
        (defn first-word [s] (-> s (string/split #" ") first))
        (f/select (f/extend app/TodoItem :first-word [String (first-word content)]))
        => (set (map #(assoc % :first-word (first-word (:content %))) app/all-items)))
  (facts "about restrict"
    (fact "restrict to done items"
      (f/select (f/restrict app/TodoItem done?))
        => (set (filter :done? app/all-items)))
    (fact "restrict to project-1 items"
          (f/select (f/restrict app/TodoItem (= list "project-1"))) => app/project-1-items)
    (fact "restrict nothing"
          (f/select (f/restrict app/TodoList true)) => app/all-lists)
    (fact "restrict everything"
          (f/select (f/restrict app/TodoItem false)) => #{}))
  (facts "about summarize"
    (fact "find number of lists by color"
      (f/select (f/summarize app/TodoList [:color] {:num-items ^Integer #(count %)}))
        => #{{:color "red" :num-items 1}
             {:color "blue" :num-items 2}})
    (fact "find total number of items without a grouping"
      (f/select (f/summarize app/TodoItem [] {:num-items ^Integer #(count %)}))
        => #{{:num-items (count app/all-items)}}))
  (facts "about join"
    (fact "join is commutative"
      (f/select (f/join app/TodoList app/TodoItem))
        => (f/select (f/join app/TodoItem app/TodoList)))
    (fact "join works with foreign keys"
      (f/select (f/join app/TodoList app/TodoItem))
        => (set/join app/all-lists app/all-items {:id :list}))
    (fact "join fails when two unrelated relations are used"
      (f/select (f/join app/Unrelated app/TodoItem)) => #{}
      (f/insert app/Unrelated {:stuff "yoyo"})
      (f/select (f/join app/Unrelated app/TodoItem))
        => (set (map #(assoc % :stuff "yoyo") app/all-items)))
    ;; TODO enable this
    #_(fact "join can use projected and extended relations"
      (f/select (f/join (f/project app/TodoList :id)
                        (f/extend app/TodoItem :abc [String "def"]))) => #{}))
  (facts "about minus"
    (fact "minus will take out appropriate items"
      (f/select (f/minus app/TodoItem
                         (f/restrict app/TodoItem done?)))
        => (set (filter #(not (:done? %)) app/all-items))))
  (facts "about combining multiple algebraic functions"
    (fact "find max number of lists by color"
      (f/select
        (f/summarize (f/summarize app/TodoList [:color] {:num-items ^Integer #(count %)})
                     [] #(f/maximum-key :num-items %)))
          => #{{:color "blue" :num-items 2}})
    (fact "find number of items in a list"
          (f/select (f/summarize (f/restrict app/TodoItem (= list "home"))
                             [:list] {:num-items ^Integer #(count %)}))
        => #{{:list "home" :num-items (count app/home-items)}})
    (fact "Find number of items in colored lists"
      (f/select (f/summarize (f/join app/TodoList app/TodoItem)
                             [:color]
                             {:num-items ^Integer #(count %)}))
        => #{{:color "red" :num-items (count app/home-items)}
             {:color "blue" :num-items (+ (count app/project-1-items)
                                          (count app/project-2-items))}})))
(facts "about updating and deleting using relational algebra"
  (fact "updating a single list with a function works"
    (let [g #(update-in % [:title] str "UPDATED")]
      (f/update (f/restrict app/TodoList (= id "home")) {:title #(str % "UPDATED")})
      (f/select (f/restrict app/TodoList (= id "home")))
        => #{(g app/home-list)}))
  (fact "updating multiple items with a set value works"
    (let [g #(assoc-in % [:done?] false)]
      (f/update (f/restrict app/TodoItem (= list "home")) {:done? false})
      (f/select (f/restrict app/TodoItem (= list "home")))
        => (set (map g app/home-items))))
  (fact "updating multiple items with multiple values works"
    (let [g #(-> % (assoc :done? false) (update :content (partial str "titlez")))]
      (f/update (f/restrict [app/TodoItem :as ti] (= ti/list "project-1")) {:done? false :content (partial str "titlez")})
      (f/select (f/restrict [app/TodoItem :as ti] (= ti/list "project-1")))
        => (set (map g app/project-1-items))))
  (fact "deleting a single list works"
        (f/delete (f/restrict app/TodoItem (= list "project-1")))
        (f/delete (f/restrict app/TodoList (= id "project-1")))
        (f/select (f/restrict app/TodoList (= id "project-1")))
      => #{})
  (fact "deleting a multiple items works"
        (f/delete (f/restrict app/TodoItem (= list "project-1")))
        (f/select (f/restrict app/TodoItem (= list "project-1")))
      => #{}))

(facts "about defview"
  (reset-app)
  (fact "selecting a view works"
    (f/select app/ListId) => (set (map #(select-keys % [:id]) app/all-lists))
    (f/select app/ColoredItems) => app/all-colored-items
    (f/delete app/TodoItem)
    (f/select app/ListId) => (set (map #(select-keys % [:id]) app/all-lists))
    (f/select app/ColoredItems) => #{}))

(facts
  "about typechecking"
  (fact "insertion checks fields"
        (f/insert app/TodoList {:whatever :yoyos})
        => (throws #"Item does not match type"))
  (fact "insertion checks types"
        (f/insert app/TodoList {:id :not-a-string :title "soemthing" :color "green"})
        => (throws #"Item does not match type"))
  (fact "update checks types"
        (f/update app/TodoList
                  {:id keyword})))

(facts "about contraints"
  (reset-app)
  (fact "violate a constraint on insertion"
    (f/insert app/TodoList {:id "empty-color-list" :title "hello" :color ""})
      => (throws #(-> % ex-data :explanation (= "No uncolored lists")))
      (f/select (f/restrict app/TodoList (empty? color))) => #{})
  (fact "violate a constraint on update"
    (f/update app/TodoItem {:done? true})
      => (throws #(-> % ex-data :explanation (= "No more than 2 done items in a list")))
      (count (f/select (f/restrict app/TodoItem done?))) => 2)
  (fact "violate a constraint on delete"
    (f/delete app/TodoList)
      => (throws #(-> % ex-data :explanation (= "No items without lists")))
    (count (f/select app/TodoList)) => 3))

(facts
 "about observers"
 (reset-app)
 (let [hits (atom 0)
       last (atom nil)]
   (f/observe "test" (f/restrict [app/TodoItem :as ti] ti/done?)
              #(do (swap! hits inc) (reset! last %2)))
   @hits => 0
   (fact "insert triggers observer appropriately"
         (f/insert app/TodoItem {:list "project-2" :content "done" :done? true})
         (f/insert app/TodoItem {:list "project-2" :content "not done" :done? false})
         @hits => 1
         @last => (f/select (f/restrict app/TodoItem done?)))
   (fact "update triggers observer appropriately"
         (f/update (f/restrict app/TodoItem (= content "not done"))
                   {:done? true})
         (f/update (f/restrict app/TodoList (= id "project-1")) {:color "yellow"})
         @hits => 2
         @last => (f/select (f/restrict app/TodoItem done?)))
   (fact "delete triggers observer appropriately"
         (f/delete (f/restrict app/TodoItem (= list "project-1")))
         (f/delete app/Unrelated)
         @hits => 3
         @last => (f/select (f/restrict app/TodoItem done?)))))
