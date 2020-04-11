(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx ApplicationAdapter Gdx]
   [com.badlogic.gdx.graphics GL20]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [com.heroes3.livewallpaper.AssetsParser Main]
   [java.io InputStream])
  (:require
   [clojure.string :as str]
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
             [setStoragePermissionStatus [Boolean] void]
             [selectFile [String] void]]))


(defonce renderer (atom nil))
(defonce h3m-map (atom nil))
(defonce settings
  (atom {:on-file-select-click     nil
         :state                    :wait
         :error                    ""
         :position-update-interval (* 60 15)}))
(defonce viewport (new ScreenViewport))


(defn set-error
  [^Exception exception]
  (swap! settings assoc
         :state :error
         :error (.getMessage exception)))


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
  (reset!
   h3m-map
   (parse-map (.read (.internal Gdx/files "maps/invasion.h3m"))))
  (reset!
   renderer
   (if (assets/try-load-atlas)
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
  (swap! settings assoc :on-file-select-click
         (fn []
           (swap! settings assoc :state :wait :error "")
           (.run file-select-handler))))


(defn -setStoragePermissionStatus
  [^ApplicationAdapter _ ^Boolean has-permission?]
  (when (not has-permission?)
    (swap! settings assoc :state :no-storage-permission)))


(defn run-on-main-thread
  [function]
  (.postRunnable
   Gdx/app
   (reify Runnable
     (run [_] (function)))))


(defn -selectFile
  [^ApplicationAdapter _ ^String path]
  (let [path-valid? (-> path
                        (str/lower-case)
                        (str/ends-with? "h3sprite.lod"))]
    (if (not path-valid?)
      (set-error (new Exception "Wrong file selected"))
      (future
        (swap! settings assoc :state :parsing)
        (try
          (Main/parseAtlas
           (.absolute Gdx/files path)
           (.local Gdx/files consts/atlas-file-name))
          (catch Exception e (set-error e)))
        (swap! settings assoc :state :loading)
        (run-on-main-thread
         (fn start-after-parsing []
           (if (assets/try-load-atlas)
             (reset! renderer (wallpaper/create-renderer settings viewport @h3m-map))
             (set-error (new Exception "Failed to load assets")))))))))


(comment
  (run-on-main-thread
   (fn []
     (reset! renderer (wallpaper/create-renderer settings viewport h3m-map)))))