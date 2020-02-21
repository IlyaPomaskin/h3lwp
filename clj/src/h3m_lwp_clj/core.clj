(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx InputMultiplexer ApplicationAdapter Gdx]
   [com.badlogic.gdx.graphics GL20]
   [java.io FileInputStream])
  (:require
   [h3m-lwp-clj.settings :as settings]
   [h3m-lwp-clj.wallpaper :as wallpaper]
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.parser :as parser])
  (:gen-class
   :name com.heroes3.livewallpaper.clojure.LiveWallpaperEngine
   :extends com.badlogic.gdx.ApplicationAdapter
   :methods [[onFileSelectClick [Runnable] void]
             [selectFile [String] void]
             [setIsPreview [Boolean] void]]))


(defonce state
  (atom {:wallpaper-renderer nil
         :settings-renderer nil
         :is-preview true}))
(defonce settings
  (atom {:on-file-select-click nil
         :selected-file ""
         :progress-bar-length 0
         :progress-bar-value 0}))


(defn -create
  [^ApplicationAdapter _]
  (let [wp (wallpaper/create-renderer)
        st (settings/create-renderer settings)]
    (swap!
     state
     assoc
     :wallpaper-renderer wp
     :settings-renderer st)
    (.setInputProcessor
     (Gdx/input)
     (doto (new InputMultiplexer)
       (.addProcessor (st))
       (.addProcessor (wp))))))


(defn -render
  [^ApplicationAdapter _]
  (doto Gdx/gl
    (.glClearColor 0 0 0 0)
    (.glClear GL20/GL_COLOR_BUFFER_BIT))
  (let [{is-preview :is-preview
         wallpaper-renderer :wallpaper-renderer
         settings-renderer :settings-renderer} @state]
    (wallpaper-renderer)
    (when (true? is-preview)
      (settings-renderer))))


(defn -onFileSelectClick
  [^ApplicationAdapter _ ^Runnable callback]
  (swap! settings assoc :on-file-select-click (fn [] (.run callback))))


(defn -selectFile
  [^ApplicationAdapter _ ^String path]
  (let [lod-file (new FileInputStream (.file (.absolute Gdx/files path)))
        list (parser/get-lod-files-list lod-file)]
    (future
      (swap!
       settings
       assoc
       :selected-file path
       :progress-bar-length (count list)
       :progress-bar-value 0)
      (parser/parse-map-sprites
       list
       lod-file
       (.local Gdx/files consts/atlas-file-name)
       (.local Gdx/files consts/edn-file-name)
       (fn [_] (swap! settings update-in [:progress-bar-value] inc))
       (fn []
         (.postRunnable
          Gdx/app
          (reify Runnable
            (run [_]
              (swap!
               state
               assoc
               :wallpaper-renderer (wallpaper/create-renderer))))))))))


(defn -setIsPreview
  [^ApplicationAdapter _ ^Boolean is-preview?]
  (swap! state assoc :is-preview is-preview?))
