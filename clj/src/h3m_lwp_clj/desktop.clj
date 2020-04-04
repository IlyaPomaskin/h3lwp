(ns h3m-lwp-clj.desktop
  (:require
   [clojure.java.io :as io])
  (:import
   [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglApplicationConfiguration]
   [com.heroes3.livewallpaper.clojure LiveWallpaperEngine]
   [java.awt FileDialog Frame]
   [java.io File]))


(def ^FileDialog file-chooser
  (doto (new FileDialog (new Frame))
    (.setMode  FileDialog/LOAD)
    (.setFile "*.lod")
    (.setMultipleMode false)))


(defn create-engine
  ^LiveWallpaperEngine
  []
  (let [engine (new LiveWallpaperEngine)]
    (.setFileSelectHandler
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
  (->> "../android/assets/sprites/h3/"
       (io/file)
       (.listFiles)
       (vec)
       (mapv
        (fn [^File file]
          (.delete file)))))


(def config (new LwjglApplicationConfiguration))
(set! (.-backgroundFPS config) 60)
(set! (.-foregroundFPS config) 60)
(set! (.-x config) 0)
(set! (.-y config) 0)
(set! (.-width config) 426)
(set! (.-height config) 500)


(defn -main []
  (delete-h3-sprites)
  (let [engine (create-engine)]
    (new LwjglApplication engine config)))
