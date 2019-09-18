(defproject clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [h3m-parser "1.0.0"]]
  :main h3m-lwp-clj.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles
  {:provided {:dependencies
              [[com.badlogicgames.gdx/gdx "1.9.6"]
               [com.badlogicgames.gdx/gdx-backend-lwjgl "1.9.6"]
               [com.badlogicgames.gdx/gdx-platform "1.9.6" :classifier "natives-desktop"]]}
   :uberjar {:aot :all
             :uberjar-name "clj-render-standalone.jar"}})
