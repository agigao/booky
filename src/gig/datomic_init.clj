(ns gig.datomic-init
  (:require [datomic.api :as d]
            [clojure.edn :as edn]
            [clojure.instant :as instant]))

(def db-uri "datomic:mem://booky")

(d/create-database db-uri)

(def conn (d/connect db-uri))

@(def schema (edn/read-string (slurp "resources/schema.edn")))
3
@(d/transact conn schema)

;; Sample data
(def data
  [{:db/id "musil"
    :book/title "The Man Without Qualities"
    :book/author "Robert Musil"
    :book/genre "Philosophical Fiction"
    :book/publication-date (instant/read-instant-date "1943-11-06")}

   {:db/id "ulrich"
    :person/first-name "Ulrich"
    :person/last-name ""
    :person/email "ulrich@kakania.at"}

   {:db/id "agatha"
    :person/first-name "Agatha"
    :person/last-name ""
    :person/email "agatha@kakania.at"}

   {:registry/book "musil"
    :registry/patron "ulrich"
    :registry/borrower "agatha"
    :registry/borrow-date (instant/read-instant-date "2023-07-28")
    :registry/due-date (instant/read-instant-date "2023-10-28")}])

@(d/transact conn data)

(defn clean-db []
  (d/delete-database db-uri))

(comment
  (clean-db) 
  (d/create-database db-uri))
