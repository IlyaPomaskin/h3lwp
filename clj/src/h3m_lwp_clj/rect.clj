(ns h3m-lwp-clj.rect
  (:import [com.badlogic.gdx.graphics OrthographicCamera]
           [com.badlogic.gdx.math Rectangle])
  (:require [h3m-lwp-clj.consts :as consts]))


(defn contain?
  [x y {x1 :x1
        y1 :y1
        x2 :x2
        y2 :y2}]
  (and (>= x x1)
       (<= x x2)
       (>= y y1)
       (<= y y2)))


(defn add [rect amount]
  {:x1 (- (:x1 rect) amount)
   :y1 (- (:y1 rect) amount)
   :width (+ (:width rect) (* 2 amount))
   :height (+ (:height rect) (* 2 amount))
   :x2 (+ (:x2 rect) amount)
   :y2 (+ (:y2 rect) amount)})


(defn get-camera-rect [^OrthographicCamera camera]
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


(defn get-camera-rectangle [^OrthographicCamera camera]
  (let [{x1 :x1
         y1 :y1
         width :width
         height :height} (get-camera-rect camera)]
    (new Rectangle x1 y1 width height)))