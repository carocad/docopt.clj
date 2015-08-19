(ns docopt.core
  (:require [clojure.string      :as string])
  (:require [docopt.match        :as match])
  (:require [docopt.optionsblock :as option])
  (:require [docopt.usageblock   :as usage])
  (:require [docopt.util         :as util])
  (:import java.util.HashMap))

(defn parse-docstring
  "Parses doc string."
  [doc]
  (letfn [(sec-re   [name]          (re-pattern (str "(?:^|\\n)(?!\\s).*(?i)" name ":\\s*(.*(?:\\n(?=[ \\t]).+)*)")))
          (section  [name splitfn]  (map string/trim (mapcat (comp splitfn second) (re-seq (sec-re name) doc))))
          (osplitfn [options-block] (re-seq #"(?<=^|\n)\s*-.*(?:\s+[^- \t\n].*)*" options-block))]
    (usage/parse (section "usage" string/split-lines) (option/parse (section "options" osplitfn)))))

(defn docopt
  "Parses doc string at compile-time and matches command line arguments at run-time.
The doc string may be omitted, in which case the metadata of '-main' is used"
  ([args]
    (let [doc (:doc (meta (find-var (symbol (pr-str (ns-name *ns*)) "-main"))))]
      (if (string? doc)
        (match/match-argv (parse-docstring doc) args)
        (throw (Exception. "Docopt with one argument requires that #'-main have a doc string.\n")))))
  ([doc args]
    (match/match-argv (parse-docstring doc) args)))
