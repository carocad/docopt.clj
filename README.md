# docopt.clj

Clojure implementation of the [docopt](http://docopt.org/) language, version 0.6,
under a [MIT license](http://github.com/docopt/docopt.clj/blob/master/LICENSE).

## Usage

Add `[docopt "0.6.1"]` to your dependencies in `project.clj`

``` clojure
(ns example.core
  (:require [docopt.core :refer [docopt]])) ; import the docopt function from docopt.core

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
  --moored      Moored (anchored) mine.
  --drifting    Drifting mine."

  [& args]
  (let [arguments (docopt args)]
        (println arguments))) ; with only one argument, docopt parses -main's docstring.
```

## Tests

Run `lein test` to validate all tests.
The tests are automatically downloaded from the language-agnostic
`testcases.docopt` file in the reference implementation, master branch commit
[511d1c57b5](https://github.com/docopt/docopt/tree/511d1c57b59cd2ed663a9f9e181b5160ce97e728).
Please feel free to (re)open an issue in case this implementation falls behind.

