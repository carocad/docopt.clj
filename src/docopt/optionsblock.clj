(ns docopt.optionsblock
  (:require [clojure.string :as string])
  (:require [docopt.util :as util]))

(defn parse2
  [option-lines]
  (let [tokenized-options (map tokenize options-lines)]
    ;REVIEW: Is there a more clean way to check this?
    (assert (and (distinct? (filter identity (map :long tokenized-options)))
                 (distinct? (filter identity (map :short tokenized-options))))
            "SYNTAX ERROR: In options descriptions, at least one option defined
            more than once.")))

(defn- tokenize ; wait until you can replace the complete function below
  "Generates a sequence of tokens for an option specification string."
  [line] ; second drops the first match, usually a trailing Character -, --
  (hash-map :default (second (re-find (:default option-types) line))
            :short   (second (re-find (:short option-types) line))
            :long    (second (re-find (:long option-types) line))
            :arg     (first (re-find (:arg option-types) line))))

; REVIEW: do we really need this?
(defn takes-arg?
  [{:keys [arg]}]
  (true? arg))

; REVIEW: should this be really public?
(def option-types {:default   #"\s{2,}(?s).*\[(?i)default(?-i):\s*([^\]]+).*"
                   :short     #"(?:^|\s+),?\s*-([^-,])"
                   :long      #"(?:^|\s+),?\s*--([^ \t=,]+)"
                   :arg       #"(<[^<>]*>|[A-Z_0-9]*[A-Z_][A-Z_0-9]*\s)"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- tokenize-option
  "Generates a sequence of tokens for an option specification string."
  [string]
  (util/tokenize string [[#"\s{2,}(?s).*\[(?i)default(?-i):\s*([^\]]+).*" :default]
                    [#"\s{2,}(?s).*"] ; description of the option
                    [#"(?:^|\s+),?\s*-([^-,])"                       :short]
                    [#"(?:^|\s+),?\s*--([^ \t=,]+)"                  :long]
                    [(re-pattern re-arg-str)                         :arg]
                    [#"\s*[=,]?\s*"]])) ; UNKNOWN

(defn- parse-option
  "Parses option description line into associative map."
  [option-line]
  (let [tokens (tokenize-option option-line)]
    (assert (filter string? tokens)
            (str "SYNTAX ERROR: Badly-formed option definition: '"
                 (string/replace option-line #"\s\s.*" "") "'."))
    (let [{:keys [short long arg default]} (reduce conj {} tokens)
          [value & more-values] (filter seq (string/split (or default "") #"\s+"))]
      (into (if arg
              {:takes-arg true :default-value (if (seq more-values) (into [value] more-values) value)}
              {:takes-arg false})
            (filter val {:short short :long long})))))

(defn parse
  "Parses options lines."
  [options-lines]
  (let [options (map parse-option options-lines)]
    (assert (and (distinct? (filter identity (map :long options)))
                 (distinct? (filter identity (map :short options))))
            "SYNTAX ERROR: In options descriptions, at least one option defined more than once.")
    (into #{} options)))
