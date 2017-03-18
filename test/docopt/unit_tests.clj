(ns docopt.unit-tests
  (:require [docopt.core :as docopt]
            [clojure.spec :as s]
            [clojure.test :as test]
            [instaparse.core :as insta]
            [clojure.string :as str]))

;; unit tests taken from
;; https://raw.githubusercontent.com/docopt/docopt/511d1c57b59cd2ed663a9f9e181b5160ce97e728/testcases.docopt
;; (some small modifications included to conform to cluno's docopt dialect)

(def test-cases "set of [docstring, [[input output]...]"
  [
   ["usage:
       prog [-a -r -m]

    options:
     -a        Add
     -r        Remote
     -m <msg>  Message

    "
    [["prog -a -r -m=yolo" {"-a" "-a" "-r" "-r" "-m" "yolo"}]]]

   ["usage:
       prog -a -b

     options:
       -a  vowel
       -b  consonant

     "
    [["prog -a -b" {"-a" "-a" "-b" "-b"}]
     ;["prog -b -a" {"-a" "-a" "-b" "-b"}] not supported: wrong order
     ["prog -a" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
        prog (-a -b)

     options:
       -a  vowel
       -b  consonant
     "
    [["prog -a -b" {"-a" "-a" "-b" "-b"}]
     ;["prog -b -a" {"-a" "-a" "-b" "-b"}] not supported: wrong order
     ["prog -a" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
       prog [-a] -b

     options:
       -a  vowel
       -b  consonant

     "
    [["prog -a -b" {"-a" "-a" "-b" "-b"}]
     ;["prog -b -a" {"-a" "-a" "-b" "-b"}] not supported: wrong order
     ["prog -a" "user-error"]
     ["prog -b" {"-b" "-b"}]
     ["prog" "user-error"]]]

   ["usage:
       prog [(-a -b)]

    options:
      -a  vowel
      -b  consonant

    "
    [["prog -a -b" {"-a" "-a" "-b" "-b"}]
     ;["prog -b -a" {"-a" "-a" "-b" "-b"}]
     ["prog -a" "user-error"]
     ["prog -b" "user-error"]
     ["prog" {}]]]

   ["usage:
       prog (-a|-b)

    options:
      -a  vowel
      -b  consonant

    "
    [["prog -a -b" "user-error"]
     ["prog" "user-error"]
     ["prog -a" {"-a" "-a"}]
     ["prog -b" {"-b" "-b"}]]]

   ["usage:
       prog [ -a | -b ]

    options:
      -a  vowel
      -b  consonant

    "
    [["prog -a -b" "user-error"]
     ["prog" {}]
     ["prog -a" {"-a" "-a"}]
     ["prog -b" {"-b" "-b"}]]]

   ["usage:
       prog <arg>"
    [["prog 10" {"<arg>" "10"}]
     ["prog 10 20" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
       prog [<arg>]"
    [["prog 10" {"<arg>" "10"}]
     ["prog 10 20" "user-error"]
     ["prog" {}]]]

   ["usage:
       prog <kind> <name> <type>"
    [["prog 10 20 40" {"<kind>" "10" "<name>" "20" "<type>" "40"}]
     ["prog 10 20" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
       prog <kind> [<name> <type>]"
    [["prog 10 20 40" {"<kind>" "10" "<name>" "20" "<type>" "40"}]
     ;["prog 10 20" {"<kind>" "10" "<name>" "20"}] not supported either one or three args
     ["prog" "user-error"]]]

   ["usage:
       prog [<kind> | <name> <type>]"
    [["prog 10 20 40" "user-error"]
     ["prog 20 40" { "<name>" "20" "<type>" "40"}]
     ["prog" {}]]]

   ["usage:
       prog (<kind> --all) | <name>

    options:
       --all  everything

    "
    [["prog 10 --all" {"<kind>" "10" "--all" "--all"}]
     ;["prog 10" {"<name>" "10"}] not supported option marked as required
     ["prog" "user-error"]]]

   ["usage:
       prog <NAME>..."
    [["prog 10 20" {"<NAME>" ["10" "20"]}]
     ["prog 10" {"<NAME>" ["10"]}]
     ["prog" "user-error"]]]

   ["usage:
       prog [<NAME>]..."
    [["prog 10 20" {"<NAME>" ["10" "20"]}]
     ["prog 10" {"<NAME>" ["10"]}]
     ["prog" {}]]]

   ["usage:
       prog [<NAME>...]"
    [["prog 10 20" {"<NAME>" ["10" "20"]}]
     ["prog 10" {"<NAME>" ["10"]}]
     ["prog" {}]]]

   ["usage:
       prog [<NAME> [<NAME> ...]]"
    [["prog 10 20" {"<NAME>" ["10" "20"]}]
     ["prog 10" {"<NAME>" ["10"]}]
     ["prog" {}]]]

   ["usage:
       prog <NAME> | (--foo <NAME>)

    options:
       --foo  bar

    "
    [["prog 10" {"<NAME>" "10"}]
     ["prog --foo 10" {"<NAME>" "10" "--foo" "--foo"}]
     ["prog --foo=10" "user-error"]]]

   ["usage:
       prog (<NAME> | --foo) [--bar | <NAME>]

    options:
      --foo  a variable
      --bar  another variable

    "
    [["prog 10" {"<NAME>" "10"}]
     ;["prog 10 20" {"<NAME>" ["10" "20"]}] ; not supported <NAME> must be multiple <NAME>...
     ["prog --foo --bar" {"--foo" "--foo" "--bar" "--bar"}]]]

   ["Naval Fate.

     Usage:
       prog ship new <name>...
       prog ship [<name>] move <x> <y> [--speed]
       prog ship shoot <x> <y>
       prog mine (set|remove) <x> <y> [--moored|--drifting]
       prog -h | --help
       prog --version

     Options:
       -h --help     Show this screen.
       --version     Show version.
       --speed=<kn>  Speed in knots [default:10].
       --moored      Mored (anchored) mine.
       --drifting    Drifting mine.

     "
     [["prog ship Guardian move 150 300 --speed=20"
       {"--speed" "20"
        "<name>" ["Guardian"]
        "<x>" "150"
        "<y>" "300"
        "move" "move"
        "ship" "ship"}]]]

   ["usage:
       prog --hello

     options:
       --hello  world"
    [["prog --hello" {"--hello" "--hello"}]]]

   ["usage:
       prog [--hello]

     options:
       --hello=<msg>  world"
    [["prog" {}]
     ["prog --hello=wrld" {"--hello" "wrld"}]]]

   ["usage:
       prog [-o]

     options:
       -o  foo"
    [["prog" {}]
     ["prog -o" {"-o" "-o"}]]]

   ["usage:
       prog -v

     options:
       -v  verbose"
    [["prog -v" {"-v" "-v"}]]]

   ["usage:
       prog --hello

     options:
       --hello  world"
    [["prog --hello" {"--hello" "--hello"}]]]

   ["usage:
       prog [--hello]

     options:
       --hello=<msg>  world"
    [["prog" {}]
     ["prog --hello=wrld" {"--hello" "wrld"}]]]

   ["usage:
       prog [-o]

      options:
        -o  foo"
    [["prog" {}]
     ["prog -o" {"-o" "-o"}]]]

   ["usage:
       git [-v | --verbose]

     options:
       -v --verbose  verbosity level"
    [["prog -v" {"--verbose" "-v"}]]]

   ["usage:
       git remote [-v | --verbose]

     options:
       -v --verbose  verbosity level"
    [["prog remote -v" {"remote" "remote" "--verbose" "-v"}]]]

   ["usage:
       prog"
    [["prog" {}]]]

   ["usage:
      prog
      prog <a> <b>
    "
    [["prog 1 2" {"<a>" "1" "<b>" "2"}]
     ["prog" {}]]]

   ["usage:
      prog <a> <b>
      prog
    "
    [["prog" {}]]]

   ["usage:
       prog [--file]

     options:
       --file  input file"
    [["prog" {}]]]

   ["usage:
       prog [--file]

    options:
       --file <a>  input file

    "
    [["prog" {}]]]

   ;["usage:
   ;    prog [-a]
   ;
   ; options:
   ;    -a --address <hostport>  TCP address [default:localhost6283].
   ;
   ; "
   ; [["prog" {"--address" "localhost6283"}]]] not supported YET default value

   ["usage:
       prog --long...

     options:
       --long <short>  a short option"
    [["prog --long:one" {"--long" ["one"]}]
     ["prog --long:one --long:two" {"--long" ["one" "two"]}]]]

   ["usage:
       prog (go <direction> --speed)...

     options:
       --speed=<number>  the velocity"
    [["prog  go left --speed=5  go right --speed=9"
      {"go" ["go" "go"] "<direction>" ["left" "right"] "--speed" ["5" "9"]}]]]

   ["usage:
      prog [-o]...

     options:
        -o=<foo>  foo [default:x]

    "
    [["prog -o:this -o:that" {"-o" ["this" "that"]}]]]
     ;["prog" {"-o" ["x"]}]]] not supported YET, default values

   ["usage:
      foo (--xx|--yy)...

     options:
       --xx=<foo>  foo desc
       --yy=<bar>  bar desc"
    [["prog --xx=1 --yy=2" {"--xx" ["1"] "--yy" ["2"]}]]]])

(test/deftest unit-test
  (doseq [[docstring in-out] test-cases
          [input result] in-out
          :let [args (docopt/parse docstring (rest (str/split input #" ")))]]
    ;(println docstring)
    (if (= result "user-error")
      (test/is (insta/failure? args) args)
      (do (test/is (not (insta/failure? args))
                   {:docstring docstring :failure args})
          (test/is (= args (clojure.walk/keywordize-keys result))
                   (clojure.walk/keywordize-keys result))))))

;(test/run-all-tests #"docopt")

; NOTE: whenever you change the test cases, run the following code to check
;       that it conforms to the appropiate formatting

;(s/def ::docstring string?)
;(s/def ::input string?)
;(s/def ::args (s/map-of string? any?))
;
;(s/def ::result (s/or :success ::args :error string?))
;(s/def ::entry (s/tuple ::input ::result))
;(s/def ::test (s/tuple ::docstring (s/coll-of ::entry)))

;(for [case test-cases]
;  (when-not (s/valid? ::test case)
;    (s/explain-str ::test case)))
