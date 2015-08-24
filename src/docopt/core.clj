(ns docopt.core
  (:require [clojure.string      :as string])
  (:require [docopt.match        :as match])
  (:require [docopt.optionsblock :as option])
  (:require [docopt.usageblock   :as usage])
  (:require [clojure.test :refer [is]]))

(def ^:private usage-regex #"(?:^|\n)(?!\s).*(?i)usage:\s*(.*(?:\n(?=[ \t]).+)*)")
(def ^:private options-regex #"(?:^|\n)(?!\s).*(?i)options:\s*(.*(?:\n(?=[ \t]).+)*)")
(def ^:private option-line-regex #"(?<=^|\n)\s*-.*(?:\s+[^- \t\n].*)*")

(defn- split-options
  [options-block]
  (re-seq option-line-regex options-block))

(defn- get-section
  [pattern split-fn docs]
  (->> (re-find pattern docs)
       (second) ; drop the Title of the section (Usage | Options)
       (split-fn) ; split the text into lines
       (map string/trim))) ; take away the trailing whitespaces

(defn parse-docstring
  "Parses docstring."
  [docstring]
  {:pre [(is (string? docstring) "Docopt requires -main to have docstring")]}
  (let [usage-section (get-section usage-regex string/split-lines docstring)
        options-section (get-section options-regex split-options docstring)]
        (usage/parse usage-section (option/parse options-section))))

(defn docopt
  "Parses doc string and matches command line arguments. The doc string may be omitted,
  in which case the metadata of '-main' is used"
  ([]
   (let [docstring (-> (ns-publics *ns*) ; get the public functions in ns
                       ('-main)
                       (meta)
                       (:doc))]
       (docopt docstring *command-line-args*)))
  ([docstring]
    (docopt docstring *command-line-args*))
  ([docstring args]
    (match/associate (parse-docstring docstring) args)))
