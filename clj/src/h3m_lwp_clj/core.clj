(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx ApplicationAdapter Gdx]
   [com.badlogic.gdx.graphics GL20]
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


(defonce state
  (atom {:renderer nil}))


(defonce settings
  (atom {:on-file-select-click nil
         :progress-bar-length 0
         :progress-bar-value 0
         :position-update-interval (* 60 15)}))


(defn set-renderer
  [renderer]
  (swap! state assoc :renderer renderer)
  (.setInputProcessor (Gdx/input) (renderer)))


(defn -create
  [^ApplicationAdapter _]
  (assets/init)
  (if (assets/assets-ready?)
    (set-renderer (wallpaper/create-renderer settings))
    (set-renderer (settings/create-renderer settings))))


(defn -render
  [^ApplicationAdapter _]
  (doto Gdx/gl
    (.glClearColor 0 0 0 0)
    (.glClear GL20/GL_COLOR_BUFFER_BIT))
  (let [{renderer :renderer} @state]
    (renderer)))


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
            (set-renderer (wallpaper/create-renderer settings)))))))))
