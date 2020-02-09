(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx InputMultiplexer ApplicationAdapter Gdx]
   [com.badlogic.gdx.graphics GL20])
  (:require
   [h3m-lwp-clj.settings :as settings]
   [h3m-lwp-clj.wallpaper :as wallpaper])
  (:gen-class
   :name com.heroes3.livewallpaper.clojure.LiveWallpaperEngine
   :extends com.badlogic.gdx.ApplicationAdapter
   :methods [[onFileSelectClick [Runnable] void]
             [setFilePath [String] void]
             [setIsPreview [Boolean] void]]))


(defmacro repl
  [body]
  `(.postRunnable
    Gdx/app
    (reify Runnable
      (run [_]
        (try
          ~body
          (catch
           Exception
           e#
            (do
              (println (str "repl exception: " (.getMessage e#)))
              (println (.printStackTrace e#)))))))))


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


(defn -setFilePath
  [^ApplicationAdapter _ ^String path]
  (swap! settings assoc :selected-file path))


(defn -setIsPreview
  [^ApplicationAdapter _ ^Boolean is-preview?]
  (swap! state assoc :is-preview is-preview?))
