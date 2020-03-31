(defproject h3m-lwp-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [h3m-parser "1.2.4"]]
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :global-vars {clojure.core/*warn-on-reflection* true}
  :aot :all
  :main h3m-lwp-clj.desktop
  :repl-options {:init-ns h3m-lwp-clj.desktop
                 :init (-main)}
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles
  {:provided {:dependencies
              [[nrepl "0.6.0"]
               [com.badlogicgames.gdx/gdx "1.9.10"]
               [com.badlogicgames.gdx/gdx-backend-lwjgl "1.9.10"]
               [com.badlogicgames.gdx/gdx-platform "1.9.10" :classifier "natives-desktop"]]}
   :uberjar {:uberjar-name "clj-render-standalone.jar"}})
