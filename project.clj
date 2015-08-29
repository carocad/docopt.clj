(defproject docopt "0.6.1"
  :description "docopt creates beautiful command-line interfaces - clojure port"
  :url "http://docopt.org"
  :license {:name "MIT" :url "https://raw.githubusercontent.com/carocad/docopt.cluno/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [instaparse "1.4.1"]]
  :profiles {:test {:dependencies [[org.clojure/data.json "0.2.1"]]}}
  :aot :all)
