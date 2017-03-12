(defproject org.clojars.carocad/docopt "0.1.0"
  :description "Docopt creates beautiful command-line interfaces - clojure (unnoficial) port"
  :url "https://github.com/carocad/docopt.cluno"
  :license {:name "MIT" :url "https://raw.githubusercontent.com/carocad/docopt.cluno/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [instaparse "1.4.5"]]
  :profiles {:test {:dependencies [[org.clojure/data.json "0.2.6"]]} :uberjar {:aot :all}}
  :main ^:skip-aot docopt.core
  :target-path "target/%s"
  :aot :all)
