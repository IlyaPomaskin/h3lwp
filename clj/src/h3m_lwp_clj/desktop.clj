(ns h3m-lwp-clj.desktop
  (:require
   [clojure.java.io :as io])
  (:import
   [com.badlogic.gdx.backends.lwjgl LwjglApplication]
   [com.badlogic.gdx.utils Timer Timer$Task]
   [com.heroes3.livewallpaper.clojure LiveWallpaperEngine]
   [java.awt FileDialog Frame]
   [java.io File]))


(def is-preview-timeout (float 3))


(def ^FileDialog file-chooser
  (doto (new FileDialog (new Frame))
    (.setMode  FileDialog/LOAD)
    (.setFile "*.lod")
    (.setMultipleMode false)))


(defn create-engine
  ^LiveWallpaperEngine
  []
  (let [engine (new LiveWallpaperEngine)]
    (.onFileSelectClick
     engine
     (reify
       Runnable
       (run [_]
         (.show file-chooser)
         (let [file (.getFile file-chooser)
               directory (.getDirectory file-chooser)
               file-path (format "%s%s" directory file)]
           (when (and (some? file) (some? directory))
             (.selectFile engine file-path))))))
    engine))


(defn delete-h3-sprites []
  (let [files (-> "../android/assets/sprites/h3/"
                  (io/file)
                  (.listFiles)
                  (vec))]
    (for [file files]
      (.delete ^File file))))


(defn -main []
  (delete-h3-sprites)
  (let [engine (create-engine)]
    (new LwjglApplication engine)
    (.scheduleTask
     (new Timer)
     (proxy [Timer$Task] []
       (run []
         (.setIsPreview engine true)))
     is-preview-timeout)))


