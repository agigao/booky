(ns gig.datomic-p1
  (:require [datomic.api :as d]
            [gig.datomic-init :refer [db]]))

(d/q '[:find ?e ?first-name ?last-name ?email
       :keys id first-name last-name email
       :where
       [?e :person/first-name ?first-name]
       [?e :person/last-name ?last-name]
       [?e :person/email ?email]]
     db)

(d/q '[:find [(pull ?e [*]) ...]
       :where [?e :person/first-name]]
     db)

(let [entity (d/entity db 17592186045420)]
  (select-keys entity [:person/first-name
                       :person/last-name
                       :person/email]))

;; Query
(d/q '[:find ?title ?borrow-date ?borrower-name ?patron-name
       :keys title borrow-date borrower patron
       :where
       [?registry :registry/book ?book]
       [?registry :registry/borrow-date ?borrow-date]
       [?registry :registry/borrower ?borrower]
       [?registry :registry/patron ?patron]
       [?book :book/title ?title]
       [?borrower :person/first-name ?borrower-name]
       [?borrower :person/email "agatha@kakania.at"]
       [?patron :person/first-name ?patron-name]]
     db)

