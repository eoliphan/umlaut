(ns umlaut.all-tests
  (:gen-class)
  (:require [clojure.test :refer [run-tests]]
            [umlaut.core-parser-test :as core]
            [umlaut.dot-test :as dot]
            [umlaut.utils-test :as utils]
            [umlaut.lacinia-test :as lacinia]
            [umlaut.graphql-test :as graphql]
            [umlaut.datomic-test :as datomic]))

(defn -main [& args]
  (run-tests (find-ns 'umlaut.core-parser-test)
             (find-ns 'umlaut.dot-test)
             (find-ns 'umlaut.utils-test)
             (find-ns 'umlaut.lacinia-test)
             (find-ns 'umlaut.graphql-test)
             (find-ns 'umlaut.datomic-test)))
