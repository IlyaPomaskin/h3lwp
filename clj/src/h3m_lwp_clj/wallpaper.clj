(ns h3m-lwp-clj.wallpaper
  (:import
   [com.badlogic.gdx Gdx])
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.orth-camera :as orth-camera]
   [h3m-lwp-clj.input-processor :as input-processor]))


(defn create-renderer
  [settings]
  (let [{position-update-interval :position-update-interval} @settings
        h3m-map (h3m-parser/parse-h3m (.read (.internal Gdx/files "maps/invasion.h3m")))
        camera (orth-camera/create)
        camera-controller (input-processor/create camera (:size h3m-map))
        terrain-renderer (terrain/create-renderer h3m-map)
        objects-renderer (objects/create-renderer h3m-map)]
    (orth-camera/subscribe-to-scale camera settings :scale)
    (orth-camera/set-camera-updation-timer camera (:size h3m-map) position-update-interval)
    (fn []
      (terrain-renderer camera)
      (objects-renderer camera)
      camera-controller)))
