(ns h3m-lwp-clj.wallpaper
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils Timer Timer$Task])
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.orth-camera :as orth-camera]
   [h3m-lwp-clj.input-processor :as input-processor]
   [h3m-lwp-clj.protocol :as protocol]))


(def camera-position-update-interval (* 60 15))
(def scale-factor 0.5)


(defonce h3m-map (atom nil))
(defonce camera (atom nil))
(defonce terrain-renderer (atom nil))
(defonce objects-renderer (atom nil))
(defonce timer (atom []))
(defonce camera-controller (atom nil))


(defn create-renderer
  []
  (assets/init)
  (reset! h3m-map (h3m-parser/parse-h3m (.read (.internal Gdx/files "maps/invasion.h3m"))))
  (reset! camera (orth-camera/create scale-factor))
  (reset! camera-controller (input-processor/create @camera (:size @h3m-map)))
  (reset! terrain-renderer (terrain/create-renderer @h3m-map))
  (reset! objects-renderer (objects/create-renderer @h3m-map))
  (reset!
   timer
   (doto (new Timer)
     (.scheduleTask
      (proxy [Timer$Task] []
        (run [] (.requestRendering (Gdx/graphics))))
      (float 0)
      (float consts/animation-interval))
     (.scheduleTask
      (proxy [Timer$Task] []
        (run [] (do
                  (orth-camera/set-random-position @camera (:size @h3m-map))
                  (.update @camera))))
      (float 0)
      (float camera-position-update-interval))
     (.stop)))
  (reify protocol/Renderer
    (start
      [this]
      (.start ^Timer @timer)
      (.setContinuousRendering (Gdx/graphics) false)
      (.setInputProcessor (Gdx/input) camera-controller))
    (stop
      [this]
      (.stop ^Timer @timer)
      (.setContinuousRendering (Gdx/graphics) true))
    (render
      [this]
      (.update @camera)
      (@terrain-renderer @camera)
      (@objects-renderer @camera))))
