# docopt.clj
[![GitHub license](https://img.shields.io/github/license/mashape/apistatus.svg?style=plastic)](https://github.com/carocad/docopt.cluno/blob/master/LICENSE)
[![Build Status](https://travis-ci.org/carocad/docopt.clj.svg?branch=master)](https://travis-ci.org/carocad/docopt.clj)

[![Clojars Project](http://clojars.org/org.clojars.carocad/docopt/latest-version.svg)](http://clojars.org/org.clojars.carocad/docopt)

Creates beautiful command-line interfaces using only your docstring. After all, a good help message has all necessary information in it to make a parser

Clojure (unnoficial) implementation of [docopt](http://docopt.org/),

## Usage
`docopt` exposes a single function in `docopt.core` called `parse`. Example usage:

``` clojure
(ns example.core
  (:require [docopt.core :as docopt]))

(defn -main
 "Naval Fate.

  Usage:
  naval_fate.py ship new <name>...
  naval_fate.py ship <name> move <x> <y> [--speed]
  naval_fate.py ship shoot <x> <y>
  naval_fate.py mine (set|remove) <x> <y> [--moored | --drifting]
  naval_fate.py (-h | --help)
  naval_fate.py --version

  Options:
  -h --help     Show this screen.
  --version     Show version.
  --speed=<kn>  Speed in knots [default: 10].
  --moored      Moored (anchored) mine.
  --drifting    Drifting mine."
  [& args]
  (docopt/parse (:doc (meta #'-main)) args))
```

you can test your cli parser by passing a sequence of string arguments to the `parse` function. The same way that you get from the console.

``` clojure
(-main "mine" "set" "10" "20" "--drifting")
; => {:mine "mine", :set "set", :<x> "10", :<y> "20", :--drifting "--drifting"}
```

## Tests
Run `lein test` to validate all tests.

## Notes
This project does NOT completely follow the original docopt `Python`
implementation. Some of them are yet to be implemented and some are not
going to be implemented at all due to a difference of opinion in the
ambiguities that the docopt language should accept.

Enjoy it :)

`Copyright (c) 2015 Camilo Roca`

## differences with the original implementation
- *default* values are not implemented yet
- short options cannot be grouped yet
- *arguments* (positional and option's arguments) MUST be wrapped in `<>`
- repeated elements MUST be declared with (`...`), i.e. repeating the same name isn't valid
  e.g. `example <port> <port>` *invalid !!* reported as a single `<port>`
- `[options]`,`[-]` `[--]` special meaning not supported
- every option MUST be declared in the `options:` section
- option's arguments MUST be declared in the `Options:` section
- option's arguments should be used with either `:` or `=`, e.g. `--speed=10`
