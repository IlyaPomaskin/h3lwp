(ns h3m-lwp-clj.wallpaper
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils Timer Timer$Task])
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.orth-camera :as orth-camera]
   [h3m-lwp-clj.input-processor :as input-processor]))


(def camera-position-update-interval (* 60 15))
(def scale-factor 0.5)


(defn create-renderer
  []
  (assets/init)
  (let [h3m-map (h3m-parser/parse-h3m (.read (.internal Gdx/files "maps/invasion.h3m")))
        camera (orth-camera/create scale-factor)
        camera-controller (input-processor/create camera (:size h3m-map))
        terrain-renderer (terrain/create-renderer h3m-map)
        objects-renderer (objects/create-renderer h3m-map)]
    (doto (new Timer)
      (.scheduleTask
       (proxy [Timer$Task] []
         (run [] (do
                   (orth-camera/set-random-position camera (:size h3m-map))
                   (.update camera))))
       (float 0)
       (float camera-position-update-interval)))
    (fn []
      (.update camera)
      (terrain-renderer camera)
      (objects-renderer camera)
      camera-controller)))
