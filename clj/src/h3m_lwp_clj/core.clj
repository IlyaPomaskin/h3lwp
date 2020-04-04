(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx ApplicationAdapter Gdx]
   [com.badlogic.gdx.graphics GL20]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [java.io FileInputStream])
  (:require
   [h3m-lwp-clj.settings :as settings]
   [h3m-lwp-clj.wallpaper :as wallpaper]
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.parser :as parser])
  (:gen-class
   :name com.heroes3.livewallpaper.clojure.LiveWallpaperEngine
   :extends com.badlogic.gdx.ApplicationAdapter
   :methods [[setFileSelectHandler [Runnable] void]
             [selectFile [String] void]]))


(defonce renderer (atom nil))
(defonce h3m-map (atom nil))
(defonce settings
  (atom {:on-file-select-click nil
         :progress-bar-length 0
         :progress-bar-value 0
         :position-update-interval (* 60 15)}))


(defonce viewport (new ScreenViewport))


(defn set-renderer
  [next-renderer]
  (reset! renderer next-renderer)
  (.setInputProcessor (Gdx/input) (next-renderer)))


(defn -create
  [^ApplicationAdapter _]
  (assets/init)
  (reset!
   h3m-map
   (->> "maps/invasion.h3m"
        (.internal Gdx/files)
        (.read)
        (parser/parse-map)))
  (if (assets/assets-ready?)
    (set-renderer (wallpaper/create-renderer settings viewport @h3m-map))
    (set-renderer (settings/create-renderer settings viewport))))


(defn -resize
  [^ApplicationAdapter _ ^long width ^long height]
  (.update ^ScreenViewport viewport width height false))


(defn -render
  [^ApplicationAdapter _]
  (.glClear Gdx/gl GL20/GL_COLOR_BUFFER_BIT)
  (@renderer))


(defn -setFileSelectHandler
  [^ApplicationAdapter _ ^Runnable file-select-handler]
  (swap! settings assoc :on-file-select-click (fn [] (.run file-select-handler))))


(defn -selectFile
  [^ApplicationAdapter _ ^String path]
  (future
    (parser/parse-map-sprites
     (new FileInputStream (.file (.absolute Gdx/files path)))
     (.local Gdx/files consts/atlas-file-name)
     (.local Gdx/files consts/edn-file-name)
     (fn [length index]
       (swap! settings assoc
              :progress-bar-length length
              :progress-bar-value index))
     (fn []
       (.postRunnable
        Gdx/app
        (reify Runnable
          (run
            [_]
            (assets/init)
            (set-renderer (wallpaper/create-renderer settings viewport @h3m-map)))))))))