(defproject umlaut "0.5.3"
  :description "A Clojure program that receives a schema and outputs code."
  :url "https://github.com/workco/umlaut"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]

                 [camel-snake-kebab "0.4.0"]
                 [instaparse "1.4.9"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [com.walmartlabs/lacinia "0.28.0"]]
  :plugins [[lein-cljfmt "0.5.6"]
            [lein-kibit "0.1.5"]]
  :deploy-repositories {"clojars" {:sign-releases false}}
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
