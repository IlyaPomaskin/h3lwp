(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx ApplicationAdapter Gdx]
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


(defonce wallpaper-renderer (atom nil))
(defonce settings-renderer (atom nil))
(defonce on-file-select-click-fn (atom (fn [] (println "UNSET FN"))))
(defonce selected-file-path (atom ""))
(defonce is-preview (atom true))
(defonce renderer (atom nil))


(add-watch
 is-preview
 :renderer-switch
 (fn [_ _ _ next-value]
   (reset!
    renderer
    (if next-value
      @settings-renderer
      @wallpaper-renderer))))


(add-watch
 renderer
 :renderer-switch
 (fn [_ _ prev-renderer next-renderer]
   (when prev-renderer
     (.stop prev-renderer))
   (.start next-renderer)))


(defn -create
  [^ApplicationAdapter _]
  #_(time
     (parser/parse-map-sprites
      (.internal Gdx/files "data/H3sprite.lod")
      (.local Gdx/files consts/atlas-file-name)
      (.local Gdx/files consts/edn-file-name)))
  (reset! wallpaper-renderer (wallpaper/create-renderer))
  (reset! settings-renderer (settings/create-renderer on-file-select-click-fn selected-file-path is-preview))
  (reset! is-preview true))


(defn -render
  [^ApplicationAdapter _]
  (doto Gdx/gl
    (.glClearColor 0 0 0 0)
    (.glClear GL20/GL_COLOR_BUFFER_BIT))
  (.render @renderer))


(defn -onFileSelectClick
  [^ApplicationAdapter _ ^Runnable callback]
  (reset! on-file-select-click-fn (fn [] (.run callback))))


(defn -setFilePath
  [^ApplicationAdapter _ ^String path]
  (reset! selected-file-path path))


(defn -setIsPreview
  [^ApplicationAdapter _ ^Boolean is-preview?]
  (reset! is-preview is-preview?))
