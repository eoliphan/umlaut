(ns umlaut.main
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [expound.alpha :as expound]
            [umlaut.core :as core]
            [umlaut.generators.datomic :as datomic]
            [umlaut.generators.dot :as dot]
            [umlaut.generators.graphql :as graphql]
            [umlaut.generators.lacinia :as lacinia]
            [umlaut.generators.spec :as spec]
            [umlaut.utils :as utils]))

(def ^:private version
  (try
    (slurp (io/resource "version"))
    (catch Throwable t
      "N/A")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Specs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::gen #{'dot 'datomic 'graphql 'lacinia 'spec})

(s/def ::base-payload (s/keys :req-un [::gen ::in ::out]))

(defmulti generator :gen)
(defmethod generator 'dot [_] ::base-payload)
(defmethod generator 'datomic [_] ::base-payload)
(defmethod generator 'graphql [_] ::base-payload)
(defmethod generator 'lacinia [_] ::base-payload)
(defmethod generator 'spec [_]
  (s/merge ::base-payload
           (s/keys :req-un [::spec-package ::custom-validators-filepath ::id-namespace])))

(s/def ::payload (s/and #(not (nil? %))
                        #(s/valid? ::gen (:gen %))
                        (s/multi-spec generator ::gen)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cli options
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private cli-options
  [["-g" "--gen GENERATOR" "One of dot, datomic, graphql, lacinia, or spec"
    :parse-fn symbol]
   ["-i" "--in PATH" "The input path where your Umlaut files are located."]
   ["-o" "--out PATH" "The output path where your generated files will be saved."]
   [nil "--spec-package" "Spec gen only - the package name to be used."]
   [nil "--custom-validators-filepath" "Spec gen only - the path for the custom validator."]
   [nil "--id-namespace" "Spec gen only - namespace that identify ids i.e. :db/id"]
   ["-e" "--edn EDN" "Options in EDN format. Overrides all other options if provided."
    :parse-fn edn/read-string]
   ["-v" "--version" "Show the installed umlaut version"]
   ["-h" "--help" "Shows this help."]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Internal utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- join-path [out filename]
  "Join two paths"
  (-> out
      (io/file filename)
      (.getPath)))

(defn- get-umlaut-files [in]
  "Returns a list of .umlaut files from input folder"
  (->> in
       (io/file)
       (file-seq)
       (map #(.getPath ^java.io.File %))
       (filter #(str/ends-with? % ".umlaut"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Processing methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process (fn [{:keys [gen]}] gen))

(defmethod process 'dot [{:keys [in out]}]
  (let [umlaut (core/run (get-umlaut-files in))]
    (utils/save-dotstring-to-image (join-path out "all.png") (dot/gen-all umlaut))
    (utils/save-string-to-file (join-path out "all.dot") (dot/gen-all umlaut))
    (reduce (fn [acc [key value]]
              (utils/save-dotstring-to-image (join-path out (str key ".png")) value)
              (utils/save-string-to-file (join-path out (str key ".dot")) value))
            {} (seq (dot/gen-by-group umlaut)))))

(defmethod process 'datomic [{:keys [in out]}]
  (utils/save-map-to-file out (datomic/gen (get-umlaut-files in))))

(defmethod process 'lacinia [{:keys [in out]}]
  (utils/save-map-to-file out (lacinia/gen (get-umlaut-files in))))

(defmethod process 'graphql [{:keys [in out]}]
  (utils/save-map-to-file out (graphql/gen (get-umlaut-files in))))

(defmethod process 'spec [{:keys [in out
                                  spec-package
                                  custom-validators-filepath
                                  id-namespace]}]
  (let [specs (spec/gen spec-package custom-validators-filepath
                        id-namespace (get-umlaut-files in))]
    (doseq [[k v] specs]
      (let [filename (str/replace k #"-" "_")]
        (utils/save-string-to-file (join-path out (str filename ".clj")) v)))))

(defmethod process :default [{:keys [gen]}]
  (throw (ex-info "Invalid generator provided." {:gen gen})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- exit [code]
  (System/exit code))

(defn- print-summary [summary]
  (->> ["umlaut - A neutral schema parsing tool that outputs diagrams and code."
        ""
        "Native image syntax.....: $ umlaut [options]"
        "Clojure runner syntax...: $ clojure -m umlaut.main [options]"
        ""
        "Options:"
        "" summary ""
        "Docs at https://github.com/workco/umlaut"
        ""]
       (str/join "\n")
       println))

(defn- help-exit [summary]
  (print-summary summary)
  (exit 0))

(defn- version-exit []
  (println version)
  (exit 0))

(defn- check-and-process [payload]
  (if (s/valid? ::payload payload)
    (do (process payload)
        (exit 0))
    (do (println (expound/expound ::payload payload))
        (exit 1))))

(defn -main
  [& args]
  (let [{:keys [options summary] {:keys [help version edn gen]} :options}
        (cli/parse-opts args cli-options)]
    (if (or help (empty? options)) 
      (help-exit summary)
      (if version
        (version-exit)
        (check-and-process (or edn options))))))
