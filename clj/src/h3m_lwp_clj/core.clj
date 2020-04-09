(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx ApplicationAdapter Gdx]
   [com.badlogic.gdx.graphics GL20]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [com.heroes3.livewallpaper.AssetsParser Main]
   [java.io InputStream])
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-lwp-clj.settings :as settings]
   [h3m-lwp-clj.wallpaper :as wallpaper]
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.random :as random])
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


(defn parse-map [^InputStream file]
  (let [h3m-map (h3m-parser/parse-h3m file)]
    (update
     h3m-map
     :objects
     (fn [objects]
       (->> objects
            (map #(assoc % :def (nth (:defs h3m-map) (:def-index %))))
            (map random/replace-random-objects))))))


(defn -create
  [^ApplicationAdapter _]
  (assets/init)
  (reset!
   h3m-map
   (parse-map (.read (.internal Gdx/files "maps/test.h3m"))))
  (reset!
   renderer
   (if (assets/assets-ready?)
     (wallpaper/create-renderer settings viewport @h3m-map)
     (settings/create-renderer settings viewport))))


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
    (time
     (try
       (Main/parseAtlas
        (.absolute Gdx/files path)
        (.local Gdx/files consts/atlas-file-name))
        ;  TODO
        ;  (fn [length index]
        ;    (swap! settings assoc
        ;           :progress-bar-length length
        ;           :progress-bar-value index))
       (.postRunnable
        Gdx/app
        (reify Runnable
          (run
            [_]
            (assets/init)
            (reset! renderer (wallpaper/create-renderer settings viewport @h3m-map)))))
       (catch Exception e (println e))))))