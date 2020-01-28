(ns h3m-lwp-clj.desktop
  (:import
   [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglApplicationConfiguration]
   [com.heroes3.livewallpaper.clojure LiveWallpaperEngine]))

(defn -main []
  (let [config (new LwjglApplicationConfiguration)]
    (set! (.-height config) 1136)
    (set! (.-width config) 640)
    (new LwjglApplication (new LiveWallpaperEngine) config)))

(defn main-repl
  []
  (new LwjglApplication (new LiveWallpaperEngine)))