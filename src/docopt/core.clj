(ns docopt.core
  (:require [instaparse.core :as insta])
  (:require [instaparse.combinators :as combi]))

;TODO accept dot (.) as a valid character in the option description
;TODO accept parenthesis () as a valid characters in the option description. EX (anchored)

(def ^:private grammar-parser
  (insta/parser
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
   <EOL> = #'(\\n|\\r)+|$'"
   :auto-whitespace (insta/parser "whitespace = #'( |\\t)+'")))

(defn- tag-match?
  [element & tags]
  ((first element) (into (hash-set) tags)))

(defn- make-option
  [option-hash]
  (let [{:keys [short-option long-option option-arg]} option-hash]
    (cond
     (and long-option short-option option-arg) (hash-map (keyword long-option)  (combi/ebnf (str "<'" long-option "'|'" short-option "'> #'\\S+'"))
                                                         (keyword short-option) (keyword long-option))
     (and long-option short-option)            (hash-map (keyword long-option)  (combi/ebnf (str "'" long-option "'|'" short-option "'"))
                                                         (keyword short-option) (keyword long-option))
     (and long-option option-arg)              (hash-map (keyword long-option)  (combi/ebnf (str "<'" long-option "'> #'\\S+'")))
     (and short-option option-arg)             (hash-map (keyword short-option) (combi/ebnf (str "<'" short-option "'> #'\\S+'" )))
     long-option                               (hash-map (keyword long-option)  (combi/string long-option))
     short-option                              (hash-map (keyword short-option) (combi/string short-option)))))

(defn- make-element
  [content]
  {(keyword content) (combi/string content)})

(defn- make-argument
  [content]
  {(keyword content) (combi/regexp "\\S+")})

(defn- get-elements
  [usage-tree options-tree]
   (let [option-lines  (map rest (rest options-tree))
         elements      (->> (tree-seq vector? identity usage-tree)
                            (filter #(and (vector? %) (not (tag-match? % :USAGE :usage-line :multiple :exclusive :required :optional))))
                            (group-by #(first %)))
         fetch-all     (fn [tag] (into (hash-set) (map second (tag elements))))
         names         (fetch-all :name)
         commands      (map make-element (fetch-all :command))
         arguments     (map make-argument (fetch-all :argument))
         ;usg-options   (into (fetch-all :long-option)
         ;                    (fetch-all :short-option))
         options       (map make-option (map #(into (hash-map) %) option-lines))]
     (into {} (apply concat commands arguments options))))

(defn- translate-grammar
  [parsed-usage short->long-option]
  (insta/transform
     {:command         #(combi/nt (keyword %))
      :argument        #(combi/nt (keyword %))
      :short-option    #(combi/nt ((keyword %) short->long-option))
      :long-option     (fn [& args] (combi/nt (keyword (first args)))) ; FIX ME: dont ignore the positional argument
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
  [docstring]
  (let [grammar-tree         (grammar-parser docstring)
        all-elements         (get-elements (first grammar-tree) (second grammar-tree))
        main-elements        (into {} (filter #(not (keyword? (second %))) (seq all-elements)))
        short->long-option   (into {} (filter #(keyword? (second %)) (seq all-elements)))]
    ;(insta/transform {:USAGE #(into (hash-map) %&)}
    ;(insta/transform {:USAGE #(merge-with concat {} %&)}
    (into (translate-grammar (first grammar-tree) short->long-option)
          main-elements)))

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
   (let [arg-tree   (argument-grammar docstring)
         arg-parser (insta/parser arg-tree :start :USAGE :auto-whitespace :standard)]
     (arg-parser args))))

(defn -main
  "Naval Fate is a game

Usage:
  naval_fate ship new <name>...
  naval_fate ship <name> move <x> <y> [--speed KN]
  naval_fate ship shoot <x> <y>
  naval_fate mine (set|remove) <x> <y> [--moored|--drifting]
  naval_fate -h | --help
  naval_fate --version

  Options:
  -h --help     Show this screen
  --version     Show version
  --speed KN    Speed in knots [default: 10]
  --moored      Moored anchored mine
  --drifting    Drifting mine"
  [args]
  (docopt (:doc (meta (var -main)))
          args))

(apply hash-map [:a 2])
(apply merge-with conj {:a [] :b []} (map #(into {} %&) (list [:a 2] [:b 2] [:a 3])))

; FIX-ME transform the result such that the right combination is archieved
(-main "ship new boat guardian")
