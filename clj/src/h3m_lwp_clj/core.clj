(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx InputMultiplexer ApplicationAdapter Gdx]
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
   :methods [[onFileSelectClick [Runnable] void]
             [selectFile [String] void]
             [setIsPreview [Boolean] void]]))


(defn set-pref [name value]
  (-> (.getPreferences Gdx/app consts/settings-name)
      (.putFloat name value)
      (.flush)))


(defn get-pref [name]
  (.getFloat (.getPreferences Gdx/app consts/settings-name) name))


(defonce state
  (atom {:wallpaper-renderer nil
         :settings-renderer nil
         :is-preview false}))


(defonce settings
  (atom {:on-file-select-click nil
         :progress-bar-length 0
         :progress-bar-value 0
         :on-scale-change
         (fn [value]
           (set-pref "scale" (float value))
           (swap! settings assoc :scale value))
         :scale 0.5
         :update-position-interval (* 60 15)}))


(defn -create
  [^ApplicationAdapter _]
  (swap! settings assoc
         :scale (get-pref "scale"))
  (let [wp (wallpaper/create-renderer settings)
        st (settings/create-renderer settings)]
    (swap! state assoc
           :wallpaper-renderer wp
           :settings-renderer st
           :is-preview (not (assets/assets-ready?)))
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
  (let [{wallpaper-renderer :wallpaper-renderer
         settings-renderer :settings-renderer
         is-preview :is-preview} @state]
    (wallpaper-renderer)
    (when (true? is-preview)
      (settings-renderer))))


(defn -onFileSelectClick
  [^ApplicationAdapter _ ^Runnable callback]
  (swap! settings assoc :on-file-select-click (fn [] (.run callback))))


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
          (run [_]
            (swap! state assoc
                   :wallpaper-renderer (wallpaper/create-renderer settings)))))))))


(defn -setIsPreview
  [^ApplicationAdapter _ ^Boolean is-preview?]
  ;; (swap! state assoc :is-preview is-preview?)
  )
