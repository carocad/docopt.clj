(ns docopt.core-test
  (:require [clojure.data.json :as json])
  (:require [clojure.string    :as string])
  (:require [docopt.core       :refer [docopt]])
  (:require [clojure.test :refer [is]]))

(def ^:private test-section-regex #"(?s)r\"{3}(.*?)(?:\n{3})")
(def ^:private docstring-regex #"(?s)(?:r\"{3})(.*?)(?:\"{3})")
(def ^:private input-regex #"(?s)(?:\$ )(.*?)\n")
(def ^:private return-regex #"(?:\$.*\n)(\{.*\}|\"user-error\")")

(def ^:private test-file-url "https://raw.github.com/docopt/docopt/511d1c57b59cd2ed663a9f9e181b5160ce97e728/testcases.docopt")
(def ^:private test-file (slurp test-file-url))

(defn- make-inputs
  [section]
  (let [full-str       (map second (re-seq input-regex section))
        raw-arguments  (map #(string/split % #"\s*(\s+)") full-str)]
    (map rest raw-arguments)))

(defn- make-returns
  [section]
  (map second (re-seq return-regex section)))

(defn- user-error?
  [return-value]
  (= "user-error" return-value))

(defn- docopt-work?
  [docstring input return]
  (if (user-error? return)
    (is (= docstring docstring)) ;FIX-ME This should run a real test
    (is (= return return)))) ; FIX-ME this should run a real test

(defn- test-section
  [section]
  (let [docstring (first (map second (re-seq docstring-regex section)))
        input-collection (make-inputs section)
        return-collection (make-returns section)]
    (map docopt-work? (repeat docstring)
                      (map seq input-collection)
                      return-collection)))

; FIX-ME dont just take the first element but the whole collection
(defn- all-tests
  []
  (let [coll-sections (first (map first (re-seq test-section-regex test-file)))]
    (test-section coll-sections)))

(all-tests)
