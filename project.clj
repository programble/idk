(defproject idk "0.1.0-SNAPSHOT"
  :description "I don't know, a site for questions and answers"
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [ring/ring-jetty-adapter "0.2.0"]
                 [ring/ring-core "0.2.0"]
                 [net.cgrand/moustache "1.0.0-SNAPSHOT"]
                 [hiccup "0.2.4"]
                 [stupiddb "0.3.0"]]
  :dev-dependencies [[swank-clojure "1.2.0-SNAPSHOT"]
                     [lein-search "0.3.0-SNAPSHOT"]])
