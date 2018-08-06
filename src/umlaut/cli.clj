(ns umlaut.cli
  "Standalone GNU-style command-line interface.  Suitable for use with clj, deps.edn, standalone.jar etc"
  (:require [clojure.tools.cli :as cli]
            [clojure.java.io :as io])
  (:gen-class))

;use
(def cli-options
  [["-f" "--format FORMAT" "Umlaut output format, one of: dot, datomic, lacinia, graphql, spec"
    :id :format
    :parse-fn keyword
    :validate [#(#{:dot :datomic :lacinia :graphql  :spec} %) "Must be one of: dot, datomic, lacinia, graphql, spec"]]
   ["-i" "--input-dir DIR" "Umlaut files folder"
    :id :input-files
    :validate [#(.isDirectory (io/as-file %)) "Must be directory"]]
   ["-o" "--output DIR or FILE" "Output file (lacinia,graphql) or directory (dot, datomic, spec)"
    :id :output
    :validate [#(or (.isFile (io/as-file %))
                    (.isDirectory (io/as-file %))) "Must be directory or file" ]]])

(defn -main
  [& args]
  (cli/parse-opts args cli-options))
