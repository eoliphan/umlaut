(ns umlaut.generators.datomic-schema
  (:require [umlaut.core :as core]
            [camel-snake-kebab.core :as csk]
            [umlaut.utils :refer [annotations-by-space-key
                                  annotations-by-space-key-value
                                  annotations-by-space
                                  annotations-by-space
                                  annotation-comparer
                                  in?
                                  resolve-inheritance
                                  primitive?]]))



(def space "lang/datomic")
(def lacinia-space "lang/lacinia")

(defn- prepend [symb curr]
  "Creates a new list with symb at the first position."
  (list (symbol symb) curr))

(defn- process-datomic-type [attr type]
  "Converts type into a keyword if the type is not a primitive."
  (if (primitive? (attr :type-id))
    type
    (keyword type)))

(defn- convert-type [type]
  "Converts umlaut names to Lacinia names."
  (case type
    "Integer" :db.type/long
    "Float" :db.type/double
    "String" :db.type/string
    "ID" :db.type/uuid
    "DateTime" :db.type/instant
    "Boolean" :db.type/boolean

    :db.type/ref))

(defn- datomic-type [attr]
  "Transform a umlaut type into a datomic type."
  (let [type (process-datomic-type attr (convert-type (attr :type-id)))]
    (if (attr :required)
      (prepend "non-null" type)
      type)))

(defn- process-variable [attr]
  "Uses :artiy to determine if the variable is a list and builds the proper datomic structure."
  (if (= (attr :arity) [1 1])
    {:type (datomic-type attr)}
    (if (attr :required)
      {:type (prepend "non-null" (prepend "list" (datomic-type attr)))}
      {:type (prepend "list" (prepend "non-null" (datomic-type attr)))})))

(defn- process-params [params]
  "Builds the datomic args structure "
  (reduce (fn [acc param]
            (merge acc {(keyword (param :id)) (process-variable param)}))
          {} params))

(defn- check-add-field-documentation [field out]
  (if (contains? (field :field-annotations) :documentation)
    (merge out {:db/doc ((field :field-annotations) :documentation)})
    out))

(defn- check-add-field-deprecation [field out]
  (if (contains? (field :field-annotations) :deprecation)
    (merge out {:isDeprecated true :deprecationReason ((field :field-annotations) :deprecation)})
    (merge out {:isDeprecated false})))

(defn- check-add-params [field out]
  (if (field :params?)
    (merge out {:args (process-params (field :params))})
    out))

(defn- check-add-resolver [field out]
  (if (contains? (field :field-annotations) :others)
    (let [resolver (annotations-by-space-key space "resolver" ((field :field-annotations) :others))]
      (if (pos? (count resolver))
        (merge out {:resolve (keyword ((first resolver) :value))})
        out))
    out))

(defn- process-field [field]
  "Receives a method and add an entry in the fields map"
  (->> (process-variable (field :return))
       (check-add-field-documentation field)
       (check-add-field-deprecation field)
       (check-add-params field)
       (check-add-resolver field)))

(defn- process-declaration [info]
  "Thread several reduces to build a map of types, args, and resolvers"
  (as-> info acc
        (reduce (fn [acc method]
                  (merge acc {(keyword (method :id)) (process-field method)}))
                {} (info :fields))))

(defn- attr-to-values [info]
  (vec (info :values)))

(defn- attr-to-parents [info]
  (vec (map datomic-type (info :parents))))

(defn- check-add-documentation [node out]
  (let [docs (first (annotations-by-space :documentation (node :annotations)))]
    (if docs
      (merge out {:db/doc (:value docs)})
      out)))

(defn- add-field-type [node out]
  (let [type (convert-type ((node :return) :type-id))]
    (merge out {:db/valueType type})))

(defn- check-add-cardinality [node out]
  (let [arity ((node :return) :arity)]
    (if (not= arity [1 1])
      (merge out {:db/cardinality :db.cardinality/many})
      (merge out {:db/cardinality :db.cardinality/one}))))

(defn- check-add-unique [node out]
  "Add unique attribute for id's"                           ;todo: handle this in concert with annotations we're adding
  (let [type ((node :return) :type-id)]
    (if (= type "ID")
      (merge out {:db/unique :db.unique/identity})
      out)))

(defn- check-add-deprecation [node out]
  (let [deprecation (first (annotations-by-space :deprecation (node :annotations)))]
    (if deprecation
      (merge out {:isDeprecated true :deprecationReason (deprecation :value)})
      (merge out {:isDeprecated false}))))

;(defn- gen-node-type [node]
;  (when (= (first node) :type)
;    (let [info (second node)]
;      (->> {:fields (process-declaration info)
;            :implements (attr-to-parents info)}
;           (check-add-documentation info)
;           (assoc {} (keyword (info :id)))))))

(defn- gen-node-type [node]
  (when (= (first node) :type)
    (let [info (second node)
          prefix (csk/->kebab-case-string (info :id))]
      (->> (map (fn [val]
                  (->> {:db/ident (keyword prefix (val :id))
                        :db/index true}
                       (add-field-type val)
                       (check-add-cardinality val)
                       (check-add-field-documentation val)
                       (check-add-unique val)

                       ))
                (info :fields))
           ))))

;(defn- gen-node-enum [node]
;  (when (= (first node) :enum)
;    (let [info (second node)]
;      (->> {:values (attr-to-values info)}
;           (check-add-documentation info)
;           (assoc {} (keyword (info :id)))))))
(defn- gen-node-enum [node]
  (when (= (first node) :enum)
    (let [info (second node)
          prefix (csk/->kebab-case-string (info :id))]
      (->> (map (fn [val] {:db/ident (keyword prefix val)}) (info :values))
           (map #(check-add-documentation info %))))))

(defn- gen-node-interface [node]
  (when (= (first node) :interface)
    (let [info (second node)]
      (->> {:fields (process-declaration info)}
           (check-add-documentation info)
           (assoc {} (keyword (info :id)))))))

(defn- gen-query-type [node]
  (let [info (second node)]
    (check-add-documentation info (process-declaration info))))

(defn- gen-union-type [node]
  (when (= (first node) :enum)
    (let [info (second node)]
      (->> {:members (vec (map keyword (info :values)))}
           (check-add-documentation info)
           (assoc {} (keyword (info :id)))))))

(defn- filter-lacinia-input-nodes [nodes]
  (filter (annotation-comparer lacinia-space "identifier" "input") nodes))

(defn- filter-lacinia-mutation-nodes [nodes]
  (filter (annotation-comparer lacinia-space "identifier" "mutation") nodes))

(defn- filter-lacinia-query-nodes [nodes]
  (filter (annotation-comparer lacinia-space "identifier" "query") nodes))

(defn- filter-lacinia-union-nodes [nodes]
  (filter (annotation-comparer lacinia-space "identifier" "union") nodes))



(defn- build-ignored-list [nodes]
  (flatten (list
             (map first (filter-lacinia-input-nodes nodes))
             (map first (filter-lacinia-mutation-nodes nodes))
             (map first (filter-lacinia-union-nodes nodes))
             (map first (filter-lacinia-query-nodes nodes)))))

(defn- filter-other-nodes [nodes]
  (let [all (build-ignored-list nodes)]
    (filter #(not (in? (first %) all)) nodes)))

(defn gen [files]
  "Returns a list of Datomic schema maps"
  (let [umlaut (resolve-inheritance (core/main files))
        nodes-seq (seq (umlaut :nodes))]
    (into [] (filter some?
            (flatten (conj (map gen-node-enum (map #(second %) (filter-other-nodes nodes-seq)))
                           (map gen-node-type (map #(second %) (filter-other-nodes nodes-seq)))))))
    ;(as-> nodes-seq coll
    ;      (reduce
    ;        (fn [acc [key node]]
    ;          (merge acc {:objects    (or (merge (acc :objects) (gen-node-type node)) {})
    ;                      :enums      (or (merge (acc :enums) (gen-node-enum node)) {})
    ;                      :interfaces (or (merge (acc :interfaces) (gen-node-interface node)) {})
    ;
    ;                      }))
    ;        {} (filter-other-nodes nodes-seq))
    ;      ;(reduce
    ;      ;  (fn [acc [key node]]
    ;      ;    (merge acc {:input-objects (or (merge (acc :input-objects) (gen-node-type node)) {})}))
    ;      ;  coll (filter-input-nodes nodes-seq))
    ;      ;(reduce
    ;      ;  (fn [acc [key node]]
    ;      ;    (merge acc {:mutations (or (merge (acc :mutations) (gen-query-type node)) {})}))
    ;      ;  coll (filter-mutation-nodes nodes-seq))
    ;      ;(reduce
    ;      ;  (fn [acc [key node]]
    ;      ;    (merge acc {:unions (or (merge (acc :unions) (gen-union-type node)) {})}))
    ;      ;  coll (filter-union-nodes nodes-seq))
    ;      ;(reduce
    ;      ;  (fn [acc [key node]]
    ;      ;    (merge acc {:queries (or (merge (acc :queries) (gen-query-type node)) {})}))
    ;      ;  coll (filter-query-nodes nodes-seq))
    ;
    ;      )
    ))

