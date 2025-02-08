(ns gig.datomic-p2
  (:require [clojure.pprint :refer [print-table]]
            [datomic.api :as d]
            [gig.datomic-init :refer [conn]]
            [tick.core :as t]))

;; Add status to out schema
(def status-schema
  [{:db/ident       :book/status
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc         "Current status of the book: :available, :borrowed, :lost"}])

@(d/transact conn status-schema)

(def musil-id
  (ffirst
    (d/q '[:find ?e
           :where [?e :book/title "The Man Without Qualities"]]
         (d/db conn))))

(do
  @(d/transact conn [[:db/add musil-id :book/status :available]])

  ;; mark it as borrowed
  (Thread/sleep 5000)
  @(d/transact conn [[:db/add musil-id :book/status :borrowed]])

  ;; mark it as lost
  (Thread/sleep 5000)
  @(d/transact conn [[:db/add musil-id :book/status :lost]]))

(defn book-status-history [title]
  (let [db (d/history (d/db conn))]                         ; Get historical database view with all past states
    (->> (d/q '[:find ?status ?inst ?op                     ; Select status, timestamp and operation type
                :in $ ?title                                ; Input: database and book title
                :where
                [?e :book/title ?title]                     ; 1. Find entity with matching title
                [?e :book/status ?status ?tx ?op]           ; 2. Get its status changes
                [?tx :db/txInstant ?inst]]                  ; 3. Get timestamps for changes
              db title)                                     ; Pass history db and title to query
         (map (fn [[status inst op]]                        ; Transform each result tuple into a map
                {:status    status
                 :when      inst
                 :operation (if (= op true) :assert :retract)}))
         (sort-by :when))))


(print-table (book-status-history "The Man Without Qualities"))

"
|    :status |                        :when | :operation |
|------------+------------------------------+------------|
| :available | Sat Feb 08 19:41:04 GET 2025 |    :assert |
| :available | Sat Feb 08 19:41:09 GET 2025 |   :retract |
|  :borrowed | Sat Feb 08 19:41:09 GET 2025 |    :assert |
|  :borrowed | Sat Feb 08 19:41:14 GET 2025 |   :retract |
|      :lost | Sat Feb 08 19:41:14 GET 2025 |    :assert |
"

;; Point-in-Time Queries
(defn library-snapshot [instant]
  (let [db-at-point (d/as-of (d/db conn) instant)]          ; Get database as it existed at that instant
    (d/q '[:find ?title ?status ?borrower                   ; Select title status and borrower
           :keys book status borrower                       ; Return them in a map with these keys
           :where
           [?b :book/title ?title]                          ; Start with book entity
           [?b :book/status ?status]                        ; Get its status
           [?r :registry/book ?b]                           ; Join with registry using book
           [?r :registry/borrower ?br]                      ; Get borrower from registry
           [?br :person/first-name ?borrower]]              ; Get borrower's name
         db-at-point)))

;; Create an atom to store our timeline
(def timeline (atom {}))

;; Create a sequence of events and record their times
(let [;; Initial state - book is available
      _ @(d/transact conn [[:db/add musil-id :book/status :available]])
      t1 (t/inst)
      _ (swap! timeline assoc :available t1)
      _ (Thread/sleep 2000)                                 ; Wait 2 seconds

      ;; Someone borrows the book
      _ @(d/transact conn [[:db/add musil-id :book/status :borrowed]])
      t2 (t/inst)
      _ (swap! timeline assoc :borrowed t2)
      _ (Thread/sleep 2000)                                 ; Wait 2 seconds

      ;; Unfortunately, book gets lost
      _ @(d/transact conn [[:db/add musil-id :book/status :lost]])
      t3 (t/inst)
      _ (swap! timeline assoc :lost t3)]

  ;; Look at different points in time
  {:at-start      (library-snapshot t1)
   :when-borrowed (library-snapshot t2)
   :when-lost     (library-snapshot t3)})

;; => {:at-start
;;     [{:book "The Man Without Qualities", :status :available, :borrower "Agatha"}],
;;     :when-borrowed
;;     [{:book "The Man Without Qualities", :status :borrowed, :borrower "Agatha"}],
;;     :when-lost
;;     [{:book "The Man Without Qualities", :status :lost, :borrower "Agatha"}]}

(library-snapshot
  (t/inst (t/<< (@timeline :borrowed)
                (t/new-duration 1 :seconds))))
;; => [{:book "The Man Without Qualities", :status :available, :borrower "Agatha"}]

;; Query 1 second after the book was borrowed
(library-snapshot
  (t/inst (t/>> (@timeline :borrowed)
                (t/new-duration 1 :seconds))))
;; => [{:book "The Man Without Qualities", :status :borrowed, :borrower "Agatha"}]

;; Or look at the state just before it was lost
(library-snapshot
  (t/inst (t/<< (@timeline :lost)
                (t/new-duration 1 :seconds))))
;; => [{:book "The Man Without Qualities", :status :borrowed, :borrower "Agatha"}]

;; The Power of History
(defn borrowing-history [book-title]
  (let [db (d/history (d/db conn))]
    (->> (d/q '[:find ?borrower ?status ?inst               ; Select borrower, status, timestamp
                :in $ ?title                                ; Input: database and book title
                :where
                [?b :book/title ?title]                     ; Find book by title
                [?b :book/status ?status ?tx true]          ; Get only assertions (added=true) of status changes
                [?r :registry/book ?b]                      ; Find registry entry
                [?r :registry/borrower ?br]                 ; Get borrower entity
                [?br :person/first-name ?borrower]          ; Get borrower's name
                [?tx :db/txInstant ?inst]]                  ; Get timestamp of transaction
              db book-title)
         (map (fn [[borrower status inst]]
                {:borrower  borrower
                 :status    status
                 :when      inst
                 :operation "assert"}))
         (sort-by :when))))

(print-table (borrowing-history "The Man Without Qualities"))

"
| :borrower |    :status |                        :when | :operation |
|-----------+------------+------------------------------+------------|
|    Agatha | :available | Sat Feb 08 19:41:04 GET 2025 |     assert |
|    Agatha |  :borrowed | Sat Feb 08 19:41:09 GET 2025 |     assert |
|    Agatha |      :lost | Sat Feb 08 19:41:14 GET 2025 |     assert |
|    Agatha | :available | Sat Feb 08 20:14:08 GET 2025 |     assert |
|    Agatha |  :borrowed | Sat Feb 08 20:14:10 GET 2025 |     assert |
|    Agatha |      :lost | Sat Feb 08 20:14:12 GET 2025 |     assert |
"

