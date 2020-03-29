(ns h3m-lwp-clj.orth-camera
  (:import
   [com.badlogic.gdx.graphics OrthographicCamera]
   [com.badlogic.gdx.utils Timer Timer$Task])
  (:require
   [h3m-lwp-clj.consts :as consts]))


(defn create []
  (doto (new OrthographicCamera)
    (.setToOrtho true)
    (.update)))


(defn get-rect [^OrthographicCamera camera]
  (let [width (* (.viewportWidth camera) (.zoom camera))
        height (* (.viewportHeight camera) (.zoom camera))
        w (+ (* width (Math/abs (.y (.up camera))))
             (* height (Math/abs (.x (.up camera)))))
        h (+ (* width (Math/abs (.x (.up camera))))
             (* height (Math/abs (.y (.up camera)))))
        x1 (- (.x (.position camera))
              (/ w 2))
        y1 (- (.y (.position camera))
              (/ h 2))
        x1-tile (int (Math/floor (/ x1 consts/tile-size)))
        y1-tile (int (Math/floor (/ y1 consts/tile-size)))
        width-tile (int (Math/ceil (/ width consts/tile-size)))
        height-tile (int (Math/ceil (/ height consts/tile-size)))]
    {:x1 x1-tile
     :y1 y1-tile
     :width width-tile
     :height height-tile
     :x2 (+ x1-tile width-tile)
     :y2 (+ y1-tile height-tile)}))


(defn set-random-position
  [^OrthographicCamera camera map-size]
  (let [box (get-rect camera)
        x-offset (Math/floor (/ (:width box) 2))
        y-offset (Math/floor (/ (:height box) 2))
        next-x (* consts/tile-size
                  (+ x-offset (rand-int (- map-size (:width box)))))
        next-y (* consts/tile-size
                  (+ y-offset (rand-int (- map-size (:height box)))))]
    (.set (.position camera) next-x next-y 0)))


(defn set-camera-updation-timer
  [^OrthographicCamera camera map-size position-update-interval]
  (.scheduleTask
   (new Timer)
   (proxy [Timer$Task] []
     (run []
       (set-random-position camera map-size)))
   (float 0)
   (float position-update-interval)))
