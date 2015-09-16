# docopt.cluno
![version](https://img.shields.io/badge/version-0.1-blue.svg)
[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg?style=plastic)](https://raw.githubusercontent.com/carocad/docopt.clj/master/LICENSE)

Creates beautiful command-line interfaces using only your docstring. After all, a good help message has all necessary information in it to make a parser

Clojure (unnoficial) implementation of the [docopt](http://docopt.org/) language,

## Usage
Docopt.cluno exposes a single function in docopt.core called "docopt". You can call it with:
- 0 arguments; docstrings fetched from -main :doc metadata and *command-line-args* clojure's global reference.
- 1 argument = docstring
- 2 arguments = docstring arguments list

Sadly a clojar is not yet available. Please be patient.

No arguments, just let the magic beging !

``` clojure
(ns example.core
  (:require [docopt.core :refer [docopt]]))

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
```

## Tests
Run `lein test` to validate all tests.

# Notes
This project does NOT yet cover all of the original docopt Python implementation. Some of them are yet to be implemented and some are not going to be implemented at all due to a difference of opinion in the ambiguities that the docopt language should accept.

Pull request and are more than welcome :)

copyright Camilo Roca
