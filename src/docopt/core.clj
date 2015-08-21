(ns docopt.core
  (:require [clojure.string      :as string])
  (:require [docopt.match        :refer [match-argv]])
  (:require [docopt.optionsblock :as option])
  (:require [docopt.usageblock   :as usage])
  (:require [docopt.util         :as util]))

(defn parse-docstring
  "Parses doc string."
  [docstring]
  (let [usage-pattern #"(?:^|\n)(?!\s).*(?i)usage:\s*(.*(?:\n(?=[ \t]).+)*)"
        options-pattern #"(?:^|\n)(?!\s).*(?i)options:\s*(.*(?:\n(?=[ \t]).+)*)"
        ops-split-fn (fn [options-block] (re-seq #"(?<=^|\n)\s*-.*(?:\s+[^- \t\n].*)*" options-block))
        fetch-section (fn [pattern split-fn docs]
                        (->> (re-find pattern docs)
                             (second) ; drop the Title of the section (Usage | Options)
                             (split-fn) ; split the text into lines
                             (map string/trim))) ; take away the trailing whitespaces
        usage-section (fetch-section usage-pattern string/split-lines docstring)
        options-section (fetch-section options-pattern ops-split-fn docstring)]
    (usage/parse usage-section
                 (option/parse options-section))))

(defn docopt
  "Parses doc string and matches command line arguments. The doc string may be omitted,
  in which case the metadata of '-main' is used"
  ([]
   (let [docstring (-> (ns-publics *ns*) ; get the public functions in ns
                       ('-main)
                       (meta)
                       (:doc))]
     (if (string? docstring)
       (docopt docstring *command-line-args*)
       (throw (Exception. "Docopt with NO argument requires -main to have docstring.\n")))))
  ([docstring]
    (docopt docstring *command-line-args*))
  ([docstring args]
    (match-argv (parse-docstring docstring) args)))
