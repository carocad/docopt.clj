(ns docopt.core
  (:require [instaparse.core :as insta]
            [instaparse.transform :as t]
            [instaparse.combinators-source :as combi]
            [instaparse.cfg :as cfg]
            [instaparse.abnf :as abnf]
            [clojure.string :as str]))

(def docopt-parser (insta/parser "
  <DOCSTRING> = <DESCRIPTION?> USAGE OPTIONS?
  DESCRIPTION = !USAGE #'^(?si)(.*?)(?=usage:)'

  usage-begin = <#'(?i)usage:'> <EOL>
  usage-line = !OPTIONS app-name expression* <EOL | #'$'>
  USAGE = <usage-begin> usage-line+

  options-begin = <#'(?i)options:'> <EOL>
  OPTIONS = <options-begin> option-line+
  option-line = short-option? long-option? <option-desc> option-default? <EOL | #'$'>

  <expression> = option-name / option-key / argument / command /
                 required / optional / exclusive / multiple
  required = <'('> expression+ <')'>        (* grouped-required expressions *)
  optional = <'['> expression+ <']'>        (* grouped-optinal expressions *)
  exclusive = expression <OR> expression
  multiple = expression <ellipsis>

  app-name = #'\\S+'
  argument = #'<\\S+>'
  command = #'\\w+'

  option-key = #'-[a-zA-Z]'
  option-name = #'--[a-zA-Z]{2,}'     (* two or more characters required for option names *)
  option-arg = #'(=| )(<[a-z_]+>|[A-Z_]+)'
  option-desc = !(short-option | long-option) #'[\\w\\.\\(\\)]+'+
  option-default = <'[default:'> #'\\w+' <']'> <'.'>?
  short-option = option-key option-arg?
  long-option = option-name option-arg?

  ellipsis = '...'  (* one or more elements *)
  OR = '|'          (* mutually exclusive expressions *)

  <EOL> = #'(\\n|\\r)+'          (* end of line but not of the string *)
  " :auto-whitespace (insta/parser "whitespace = #'[\\ \\t]+'")))

(defn- usage-rules
  "convert the command and arguments defined in the parsed USAGE section to EBNF
   notation using instaparse.combinators."
  [usage]
  (let [elements (into #{} (comp (filter vector?)
                                 (filter (comp #{:command :argument} first)))
                           (tree-seq vector? identity usage))]
    (into {} (for [[k name] elements]
               (if (= k :command) ; else :argument
                 [(keyword name) (combi/string name)]
                 [(keyword name) (combi/regexp "\\w+")])))))

(defn- optionize
  "transforms an option (short or long) into a tuple of
   [keywordized-name EBNF-combinator]. If the option accepts an argument,
   an extra regex match is added"
  ([name] ;; option with no arg
   [(keyword name) (combi/string name)])
  ([name _] ;; option with arg
   [(keyword name)
    (combi/cat (combi/hide (combi/regexp (str name "[=:]")))
               (combi/regexp "\\S+"))]))

(defn- opt-combi
  "transforms an option-line into a 2-tuple {key ebnf-parser}. If an option
  line contains both short and long options, the short option is a simple
  reference to the long one. Default option values are ignored"
  [& elements]
  (let [rel (remove nil? elements)] ;; nil = sentinel for default value
    (if (= 1 (count rel)) [(apply hash-map (first rel))] ;; 2 = long and short option
      (let [pars (apply combi/alt (map second rel))
            name (first (second rel))]
        [{name pars} ;; main match
         {(ffirst rel) (combi/hide-tag (combi/nt name))}]))))

(defn- options-rules
  "convert the parsed OPTIONS section into EBNF notation using instaparse.combinators.
  Each option line is transformed according to opt-combi and optionize rules"
  [options]
  (t/transform
    {:option-name    identity
     :option-key     identity
     :option-default #{} ;; TODO handle default values properly
     :option-arg     #(subs % 1)
     :short-option   optionize
     :long-option    optionize
     :option-line    opt-combi ;(fn [& v] v)
     :OPTIONS        (fn [& v] (apply merge (mapcat identity v)))}
    options))

(defn- non-terminal
  "converts docopt's USAGE section into a hash-map of
  use-case -> matching components (as keywords)"
  [usage]
  (let [referral (comp combi/nt keyword)]
    (t/transform
      {:command     referral
       :argument    referral
       :option-key  referral
       :option-name referral
       :multiple    combi/plus
       :required    combi/cat
       :optional    (comp combi/opt combi/cat)
       :exclusive   combi/alt; %&
       ; drop the name of the program & ignore the use-tag
       :usage-line  (fn [& els] (combi/hide-tag (apply combi/cat (rest els))))
       :USAGE       (fn [& use-lines] ;; TODO: seems unnecessary, simply return an alternation of inner elements
                      (let [use-names (map #(keyword (str "use" %)) (range (count use-lines)))
                            content   (apply combi/alt (map combi/nt use-names))
                            use-cases (zipmap use-names use-lines)]
                        (assoc use-cases :USAGE content)))};; add the start rule
      usage)))

(defn- multiple-nt
  "transform the USAGE section into a set of keywords that can be repeated by the
   user in the CLI according to docopt's ellipsis usage."
  [usage]
  (let [elements  (tree-seq vector? identity usage)
        multiples (comp (filter vector?); any instaparse result
                        (filter (comp #{:multiple} first))
                        (distinct))]
    (into #{} (comp (filter string?) (map keyword))
      (tree-seq sequential? identity (sequence multiples elements)))))

(defn- combine
  "transform the parsed CLI arguments into a hash-map of {:name value}, where
  value is either a string or a list with all passed values. Only elements
  defined with ellipsis (...) are combined into a list"
  [result tags] ; rest result -> drop USAGE
  (let [multiples (into #{} (comp (map first) (filter tags)) (rest result))
        singles   (into {} (remove (comp tags first) (rest result)))
        elements  (group-by first (rest result))]
    (into singles (map (fn [k] (vector k (map second (k elements)))))
                  multiples)))

(defn parse
  "Parses a docstring, generates a custom parser for the specific cli options
   and returns a map of {element value} where element is one of option-name,
   command or positional-argument. Repeatable arguments are put into a vector"
  [docstring args]
  (let [grammar-tree (docopt-parser docstring)]
    (if (insta/failure? grammar-tree) grammar-tree
      (let [[usage options] grammar-tree
            refs            (non-terminal usage)
            use-laws        (usage-rules usage)
            opt-laws        (options-rules (or options []))
            cli-parser      (insta/parser (merge refs use-laws opt-laws)
                              :start :USAGE :auto-whitespace :standard)
            result          (cli-parser (str/join " " args))]
        (if (insta/failure? result) result
          (combine result (multiple-nt usage)))))))
          ;(t/transform {:USAGE #(into (hash-map) %&)} result))))))

;(non-terminal (first (insta/parse docopt-parser bar)))
;(usage-rules (first (insta/parse docopt-parser bar)))
;(options-rules (second (insta/parse docopt-parser bar)))))

;FIXME: there is an issue with the whitespaces which prevents the docstring from
; being indented as usual. Instead I have to start the docstring on the first line
; and indent the docstring most to the left
;(defn -main "
;Naval Fate.
;
;Usage:
; naval_fate.py ship new <name>...
; naval_fate.py ship <name> move <x> <y> [--speed]
; naval_fate.py ship shoot <x> <y>
; naval_fate.py mine (set|remove) <x> <y> [--moored | --drifting]
; naval_fate.py (-h | --help)
; naval_fate.py --version
;
;Options:
; -h --help     Show this screen.
; --version     Show version.
; --speed=<kn>  Speed in knots [default: 10].
; --moored      Moored (anchored) mine.
; --drifting    Drifting mine."
;  [& args]
;  (parse (:doc (meta #'-main)) args))

;(def bar (:doc (meta #'-main)))
