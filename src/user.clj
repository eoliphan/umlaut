(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs refresh refresh-all]]
            [clojure.repl :refer :all]
            [clojure.test :refer :all]
            [umlaut.core-parser-test :as core]
            [umlaut.dot-test :as dot]
            [umlaut.utils-test :as utils]
            [umlaut.lacinia-test :as lacinia]
            [umlaut.graphql-test :as graphql]
            [umlaut.datomic-test :as datomic]
            [umlaut.generators.fixtures :as fixtures]))

(set-refresh-dirs "src")

(defn reset [] (refresh))
(defn tests []
  (reset)
  (run-tests (find-ns 'umlaut.core-parser-test)
             (find-ns 'umlaut.dot-test)
             (find-ns 'umlaut.utils-test)
             (find-ns 'umlaut.lacinia-test)
             (find-ns 'umlaut.graphql-test)
             (find-ns 'umlaut.datomic-test)))

(defn gen-fixtures [] (fixtures/gen-all))
