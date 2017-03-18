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
    [["prog -a -r -m=yolo" {"-a" true "-r" true "-m" "yolo"}]]]

   ["usage:
       prog -a -b

     options:
       -a
       -b

     "
    [["prog -a -b" {"-a" true, "-b" true}]
     ["prog -b -a" {"-a" true, "-b" true}]
     ["prog -a" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
        prog (-a -b)

     options:
       -a
       -b

     "
    [["prog -a -b" {"-a" true, "-b" true}]
     ["prog -b -a" {"-a" true, "-b" true}]
     ["prog -a" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
       prog [-a] -b

     options:
       -a
       -b

     "
    [["prog -a -b" {"-a" true "-b" true}]
     ["prog -b -a" {"-a" true "-b" true}]
     ["prog -a" "user-error"]
     ["prog -b" {"-a" false, "-b" true}]
     ["prog" "user-error"]]]

   ["usage:
       prog [(-a -b)]

    options:
      -a
      -b

    "
    [["prog -a -b" {"-a" true, "-b" true}]
     ["prog -b -a" {"-a" true, "-b" true}]
     ["prog -a" "user-error"]
     ["prog -b" "user-error"]
     ["prog" {"-a" false, "-b" false}]]]

   ["usage:
       prog (-a|-b)

    options:
      -a
      -b

    "
    [["prog -a -b" "user-error"]
     ["prog" "user-error"]
     ["prog -a" {"-a" true, "-b" false}]
     ["prog -b" {"-a" false, "-b" true}]]]

   ["usage:
       prog [ -a | -b ]

    options:
      -a
      -b

    "
    [["prog -a -b" "user-error"]
     ["prog" {"-a" false, "-b" false}]
     ["prog -a" {"-a" true, "-b" false}]
     ["prog -b" {"-a" false, "-b" true}]]]

   ["usage:
       prog <arg>"
    [["prog 10" {"<arg>" "10"}]
     ["prog 10 20" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
       prog [<arg>]"
    [["prog 10" {"<arg>" "10"}]
     ["prog 10 20" "user-error"]
     ["prog" {"<arg>" nil}]]]

   ["usage:
       prog <kind> <name> <type>"
    [["prog 10 20 40" {"<kind>" "10", "<name>" "20", "<type>" "40"}]
     ["prog 10 20" "user-error"]
     ["prog" "user-error"]]]

   ["usage:
       prog <kind> [<name> <type>]"
    [["prog 10 20 40" {"<kind>" "10", "<name>" "20", "<type>" "40"}]
     ["prog 10 20" {"<kind>" "10", "<name>" "20", "<type>" nil}]
     ["prog" "user-error"]]]

   ["usage:
       prog [<kind> | <name> <type>]"
    [["prog 10 20 40" "user-error"]
     ["prog 20 40" {"<kind>" nil, "<name>" "20", "<type>" "40"}]
     ["prog" {"<kind>" nil, "<name>" nil, "<type>" nil}]]]

   ["usage:
       prog (<kind> --all | <name>)

    options
       --all

    "
    [["prog 10 --all" {"<kind>" "10", "--all" true, "<name>" nil}]
     ["prog 10" {"<kind>" nil, "--all" false, "<name>" "10"}]
     ["prog" "user-error"]]]

   ["usage:
       prog NAME..."
    [["prog 10 20" {"NAME" ["10", "20"]}]
     ["prog 10" {"NAME" ["10"]}]
     ["prog" "user-error"]]]

   ["usage:
       prog [NAME]..."
    [["prog 10 20" {"NAME" ["10", "20"]}]
     ["prog 10" {"NAME" ["10"]}]
     ["prog" {"NAME" []}]]]

   ["usage:
       prog [NAME...]"
    [["prog 10 20" {"NAME" ["10", "20"]}]
     ["prog 10" {"NAME" ["10"]}]
     ["prog" {"NAME" []}]]]

   ["usage:
       prog [NAME [NAME ...]]"
    [["prog 10 20" {"NAME" ["10", "20"]}]
     ["prog 10" {"NAME" ["10"]}]
     ["prog" {"NAME" []}]]]

   ["usage:
       prog (NAME | --foo NAME)

    options:
       --foo

    "
    [["prog 10" {"NAME" "10", "--foo" false}]
     ["prog --foo 10" {"NAME" "10", "--foo" true}]
     ["prog --foo=10" "user-error"]]]

   ["usage:
       prog (NAME | --foo) [--bar | NAME]

    options:
      --foo
      --bar

    "
    [["prog 10" {"NAME" ["10"], "--foo" false, "--bar" false}]
     ["prog 10 20" {"NAME" ["10", "20"], "--foo" false, "--bar" false}]
     ["prog --foo --bar" {"NAME" [], "--foo" true, "--bar" true}]]]

   ["Naval Fate.

     Usage
       prog ship new <name>...
       prog ship [<name>] move <x> <y> [--speed]
       prog ship shoot <x> <y>
       prog mine (set|remove) <x> <y> [--moored|--drifting]
       prog -h | --help
       prog --version

     Options
       -h --help     Show this screen.
       --version     Show version.
       --speed=<kn>  Speed in knots [default: 10].
       --moored      Mored (anchored) mine.
       --drifting    Drifting mine.

     "
     [["prog ship Guardian move 150 300 --speed=20"
       {"--drifting" false,
        "--help" false,
        "--moored" false,
        "--speed" "20",
        "--version" false,
        "<name>" ["Guardian"],
        "<x>" "150",
        "<y>" "300",
        "mine" false,
        "move" true,
        "new" false,
        "remove" false,
        "set" false,
        "ship" true,
        "shoot" false}]]]

   ["usage:
       prog --hello"
    [["prog --hello" {"--hello" true}]]]

   ["usage:
       prog [--hello]"
    [["prog" {"--hello" nil}]
     ["prog --hello wrld" {"--hello" "wrld"}]]]

   ["usage:
       prog [-o]"
    [["prog" {"-o" false}]
     ["prog -o" {"-o" true}]]]

   ["usage:
       prog -v"
    [["prog -v" {"-v" true}]]]

   ["usage:
       prog --hello"
    [["prog --hello" {"--hello" true}]]]

   ["usage:
       prog [--hello]"
    [["prog" {"--hello" nil}]
     ["prog --hello wrld" {"--hello" "wrld"}]]]

   ["usage:
       prog [-o]"
    [["prog" {"-o" false}]
     ["prog -o" {"-o" true}]]]

   ["usage:
       git [-v | --verbose]"
    [["prog -v" {"-v" true, "--verbose" false}]]]

   ["usage:
       git remote [-v | --verbose]"
    [["prog remote -v" {"remote" true, "-v" true, "--verbose" false}]]]

   ["usage:
       prog"
    [["prog" {}]]]

   ["usage:
      prog
      prog <a> <b>
    "
    [["prog 1 2" {"<a>" "1", "<b>" "2"}]
     ["prog" {"<a>" nil, "<b>" nil}]]]

   ["usage:
      prog <a> <b>
      prog
    "
    [["prog" {"<a>" nil, "<b>" nil}]]]

   ["usage:
       prog [--file]"
    [["prog" {"--file" nil}]]]

   ["usage:
       prog [--file]

    options:
       --file <a>

    "
    [["prog" {"--file" nil}]]]

   ["usage:
       prog [-a]

    options:
       -a, --address <hostport>  TCP address [default: localhost6283].

    "
    [["prog" {"--address" "localhost6283"}]]]

   ["usage:
       prog --long..."
    [["prog --long one" {"--long" ["one"]}]
     ["prog --long one --long two" {"--long" ["one", "two"]}]]]

   ["usage:
       prog (go <direction> --speed)..."
    [["prog  go left --speed=5  go right --speed=9"
      {"go" 2, "<direction>" ["left", "right"], "--speed" ["5", "9"]}]]]

   ["usage:
      prog [-o]...

     options:
        -o  [default: x]

    "
    [["prog -o this -o that" {"-o" ["this", "that"]}]
     ["prog" {"-o" ["x"]}]]]

   ["usage:
      foo (--xx=x|--yy=y)..."
    [["prog --xx=1 --yy=2" {"--xx" ["1"], "--yy" ["2"]}]]]])

(test/deftest unit-test
  (doseq [[docstring in-out] test-cases
          [input result] in-out
          :let [args (docopt/parse docstring (rest (str/split input #" ")))]]
    (if (= result "user-error")
      (test/is (insta/failure? args) args)
      (test/is (and (not (insta/failure? args))
                    (= result (clojure.walk/keywordize-keys args)))
               args))))

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

;(test/run-all-tests #"docopt")
