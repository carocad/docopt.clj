(ns docopt.core
  (:gen-class)
  (:require [instaparse.core :as insta])
  (:require [instaparse.combinators :as combi]))

;TODO accept dot (.) as a valid character in the option description
;TODO accept parenthesis () as a valid characters in the option description. EX (anchored)

(def ^:private docopt-language-grammar
  "<DOCSTRING> = [<DESCRIPTION>] USAGE [OPTIONS]
    DESCRIPTION = #'(?si).*?(?:(?!usage).)*'
    USAGE = <#'(?i)usage:\\s+'> usage-line {usage-line}
    OPTIONS = <#'(?i)options:\\s+'> option-line {option-line}
    usage-line = name {expression} <EOL>
    option-line = [short-option option-arg?] [long-option option-arg?] <option-desc> [default] <EOL>
    name = #'[a-zA-Z-_]+'

    <expression> = required | optional | exclusive | multiple | short-option [<option-arg>] | long-option [<option-arg>] | argument | command
    required = <'('> expression <')'>
    optional = <'['> expression <']'>
    exclusive = expression <OR> expression
    multiple = expression <ellipsis>

    argument = #'<\\w+>'
    short-option = #'-[a-zA-Z]'
    long-option = #'--\\w+'
    option-arg = #'(?:=)?([A-Z]+)'
    command = #'\\w+'
    option-desc = &#' {2,}' word {word}
    default = <'[default:'> word <']'>

    all-options = '[options]'      (*TODO*)
    single-hyphen = '[-]'          (*TODO*)
    double-hyphen = '[--]'         (*TODO*)

    ellipsis = '...'
    OR = '|'

   <word> = #'\\w+'               (* two or more letters are a word*)
   <EOL> = #'(\\n|\\r)+|$'")

(def ^:private matcher
  {:full-option #(str "<'" %1 "'|'" %2 "'> #'\\S+'")
   :both-option #(str "'" %1 "'|'" %2 "'")
   :opt+arg     #(str "<'" % "'> #'\\S+'")
   :opt         identity
   :argument    #(str "\\S+")
   :command     #(str %)
   :refer-to    #(keyword %)})

(defn- tag-match?
  [element tags]
  ((first element) (apply hash-set tags)))

;TO-THINK: if I'm sure that there is a short option, why don't replace it inmediately as a combinator?
(defn- make-option
  "create a hash-map of name-instaparse.combinator based on the different kind of long/short options and if
  an option accepts arguments."
  [{:keys [short-option long-option option-arg]}]
  (let [short-key (keyword short-option)
        long-key  (keyword long-option)]
    (cond
     (and long-option short-option option-arg) (hash-map long-key  (combi/ebnf ((matcher :full-option) long-option short-option))
                                                         short-key ((matcher :refer-to) long-option))
     (and long-option short-option)            (hash-map long-key  (combi/ebnf ((matcher :both-option) long-option short-option))
                                                         short-key ((matcher :refer-to) long-option))
     (and long-option option-arg)              (hash-map long-key  (combi/ebnf ((matcher :opt+arg) long-option)))
     (and short-option option-arg)             (hash-map short-key (combi/ebnf ((matcher :opt+arg) short-option)))
     long-option                               (hash-map long-key  (combi/string ((matcher :opt) long-option)))
     short-option                              (hash-map short-key (combi/string ((matcher :opt+arg) short-option))))))

(defn- make-element
  [content]
  {(keyword content) (combi/string content)})

(defn- make-argument
  [content]
  {(keyword content) (combi/regexp "\\S+")})

(defn- fetch-elements
  "traverse the usage-tree, extract the interesting tags and group them by tag name"
  [usage-tree & target-tag]
  (->> (tree-seq vector? identity usage-tree)
       (filter #(and (vector? %) (tag-match? % target-tag)))
       (group-by #(first %))))

(defn- elements-combinators
  "based on the parsed usage and options section, get the commands, arguments
  and options. Each element is transformed into a key-value pair with the key
  its name (<one> --two -t ...), each value is the corresponding
  instaparse.combinators to be matched against."
  [usage-tree options-tree]
   (let [option-lines  (map rest (rest options-tree))
         elements      (fetch-elements usage-tree :name :command :argument)
         fetch-all     (fn [tag] (into (hash-set) (map second (tag elements))))
         names         (fetch-all :name) ; TODO: check that only one element is here
         commands      (map make-element (fetch-all :command))
         arguments     (map make-argument (fetch-all :argument))
         ;usg-options   (into (fetch-all :long-option)
         ;                    (fetch-all :short-option))
         options       (map make-option (map #(into (hash-map) %) option-lines))]
     (into {} (apply concat commands arguments options))))

(defn- translate-grammar
  "conver the docopt argument-parsing language-grammar into EBNF notation using
  instaparse.combinators. The complete Usage section is converted into an flat
  alternation (use1 | use2 | ...) based on each usage line."
  [parsed-usage short->long-option]
  (insta/transform
     {:command         #(combi/nt (keyword %))
      :argument        #(combi/nt (keyword %))
      :short-option    #(combi/nt ((keyword %) short->long-option))
      :long-option     (fn [& args] (combi/nt (keyword (first args))))
      :multiple        #(combi/plus %)
      :required        #(combi/cat %) ; LATER: does this really work?
      :optional        #(combi/opt %)
      :exclusive       #(apply combi/alt %&)
      :usage-line      #(combi/hide-tag (apply combi/cat (rest %&))) ; drop the name of the program & ignore the use-tag
      :USAGE           (fn [& args]
                         (let [use-names     (map #(keyword (str "use" %)) (range (count args)))
                               use-nt        (map combi/nt use-names)
                               start-content (apply combi/alt use-nt)
                               start         (hash-map :USAGE start-content)
                               use-cases     (apply merge (map hash-map use-names args))]
                           (merge start use-cases)))}
     parsed-usage))

(defn- argument-grammar
  "Creates a grammar specification hash-map for instaparse/parser to work with."
  [docstring]
  (let [docopt-parser        (insta/parser docopt-language-grammar
                                           :auto-whitespace (insta/parser "whitespace = #'( |\\t)+'"))
        grammar-tree         (docopt-parser docstring)
        all-elements         (elements-combinators (first grammar-tree) (second grammar-tree))
        main-elements        (into {} (filter #(not (keyword? (second %))) (seq all-elements)))
        short->long-option   (into {} (filter #(keyword? (second %)) (seq all-elements)))]
    ;(insta/transform {:USAGE #(into (hash-map) %&)}
    (into (translate-grammar (first grammar-tree) short->long-option)
          main-elements)))

(defn docopt
  "Parses docopt argument-parsing language and returns a hash-map with the
  command line arguments according to the described use cases.
  If no docstring is provided, it defaults to -main :doc metadata.
  If no arguments are given, clojure's *command-line-args* are used."
  ([]
   (let [docstring (-> (ns-publics *ns*) ; get the public functions in ns
                       ('-main)
                       (meta)
                       (:doc))]
       (docopt docstring *command-line-args*)))
  ([docstring]
    (docopt docstring *command-line-args*))
  ([docstring args]
   (let [arg-tree    (argument-grammar docstring)
         arg-parser  (insta/parser arg-tree :start :USAGE :auto-whitespace :standard)
         parsed-argv (arg-parser (clojure.string/join " " args))]
     (insta/transform {:USAGE #(into (hash-map) %&)}  parsed-argv))))


(defn -main
  "Naval Fate.

Usage:
  naval_fate ship new <name>...
  naval_fate ship <name> move <x> <y> [--speed=<kn>]
  naval_fate ship shoot <x> <y>
  naval_fate mine (set|remove) <x> <y> [--moored|--drifting]
  naval_fate -h | --help
  naval_fate --version

Options:
  -h --help     Show this screen.
  --version     Show version.
  --speed=<kn>  Speed in knots [default: 10].
  --moored      Moored mine
  --drifting    Drifting mine"

  [& args]
  (let [arguments (docopt)]
    (println arguments))) ; with only one argument, docopt parses -main's docstring.
