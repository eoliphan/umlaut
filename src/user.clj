(ns user
  (:require [clojure.tools.namespace.repl :refer [set-refresh-dirs refresh refresh-all]]
            [clojure.repl :refer :all]
            [clojure.test :refer :all]
            [umlaut.core-parser-test ]
            [umlaut.dot-test ]
            [umlaut.utils-test ]
            [umlaut.lacinia-test ]
            [umlaut.graphql-test ]
            [umlaut.datomic-test ]
            [umlaut.core :as core]
            [umlaut.generators.dot :as dot]
            [umlaut.utils :as utils]
            [umlaut.generators.lacinia :as lacinia]
            [umlaut.generators.graphql :as graphql]
            [umlaut.generators.datomic :as datomic]
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
