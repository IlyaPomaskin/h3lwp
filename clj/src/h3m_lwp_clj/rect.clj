(ns h3m-lwp-clj.rect
  (:require [h3m-lwp-clj.consts :as consts]))


(defn get-random
  [size screen-width screen-height]
  (let [width (int (Math/ceil (/ screen-width consts/tile-size)))
        height (int (Math/ceil (/ screen-height consts/tile-size)))
        x1 (max 1 (rand-int (- size width)))
        y1 (max 1 (rand-int (- size height)))
        x2 (min size (+ x1 width))
        y2 (min size (+ y1 height))]
    {:x1 x1
     :y1 y1
     :x2 x2
     :y2 y2}))


(defn contain?
  [x y {x1 :x1
        y1 :y1
        x2 :x2
        y2 :y2}]
  (and (>= x x1)
       (<= x x2)
       (>= y y1)
       (<= y y2)))