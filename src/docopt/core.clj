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
  " :auto-whitespace (insta/parser "whitespace = #'[\\ \\t]+' (* whitespace *)")))

(defn- usage-rules
  "conver the docopt argument-parsing language-grammar into EBNF notation using
  instaparse.combinators. The complete Usage section is converted into an flat
  alternation (use1 | use2 | ...) based on each usage line."
  [usage]
  (let [sentinel (fn [& args] nil)
        inner    (fn [& v] v)
        tuse
        (t/transform
          {:command     (fn [name] [(keyword name) (combi/string name)])
           :argument    (fn [name] [(keyword name) (combi/regexp "\\w+")])
           :option-key  sentinel
           :option-name sentinel
           :multiple    inner
           :required    inner
           :optional    inner
           :exclusive   inner
           ; drop the name of the program
           :usage-line  (fn [& v] (flatten (rest v)))
           :USAGE       (fn [& v] (apply concat v))}
          usage)]
    (apply hash-map (remove nil? tuse))))

(defn- optionize
  ([name]
   [(keyword name) (combi/string name)])
  ([name _]
   [(keyword name)
    (combi/cat (combi/hide (combi/regexp (str name "[=:]")))
               (combi/regexp "\\S+"))]))

(defn- opt-combi
  [& elements]
  (let [rel (remove nil? elements)]
    (if (= 1 (count rel)) [(apply hash-map (first rel))] ;; 2 = long and short option
      (let [pars (apply combi/alt (map second rel))
            name (first (second rel))]
        [{name pars} {(ffirst rel) (combi/hide-tag (combi/nt name))}]))))

(defn- options-rules
  "conver the docopt argument-parsing language-grammar into EBNF notation using
  instaparse.combinators. The complete Usage section is converted into an flat
  alternation (use1 | use2 | ...) based on each usage line."
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
  "conver the docopt argument-parsing language-grammar into EBNF notation using
  instaparse.combinators. The complete Usage section is converted into an flat
  alternation (use1 | use2 | ...) based on each usage line."
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

(defn- manifold
  [usage]
  (t/transform
    {:command     identity
     :argument    identity
     :option-key  identity
     :option-name identity
     :multiple    (fn [& v] [:multiple v])
     :required    (fn [& v] v)
     :optional    (fn [& v] v)
     :exclusive   (fn [& v] v)
     ; drop the name of the program
     :usage-line  (fn [& v] (rest v))
     :USAGE       (fn [& v] (apply concat v))}
    usage))

(defn- multiple-nt
  [usage]
  (let [reorg (manifold usage)
        mulel (sequence (comp (filter vector?)
                              (filter #(= :multiple (first %)))
                              (map rest))
                (tree-seq sequential? identity reorg))
        eles  (flatten mulel)]
    (into #{} (map keyword) eles)))

(defn- combine
  [result tags]
  (let [all     (filter (comp tags first) (rest result))
        ks      (into #{} (map first all))
        cleaned (into {} (remove (comp tags first) (rest result)))]
    (into cleaned
      (for [k ks]
        [k (sequence (comp (filter (comp #{k} first)) (map second)) all)]))))

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
          (combine result (multiple-nt usage))))))) ;(t/transform {:USAGE #(into (hash-map) %&)} result))))))

;(def baz (merge (non-terminal (first (insta/parse docopt-parser bar)))
;                (usage-rules (first (insta/parse docopt-parser bar)))
;                (options-rules (second (insta/parse docopt-parser bar)))))
