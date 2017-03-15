(ns docopt.unit-tests
  (:require [clojure.spec :as s]))

;; unit tests taken from
;; https://raw.githubusercontent.com/docopt/docopt/511d1c57b59cd2ed663a9f9e181b5160ce97e728/testcases.docopt
;; (some small modifications included to conform to cluno's docopt dialect)

(def test-cases "set of [docstring, [[input output]...]"
  ;[["usage: prog"
  ;  [["prog" {}]
  ;   ["prog --xxx" "user-error"]]]
  ;
  ; ["usage: prog [options]
  ;
  ;   options: -a  All."
  ;  [["prog"] {"-a" false}
  ;   ["prog -a" {"-a" true}]
  ;   ["prog -x" "user-error"]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options: --all  All.
  ;
  ;  "
  ;  ["prog" {"--all" false}]
  ;  ["prog --all" {"--all" true}]
  ;  ["prog" {"--xxx" "user-error"}]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options: -v, --verbose  Verbose.
  ;
  ;  "
  ;  [["prog --verbose" {"--verbose" true}]
  ;   ;["prog --ver" {"--verbose" true}]
  ;   ["prog -v" {"--verbose" true}]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options: -p PATH
  ;
  ;  "
  ;  [["prog -p home/" {"-p" "home/"}]
  ;   ["prog -phome/" {"-p" "home/"}]
  ;   ["prog -p" "user-error"]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options: --path <path>
  ;
  ;  "
  ;  [["prog --path home/" {"--path" "home/"}]
  ;   ["prog --path=home/" {"--path" "home/"}]
  ;   ["prog --pa home/" {"--path" "home/"}]
  ;   ["prog --pa=home/" {"--path" "home/"}]
  ;   ["prog --path" "user-error"]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options: -p PATH, --path=<path>  Path to files.
  ;
  ;  "
  ;  [["prog -proot" {"--path" "root"}]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options:    -p --path PATH  Path to files.
  ;
  ;  "
  ;  [["prog -p root" {"--path" "root"}]
  ;   ["prog --path root" {"--path" "root"}]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  Options
  ;   -p PATH  Path to files [default ./]
  ;
  ;  "
  ;  [["prog" {"-p" "./"}]
  ;   ["prog -phome" {"-p" "home"}]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options: --path=<files>  Path to files
  ;                  [dEfAuLt /root]
  ;
  ;  "
  ;  [["prog" {"--path" "/root"}]
  ;   ["prog --path=home" "--path" "home"]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options
  ;      -a        Add
  ;      -r        Remote
  ;      -m <msg>  Message
  ;
  ;  "
  ;  [["prog -a -r -m Hello" {"-a" true "-r" true "-m" "Hello"}]
  ;   ["prog -armyourass" {"-a" true "-r" true "-m" "yourass"}]
  ;   ["prog -a -r" {"-a" true "-r" true "-m" nil}]]]
  ;
  ; ["usage: prog [options]
  ;
  ;  options: --version
  ;           --verbose
  ;
  ;  "]
  ; [["prog --version" {"--version" true "--verbose" false}]
  ;  ["prog --verbose" {"--version" false "--verbose" true}]
  ;  ["prog --ver" "user-error"]
  ;  ["prog --verb" {"--version" false "--verbose" true}]]

   [["usage:
       prog [-a -r -m <msg>]

    options:
     -a        Add
     -r        Remote
     -m <msg>  Message

    "]
    ["prog -a -r -m=yourass" {"-a" true "-r" true "-m" "yourass"}]

;"usage: prog [-armmsg]
;
;options: -a        Add
;         -r        Remote
;         -m <msg>  Message
;
;"
;"prog -a -r -m Hello"
;{"-a" true,
;     "-r" true,
; "-m" "Hello"}

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
      ["prog -a"] {"-a" true, "-b" false}
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
				prog [<name> <name>]"
     [["prog 10 20" {"<name>" ["10", "20"]}]
      ["prog 10" {"<name>" ["10"]}]
      ["prog" {"<name>" []}]]]

    ["usage:
				prog [(<name> <name>)]"
     [["prog 10 20" {"<name>" ["10", "20"]}]
      ["prog 10" "user-error"]
      ["prog" {"<name>" []}]]]

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

     "]
    [["prog 10" {"NAME" "10", "--foo" false}]
     ["prog --foo 10" {"NAME" "10", "--foo" true}]
     ["prog --foo=10" "user-error"]]

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
       prog ship [<name>] move <x> <y> [--speed=<kn>]
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
				prog [--hello=<world>]"
     [["prog" {"--hello" nil}]
      ["prog --hello wrld" {"--hello" "wrld"}]]]

    ["usage:
				prog [-o]"
     [["prog" {"-o" false}]
      ["prog -o" {"-o" true}]]]

    ;["usage: prog [-opr]"
    ; ["prog -op" {"-o" true, "-p" true, "-r" false}]]
    ;
    ;["usage: prog --aabb | --aa"
    ; [["prog --aa" {"--aabb" false, "--aa" true}]
    ;  ["prog --a" "user-error"]]]  ; not a unique prefix

;
; Counting number of flags
;
    ["usage:
				prog -v"
     [["prog -v" {"-v" true}]]]

;
;"usage: prog [-v -v]"
;"prog"
;{"-v" 0}
;
;"prog -v"
;{"-v" 1}
;
;"prog -vv"
;{"-v" 2}


;"usage: prog -v ..."
;"prog"
;"user-error"
;
;"prog -v"
;{"-v" 1}
;
;"prog -vv"
;{"-v" 2}
;
;"prog -vvvvvv"
;{"-v" 6}


;"usage: prog [-v | -vv | -vvv]
;
;This one is probably most readable user-friednly variant.
;
;"
;"prog"
;{"-v" 0}
;
;"prog -v"
;{"-v" 1}
;
;"prog -vv"
;{"-v" 2}
;
;"prog -vvvv"
;"user-error"
;
;
;"usage: prog [--ver --ver]"
;"prog --ver --ver"
;{"--ver" 2


;
; Counting commands}
;

;    "usage: prog [go]"])
;"prog go"
;{"go" true}
;
;
;"usage: prog [go go]"
;"prog"
;{"go" 0}
;
;"prog go"
;{"go" 1}
;
;"prog go go"
;{"go" 2}
;
;"prog go go go"
;"user-error"
;
;"usage: prog go..."
;"prog go go go go go"
;{"go" 5

;
; [options] does not include options: from usage-pattern}
;
;    "usage: prog [options] [-a]
;
;options: -a
;         -b
;"])
;"prog -a"
;{"-a" true, "-b" false}
;
;"prog -aa"
;"user-error"

;
    ; Test [options] shourtcut
;

;    "usage: prog [options] A
;Options
;    -q  Be quiet
;    -v  Be verbose.
;
;"])
;"prog arg"
;{"A" "arg", "-v" false, "-q" false}
;
;"prog -v arg"
;{"A" "arg", "-v" true, "-q" false}
;
;"prog -q arg"
;{"A" "arg", "-v" false, "-q" true
;
;;
;    ; Test single dash}
;;
;
;    "usage: prog [-]"}
;
;"prog -"
;{"-" true}
;
;"prog"
;{"-" false
;
;;
;    ; If argument is repeated, its value should always be a list}
;;
;
;    "usage: prog [NAME [NAME ...]]"}
;
;"prog a b"
;{"NAME" ["a", "b"]}
;
;"prog"
;{"NAME" []
;
;;
;    ; Option's argument defaults to nil/None
;;
;
;    "usage: prog [options]
;options
; -a        Add
; -m <msg>  Message
;
;"}
;"prog -a"
;{"-m" nil, "-a" true

;
; Test options: without description
;

    ["usage:
				prog --hello"
     ["prog --hello" {"--hello" true}]]

    ["usage:
				prog [--hello=<world>]"
     [["prog" {"--hello" nil}]
      ["prog --hello wrld" {"--hello" "wrld"}]]]

    ["usage:
				prog [-o]"
     [["prog" {"-o" false}]
      ["prog -o" {"-o" true}]]]

;"usage: prog [-opr]"
;"prog -op"
;{"-o" true, "-p" true, "-r" false}

    ["usage:
				git [-v | --verbose]"
     [["prog -v" {"-v" true, "--verbose" false}]]]

    ["usage:
				git remote [-v | --verbose]"
     [["prog remote -v" {"remote" true, "-v" true, "--verbose" false}]]]

;
; Test empty usage: pattern
;
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

;
; Option's argument should not capture default value from usage: pattern
;

    ["usage:
				prog [--file=<f>]"
     [["prog" {"--file" nil}]]]

    ["usage:
				prog [--file=<f>]

     options:
				--file <a>

     "
     [["prog" {"--file" nil}]]]

    ["usage:
				prog [-a <hostport>]

     options:
				-a, --address <hostport>  TCP address [default: localhost6283].

     "
     [["prog" {"--address" "localhost6283"}]]]

;
; If option with argument could be repeated,
; its arguments should be accumulated into a list
;
    ["usage:
				prog --long=<arg> ..."
     [["prog --long one" {"--long" ["one"]}]
      ["prog --long one --long two" {"--long" ["one", "two"]}]]]

;
; Test multiple elements repeated at once
;
    ["usage:
				prog (go <direction> --speed=<km/h>)..."
     [["prog  go left --speed=5  go right --speed=9"
       {"go" 2, "<direction>" ["left", "right"], "--speed" ["5", "9"]}]]]

;
; Required options: should work with option shortcut
;

;    "usage: prog [options] -a
;
;options: -a
;
;"])
;"prog -a"
;{"-a" true

;
; If option could be repeated its defaults should be split into a list
;
    ["usage:
				prog [-o <o>]...

     options:
				-o <o>  [default: x]

    "
     [["prog -o this -o that" {"-o" ["this", "that"]}]
      ["prog" {"-o" ["x"]}]]]

;"usage: prog [-o <o>]...
;
;options: -o <o>  [default: x y]
;
;"
;"prog -o this"
;{"-o" ["this"]}
;
;"prog"
;{"-o" ["x", "y"]

;
; Test stacked option's argument
;

;    "usage: prog -pPATH
;
;options: -p PATH
;
;"])
;"prog -pHOME"
;{"-p" "HOME"

;
; Issue 56 Repeated mutually exclusive args give nested lists sometimes
;
    ["usage:
				foo (--xx=x|--yy=y)..."
     [["prog --xx=1 --yy=2" {"--xx" ["1"], "--yy" ["2"]}]]]])

;
; POSIXly correct tokenization
;

;"usage: prog [<input file>]"
;"prog f.txt"
;{"<input file>" "f.txt"}
;
;"usage: prog [--input=<file name>]..."
;"prog --input a.txt --input=b.txt"
;{"--input" ["a.txt", "b.txt"]

;
; Issue 85 `[options]` shourtcut with multiple subcommands
;

;    "usage: prog good [options]
;           prog fail [options]
;
;options: --loglevel=N
;
;"])
;"prog fail --loglevel 5"
;{"--loglevel" "5", "fail" true, "good" false

;
; Usage-section syntax
;

;    "usageprog --foo"])
;"prog --foo"
;{"--foo" true}

;"Usage
; prog --foo
; prog --bar
;
;NOT PART OF SECTION"
;"prog --foo"
;{"--foo" true, "--bar" false}
;
;"Usage
; prog --foo
; prog --bar
;NOT PART OF SECTION"
;"prog --foo"
;{"--foo" true, "--bar" false

;
; Options-section syntax
;

;    "usage: prog [options]
;
;global options: --foo
;local options: --baz
;               --bar
;other options
; --egg
; --spam
;-not-an-option-
;
;"])
;"prog --baz --egg"
;{"--foo" false, "--baz" true, "--bar" false, "--egg" true, "--spam" false}

(s/def ::docstring string?)
(s/def ::input string?)
(s/def ::result-map (s/map-of string? any?))

(s/def ::test-result (s/or ::result-map string?))
(s/def ::test-entry (s/tuple ::input ::result))
(s/def ::test (s/tuple ::docstring ::test-entry))

