(ns umlaut.lacinia-test
  (:require [clojure.test :refer :all]
            [umlaut.core :as core]
            [umlaut.generators.lacinia :as lacinia]
            [clojure.data :as data]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]))
(use '[clojure.pprint :only [pprint]])

(def fixture (read-string (slurp "test/fixtures/person/lacinia.fixture")))
(def resolvers
  {:resolve-name (constantly "name")
   :resolve-mutation (constantly "mutate")
   :resolve-unionquery (constantly "union")
   :resolve-getfriends (constantly "getfriends")})

(def streamers
  {:stream-messages (constantly "msg")
   :stream-logs (constantly "msg")})

(defn validate-fixture
  "TODO: Document me"
  []
  (-> "fixtures/person/lacinia.fixture"
      io/resource
      slurp
      edn/read-string
      (util/attach-resolvers resolvers)
      (util/attach-streamers streamers)
      schema/compile))


(deftest lacinia-test
  (testing "Lacinia generator test"
    (let [lacinia-schema (lacinia/gen ["test/fixtures/person/person.umlaut" "test/fixtures/person/profession.umlaut"])
          diff (data/diff fixture
                          lacinia-schema)]
      (is (some? (validate-fixture)))
      (is (and
           (nil? (first diff))
           (nil? (second diff)))))))

(run-all-tests)
