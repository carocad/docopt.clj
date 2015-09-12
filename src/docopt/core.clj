(ns docopt.core
  (:require [instaparse.core :as insta])
  (:require [instaparse.combinators :as combi]))

;TODO accept dot (.) as a valid character in the option description
;TODO accept parenthesis () as a valid characters in the option description. EX (anchored)

(def ^:private docstring-parser
  (insta/parser
   "<DOCSTRING> = [<DESCRIPTION>] USAGE [OPTIONS]
    DESCRIPTION = #'(?si).*?(?:(?!usage).)*'
    USAGE = <#'(?i)usage:\\s+'> usage-line {usage-line}
    OPTIONS = <#'(?i)options:\\s+'> option-line {option-line}
    usage-line = name {expression} <EOL>
    option-line = [short-option] [long-option] <option-desc> [default] <EOL>
    name = #'[a-zA-Z-_]+'

    <expression> = required | optional | exclusive | multiple | long-option | short-option | argument | command
    required = <'('> expression <')'>
    optional = <'['> expression <']'>
    exclusive = expression <OR> expression
    multiple = expression <ellipsis>

    argument = #'<\\w+>'
    short-option = #'-[a-zA-Z]' [#'[A-Z]+?']
    long-option = #'--\\w+' [#'(?:=)?[A-Z]+']
    command = #'\\w+'
    option-desc = word {word}
    default = <'[default:'> word <']'>

    all-options = '[options]'      (*TODO*)
    single-hyphen = '[-]'          (*TODO*)
    double-hyphen = '[--]'         (*TODO*)

    ellipsis = '...'
    OR = '|'

   <word> = #'\\w+'               (* two or more letters are a word*)
   <EOL> = #'(\\n|\\r)+|$'"
   :auto-whitespace (insta/parser "whitespace = #'( |\\t)+'")))

(defn- option-regex
  [long-opt short-opt]
  (cond
    (and long-opt short-opt) (str "#'" long-opt "|" short-opt "'")
    long-opt  (str "#'" long-opt "'")
    short-opt (str "#'" short-opt "'")))

(defn- tag-match?
  [element & tags]
  ((first element) (into (hash-set) tags)))

(defn- make-option
  [option-hash]
  (let [long-opt  (:long-option option-hash)
        short-opt (:short-option option-hash)]
    (cond
     (and long-opt short-opt) (list (vector (keyword long-opt)  (option-regex long-opt short-opt))
                                    (vector (keyword short-opt) (keyword long-opt)))
     long-opt (list (vector (keyword long-opt) (option-regex long-opt short-opt)))
     short-opt (list (vector (keyword short-opt) (option-regex long-opt short-opt))))))

(defn- make-element
  [content]
  (vector (keyword content) (str "'" content "'")))

(defn- make-argument
  [content]
  (vector (keyword content) "#'\\S+'"))

(defn- get-elements
  [parsed-docstring]
   (let [USAGE         (first parsed-docstring)
         option-lines  (rest (second parsed-docstring))
         elements      (->> (tree-seq vector? identity (first parsed-docstring))
                            (filter #(and (vector? %) (not (tag-match? % :USAGE :usage-line :multiple :exclusive :required :optional))))
                            (group-by #(first %)))
         my-test (->> (tree-seq vector? identity (second parsed-docstring))
                            (filter #(and (vector? %) (tag-match? % :short-option))))
         fetch-all     (fn [tag] (into (hash-set) (map second (tag elements))))
         names         (fetch-all :name)
         commands      (map make-element (fetch-all :command))
         arguments     (map make-argument (fetch-all :argument))
         usg-options   (into (fetch-all :long-option)
                             (fetch-all :short-option))
         options       (->> (map rest option-lines)
                            (map (partial into (hash-map)))
                            (map make-option))]
     (into {} (apply concat commands arguments options))))

(defn- translate-grammar
  [parsed-usage all-elements]
  (insta/transform
     {:command         #(combi/nt (keyword %))
      :argument        #(combi/nt (keyword %))
      :short-option    #(combi/nt ((keyword %) all-elements))
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

(defn- match
  [docstring args]
  (let [parsed-docstring (docstring-parser docstring)
        all-elements     (get-elements parsed-docstring)
        main-elements    (filter #(not (keyword? (second %))) (seq all-elements))
        combi-elements   (map hash-map (map first main-elements) (map combi/ebnf (map second main-elements)))
        panacea          (into (translate-grammar (first parsed-docstring) all-elements)
                               combi-elements)]
    (insta/transform {:USAGE #(into (hash-map) %&)}
      ((insta/parser panacea
                     :start :USAGE
                     :auto-whitespace :standard) args))))

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
    (match docstring args)))
