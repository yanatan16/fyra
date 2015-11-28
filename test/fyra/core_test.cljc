(ns fyra.core-test
  (:refer-clojure :exclude [last])
  (:require #?(:clj [clojure.test :refer [deftest is use-fixtures]]
               :cljs [cljs.test :refer-macros [deftest is use-fixtures]])
            [fyra.sweet :as f
             #?@(:cljs [:include-macros true])]
            [fyra.test-app :refer (reset-app db) :as app]
            [schema.core :as s]
            [clojure.set :as set]
            [clojure.string :as string])
  (:import [clojure.lang ExceptionInfo]))

(def hits (atom 0))
(def last (atom nil))
(defn reset-observer []
  (def hits (atom 0))
  (def last (atom nil))
  (f/stop-observe db "test" app/TodoItem)
  (f/observe db "test" (f/restrict [app/TodoItem :as ti] ti/done?)
             #(do (swap! hits inc) (reset! last %2))))

#?(:cljs (use-fixtures :each
           :before #(do (reset-app) (reset-observer)))
   :clj  (use-fixtures :each
           #(do (reset-app) (reset-observer) (%))))

;(defmacro thrown-with-info? [obj form]
;  `(try (do ~form false)
;        (catch ExceptionInfo #e
;            (= (ex-info #e) ~obj))))

(deftest test-select-all-lists
  (is (= (f/select db app/TodoList) app/all-lists)))
(deftest test-select-all-items
  (is (= (f/select db app/TodoItem) app/all-items)))

(deftest test-insert-item
  (let [item {:list "project-2" :content "Get approval" :done? false}]
    (f/insert db app/TodoItem item)
    (is (= (f/select db app/TodoItem) (conj app/all-items item)))))
(deftest test-update-all-lists
  (let [updtr #(update-in % [:title] str "UPDATED")]
    (f/update db app/TodoList {:title #(str % "UPDATED")})
    (is (= (f/select db app/TodoList) (set (map #(update % :title str "UPDATED") app/all-lists))))))
(deftest test-delete-all
  (f/delete db app/TodoItem)
  (is (=  (f/select db app/TodoItem) #{})))

(deftest test-project-single-key
  (is (= (f/select db (f/project app/TodoList :id))
         (set (map #(select-keys % [:id]) app/all-lists)))))
(deftest test-project-mult-keys
  (is (= (f/select db (f/project app/TodoList :title :color))
         (set (map #(select-keys % [:title :color]) app/all-lists)))))
(deftest test-project-away-mult-keys
  (is (= (f/select db (f/project-away app/TodoList :title :color))
         (set (map #(select-keys % [:id]) app/all-lists)))))
(deftest test-extend
      (defn first-word [s] (-> s (string/split #" ") first))
  (is (= (f/select db (f/extend app/TodoItem :first-word [s/Str (first-word content)]))
         (set (map #(assoc % :first-word (first-word (:content %))) app/all-items)))))

(deftest test-restrict-done
  (is (= (f/select db (f/restrict app/TodoItem done?))
         (set (filter :done? app/all-items)))))
(deftest test-restrict-list
  (is (= (f/select db (f/restrict app/TodoItem (= list "project-1"))) app/project-1-items)))
(deftest test-restrict-nothing
  (is (= (f/select db (f/restrict app/TodoList true)) app/all-lists)))
(deftest test-restrict-everything
  (is (= (f/select db (f/restrict app/TodoItem false)) #{})))


(deftest test-summarize-agg-on-color
  (is (= (f/select db (f/summarize app/TodoList [:color] {:num-items [s/Int #(count %)]}))
         #{{:color "red" :num-items 1}
           {:color "blue" :num-items 2}})))
(deftest test-summarize-agg-all
  (is (= (f/select db (f/summarize app/TodoItem [] {:num-items [s/Int #(count %)]}))
         #{{:num-items (count app/all-items)}})))

(deftest test-join-commutative
  (is (= (f/select db (f/join app/TodoList app/TodoItem))
         (f/select db (f/join app/TodoItem app/TodoList)))))
(deftest test-join-foreign-keys
  (is (= (f/select db (f/join app/TodoList app/TodoItem))
         (set/join app/all-lists app/all-items {:id :list}))))
(deftest test-join-no-foreign-keys
  (is (= (f/select db (f/join app/Unrelated app/TodoItem)) #{}))
  (f/insert db app/Unrelated {:stuff "yoyo"})
  (is (= (f/select db (f/join app/Unrelated app/TodoItem))
         (set (map #(assoc % :stuff "yoyo") app/all-items)))))
#_(deftest test-join-projected-extended
  (is (= (f/select db (f/join (f/project app/TodoList :id)
                              (f/extend app/TodoItem :abc [s/Str "def"])))
         #{})))

(deftest test-minus
  (is (= (f/select db (f/minus app/TodoItem
                               (f/restrict app/TodoItem done?)))
         (set (filter #(not (:done? %)) app/all-items)))))

(deftest test-complex-max-size-list
  (is (= (f/select db
                   (f/summarize
                    (f/summarize app/TodoList [:color]
                                 {:num-items [s/Int #(count %)]})
                    [] #(f/maximum-key :num-items %)))
         #{{:color "blue" :num-items 2}})))
(deftest test-complex-list-num-items
  (is (= (f/select db (f/summarize (f/restrict app/TodoItem (= list "home"))
                                   [:list] {:num-items [s/Int #(count %)]}))
         #{{:list "home" :num-items (count app/home-items)}})))
(deftest test-complex-colors-num-items
  (is (= (f/select db (f/summarize (f/join app/TodoList app/TodoItem)
                                   [:color]
                                   {:num-items [s/Int #(count %)]}))
         #{{:color "red" :num-items (count app/home-items)}
           {:color "blue" :num-items (+ (count app/project-1-items)
                                            (count app/project-2-items))}})))
(deftest test-update-map-fn
  (let [g #(update-in % [:title] str "UPDATED")]
    (f/update db (f/restrict app/TodoList (= id "home")) {:title #(str % "UPDATED")})
    (is (= (f/select db (f/restrict app/TodoList (= id "home")))
           #{(g app/home-list)}))))
(deftest test-update-mult-const
  (let [g #(assoc-in % [:done?] false)]
    (f/update db (f/restrict app/TodoItem (= list "home")) {:done? false})
    (is (= (f/select db (f/restrict app/TodoItem (= list "home")))
           (set (map g app/home-items))))))
(deftest test-update-mult-fields
  (let [g #(-> % (assoc :done? false) (update :content (partial str "titlez")))]
    (f/update db (f/restrict [app/TodoItem :as ti] (= ti/list "project-1")) {:done? false :content (partial str "titlez")})
    (is (= (f/select db (f/restrict [app/TodoItem :as ti] (= ti/list "project-1")))
           (set (map g app/project-1-items))))))
(deftest test-delete-single-item
  (f/delete db (f/restrict app/TodoItem (= list "project-1")))
  (f/delete db (f/restrict app/TodoList (= id "project-1")))
  (is (= (f/select db (f/restrict app/TodoList (= id "project-1")))
         #{})))
(deftest test-delete-mult-items
  (f/delete db (f/restrict app/TodoItem (= list "project-1")))
  (is (= (f/select db (f/restrict app/TodoItem (= list "project-1")))
         #{})))

(deftest test-select-view
  (is (= (f/select db app/ListId) (set (map #(select-keys % [:id]) app/all-lists))))
  (is (= (f/select db app/ColoredItems) app/all-colored-items))
  (f/delete db app/TodoItem)
  (is (=  (f/select db app/ListId) (set (map #(select-keys % [:id]) app/all-lists))))
  (is (= (f/select db app/ColoredItems) #{})))

(deftest test-bad-schema-insert
  (is (thrown-with-msg? Exception #"Item does not match schema"
                        (f/insert db app/TodoList {:whatever :yoyos})))
  (is (thrown-with-msg? Exception #"Item does not match schema"
                        (f/insert db app/TodoList {:id :not-a-string :title "soemthing" :color "green"}))))
(deftest test-bad-schema-update
  (is (thrown-with-msg? Exception #"Item does not match schema"
                        (f/update db app/TodoList {:id keyword}))))

(deftest test-constraint-insert
  (try ((f/insert db app/TodoList {:id "empty-color-list" :title "hello" :color ""}))
       (is (true? false) "insert did not cause constraint violation")
       (catch ExceptionInfo e
         (is (= (ex-data e)
                {:explanation "No uncolored lists"}))))
  (is (= (f/select db (f/restrict app/TodoList (empty? color))) #{})))
(deftest test-constraint-update
  (try (f/update db app/TodoItem {:done? true})
       (is (true? false) "update did not cause constraint violation")
       (catch ExceptionInfo e
         (is (= (ex-data e)
                {:explanation "No more than 2 done items in a list"}))))
  (is (= (count (f/select db (f/restrict app/TodoItem done?))) 2)))
(deftest test-constraint-delete
  (try (f/delete db app/TodoList)
       (is (true? false) "delete did not cause constraint violation")
       (catch ExceptionInfo e
         (is (= (ex-data e)
                {:explanation "No items without lists"}))))
  (is (= (count (f/select db app/TodoList)) 3)))

(deftest test-observer-select
  (f/select db app/TodoItem)
  (is (= @hits 0))
  (is (= @last nil)))
(deftest test-observer-insert
  (f/insert db app/TodoItem {:list "project-2" :content "done" :done? true})
  (f/insert db app/TodoItem {:list "project-2" :content "not done" :done? false})
  (is (= @hits 1))
  (is  (= @last (f/select db (f/restrict app/TodoItem done?)))))
(deftest test-observer-update
  (is (= @hits 0))
  (f/insert db app/TodoItem {:list "project-2" :content "done" :done? true})
  (is (= @hits 1))
  (f/update db (f/restrict app/TodoItem (= content "done"))
            {:done? false})
  (is (= @hits 2))
  (f/update db (f/restrict app/TodoList (= id "project-1")) {:color "yellow"})
  (is (= @hits 2))
  (is (= @last (f/select db (f/restrict app/TodoItem done?)))))
(deftest test-obvserver-delete
  (f/delete db (f/restrict app/TodoItem (= list "project-1")))
  (f/delete db app/Unrelated)
  (is (= @hits 1))
  (is (= @last (f/select db (f/restrict app/TodoItem done?)))))
