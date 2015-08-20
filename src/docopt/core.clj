(ns docopt.core
  (:require [clojure.string      :as string])
  (:require [docopt.match        :refer [match-argv]])
  (:require [docopt.optionsblock :as option])
  (:require [docopt.usageblock   :as usage])
  (:require [docopt.util         :as util])
  (:import java.util.HashMap))

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
   (docopt *command-line-args*)
  ([args]
   (let [docstring (:doc (meta (find-var (symbol (pr-str (ns-name *ns*)) "-main"))))]
     (docopt docstring args)))
  ([doc args]
    (if (string? doc)
      (match-argv (parse-docstring doc) args)
      (throw (Exception. "Docopt with one argument requires that #'-main have a doc string.\n")))))
