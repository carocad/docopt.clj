(ns docopt.core
  (:require [instaparse.core :as insta]
            [instaparse.transform :as t]
            [instaparse.combinators-source :as combi]
            [instaparse.cfg :as cfg]
            [instaparse.abnf :as abnf]))

(def docopt-parser (insta/parser "
  <DOCSTRING> = <DESCRIPTION?> USAGE OPTIONS?
  DESCRIPTION = !USAGE #'(?si).*?' <EOL> (* anything except the usage section*)

  usage-begin = <#'(?i)usage:'> <EOL>
  usage-line = !OPTIONS app-name expression+ <EOL | #'$'>
  USAGE = <usage-begin> usage-line+

  options-begin = <#'(?i)options:'> <EOL>
  OPTIONS = <options-begin> option-line+
  option-line = short-option? long-option? <option-desc> option-default? <EOL | #'$'>

  <expression> = option-name / option-key / argument / command /
                 required / optional / exclusive / multiple
  required = <'('> expression <')'>        (* grouped-required expressions *)
  optional = <'['> expression <']'>        (* grouped-optinal expressions *)
  exclusive = expression <OR> expression
  multiple = expression <ellipsis>

  app-name = #'\\S+'
  argument = #'<\\S+>'
  command = #'\\w+'

  option-key = #'-([a-zA-Z])'
  option-name = #'--[a-zA-Z]{2,}'     (* two or more characters required for option names *)
  option-arg = #'(=| )(<[a-z_]+>|[A-Z_]+)'
  option-desc = !(short-option | long-option) #'\\w+'+
  option-default = <'[default:'> #'\\w+' <']'>
  short-option = option-key option-arg?
  long-option = option-name option-arg?

  ellipsis = '...'  (* one or more elements *)
  OR = '|'          (* mutually exclusive expressions *)

  <EOL> = #'(\\n|\\r)+'          (* end of line but not of the string *)
  " :auto-whitespace (insta/parser "whitespace = #'[\\ |\\t]+' (* whitespace *)")))

(insta/parses docopt-parser
  "Usage:
   prog.py [--count] --PATH --FILE...

   Options:
    --FILE     input file[default:foo]
    --PATH     out directory
    --count=N   number of operations")
  ;:total true)

(insta/parses docopt-parser "
  Usage:
    quick_example.py tcp <host> <port> [--timeout]
    quick_example.py serial <port> [--baud] [--timeout]
    quick_example.py -h | --help | --version")

;(def ^:private matcher
;  {:full-option #(str "<'" %1 "'|'" %2 "'> #'\\S+'")
;   :both-option #(str "'" %1 "'|'" %2 "'")
;   :opt+arg     #(str "<'" % "'> #'\\S+'")
;   :opt         identity
;   :argument    #(str "\\S+")
;   :command     #(str %)
;   :refer-to    #(keyword %)})
;
;(defn- tag-match?
;  [element tags]
;  ((first element) (apply hash-set tags)))
;
;;TO-THINK: if I'm sure that there is a short option, why don't replace it inmediately as a combinator?
;(defn- make-option
;  "create a hash-map of name-instaparse.combinator based on the different kind of long/short options and if
;  an option accepts arguments."
;  [{:keys [short-option long-option option-arg]}]
;  (let [short-key (keyword short-option)
;        long-key  (keyword long-option)]
;    (cond
;     (and long-option short-option option-arg) (hash-map long-key  (cfg/ebnf ((matcher :full-option) long-option short-option))
;                                                         short-key ((matcher :refer-to) long-option))
;     (and long-option short-option)            (hash-map long-key  (cfg/ebnf ((matcher :both-option) long-option short-option))
;                                                         short-key ((matcher :refer-to) long-option))
;     (and long-option option-arg)              (hash-map long-key  (cfg/ebnf ((matcher :opt+arg) long-option)))
;     (and short-option option-arg)             (hash-map short-key (cfg/ebnf ((matcher :opt+arg) short-option)))
;     long-option                               (hash-map long-key  (combi/string ((matcher :opt) long-option)))
;     short-option                              (hash-map short-key (combi/string ((matcher :opt+arg) short-option))))))
;
;(defn- make-element
;  [content]
;  {(keyword content) (combi/string content)})
;
;(defn- make-argument
;  [content]
;  {(keyword content) (combi/regexp "\\S+")})
;
;(defn- fetch-elements
;  "traverse the usage-tree, extract the interesting tags and group them by tag name"
;  [usage-tree & target-tag]
;  (->> (tree-seq vector? identity usage-tree)
;       (filter #(and (vector? %) (tag-match? % target-tag)))
;       (group-by #(first %))))
;
;(defn- elements-combinators
;  "based on the parsed usage and options section, get the commands, arguments
;  and options. Each element is transformed into a key-value pair with the key
;  its name (<one> --two -t ...), each value is the corresponding
;  instaparse.combinators to be matched against."
;  [usage-tree options-tree]
;  (let [option-lines  (map rest (rest options-tree))
;        elements      (fetch-elements usage-tree :name :command :argument)
;        fetch-all     (fn [tag] (into (hash-set) (map second (tag elements))))
;        names         (fetch-all :name) ; TODO: check that only one element is here
;        commands      (map make-element (fetch-all :command))
;        arguments     (map make-argument (fetch-all :argument))
;         ;usg-options   (into (fetch-all :long-option)
;         ;                    (fetch-all :short-option))
;        options       (map make-option (map #(into (hash-map) %) option-lines))]
;    (into {} (apply concat commands arguments options))))
;
;(defn- translate-grammar
;  "conver the docopt argument-parsing language-grammar into EBNF notation using
;  instaparse.combinators. The complete Usage section is converted into an flat
;  alternation (use1 | use2 | ...) based on each usage line."
;  [parsed-usage short->long-option]
;  (t/transform
;     {:command         #(combi/nt (keyword %))
;      :argument        #(combi/nt (keyword %))
;      :short-option    #(combi/nt ((keyword %) short->long-option))
;      :long-option     (fn [& args] (combi/nt (keyword (first args))))
;      :multiple        #(combi/plus %)
;      :required        #(combi/cat %) ; LATER: does this really work?
;      :optional        #(combi/opt %)
;      :exclusive       #(apply combi/alt %&)
;      :usage-line      #(combi/hide-tag (apply combi/cat (rest %&))) ; drop the name of the program & ignore the use-tag
;      :USAGE           (fn [& args]
;                         (let [use-names     (map #(keyword (str "use" %)) (range (count args)))
;                               use-nt        (map combi/nt use-names)
;                               start-content (apply combi/alt use-nt)
;                               start         (hash-map :USAGE start-content)
;                               use-cases     (apply merge (map hash-map use-names args))]
;                           (merge start use-cases)))}
;     parsed-usage))
;
;(defn- argument-grammar
;  "Creates a grammar specification hash-map for instaparse/parser to work with."
;  [docstring]
;  (let [docopt-parser        (insta/parser docopt-language-grammar
;                                           :auto-whitespace (insta/parser "whitespace = #'[\\ |\\t]+'"))
;        grammar-tree         (docopt-parser docstring)
;        all-elements         (elements-combinators (first grammar-tree) (second grammar-tree))
;        main-elements        (into {} (filter #(not (keyword? (second %))) (seq all-elements)))
;        short->long-option   (into {} (filter #(keyword? (second %)) (seq all-elements)))]
;    ;(insta/transform {:USAGE #(into (hash-map) %&)}
;    (into (translate-grammar (first grammar-tree) short->long-option)
;          main-elements)))
;
;(defn docopt
;  "Parses docopt argument-parsing language and returns a hash-map with the
;  command line arguments according to the described use cases.
;  If no docstring is provided, it defaults to -main :doc metadata.
;  If no arguments are given, clojure's *command-line-args* are used."
;  ([]
;   (let [docstring (-> (ns-publics *ns*) ; get the public functions in ns
;                       ('-main)
;                       (meta)
;                       (:doc))]
;       (docopt docstring *command-line-args*)))
;  ([docstring]
;   (docopt docstring *command-line-args*))
;  ([docstring args]
;   (let [arg-tree    (argument-grammar docstring)
;         arg-parser  (insta/parser arg-tree :start :USAGE :auto-whitespace :standard)
;         parsed-argv (arg-parser (clojure.string/join " " args))]
;     (t/transform {:USAGE #(into (hash-map) %&)}  parsed-argv))))
