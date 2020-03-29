(ns h3m-lwp-clj.wallpaper
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils Timer Timer$Task])
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.orth-camera :as orth-camera]
   [h3m-lwp-clj.input-processor :as input-processor]))


(defn create-renderer
  [settings]
  (let [{scale :scale
         update-position-interval :update-position-interval} @settings
        h3m-map (h3m-parser/parse-h3m (.read (.internal Gdx/files "maps/invasion.h3m")))
        camera (orth-camera/create)
        camera-controller (input-processor/create camera (:size h3m-map))
        terrain-renderer (terrain/create-renderer h3m-map)
        objects-renderer (objects/create-renderer h3m-map)]
    (set! (.-zoom camera) scale)
    (add-watch
     settings
     :scale-change
     (fn [_ _ _ next-settings]
       (set! (.-zoom camera) (:scale next-settings))))
    (.scheduleTask
     (new Timer)
     (proxy [Timer$Task] []
       (run []
         (do
           (orth-camera/set-random-position camera (:size h3m-map))
           (.update camera))))
     (float 0)
     (float update-position-interval))
    (fn []
      (.update camera)
      (terrain-renderer camera)
      (objects-renderer camera)
      camera-controller)))
