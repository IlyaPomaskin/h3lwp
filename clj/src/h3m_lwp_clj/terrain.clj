(ns h3m-lwp-clj.terrain
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch])
  (:require [h3m-lwp-clj.assets :as assets]
            [h3m-lwp-clj.rect :as rect]
            [h3m-lwp-clj.consts :as consts]))


(defn tile->filename
  [tile part]
  (get-in consts/terrain [part (get tile part)]))


(defn render-tile
  [^SpriteBatch batch tile]
  (let [{mirror-config :mirror-config
         x-position :x-position
         y-position :y-position
         render-terrain :render-terrain
         render-river :render-river
         render-road :render-road} tile
        flip-terrain-x (bit-test mirror-config 0)
        flip-terrain-y (bit-test mirror-config 1)
        flip-river-x (bit-test mirror-config 2)
        flip-river-y (bit-test mirror-config 3)
        flip-road-x (bit-test mirror-config 4)
        flip-road-y (bit-test mirror-config 5)]
    (.draw
     batch
     (render-terrain)
     (+ (float x-position) (if flip-terrain-x (- consts/tile-size) 0))
     (+ (float y-position) (if flip-terrain-y (- consts/tile-size) 0))
     (float consts/tile-size)
     (float consts/tile-size)
     (float consts/tile-size)
     (float consts/tile-size)
     (float (if flip-terrain-x -1 1))
     (float (if flip-terrain-y -1 1))
     (float 0))
    (when (fn? render-river)
      (.draw
       batch
       (render-river)
       (+ (float x-position) (if flip-river-x (- consts/tile-size) 0))
       (+ (float y-position) (if flip-river-y (- consts/tile-size) 0))
       (float consts/tile-size)
       (float consts/tile-size)
       (float consts/tile-size)
       (float consts/tile-size)
       (float (if flip-river-x -1 1))
       (float (if flip-river-y -1 1))
       (float 0)))
    (when (fn? render-road)
      (.draw
       batch
       (render-road)
       (+ (float x-position) (if flip-road-x (- consts/tile-size) 0))
       (+ (float y-position) (if flip-road-y (- consts/tile-size) 0))
       (float consts/tile-size)
       (float consts/tile-size)
       (float consts/tile-size)
       (float consts/tile-size)
       (float (if flip-road-x -1 1))
       (float (if flip-road-y -1 1))
       (float 0)))))


(defn get-visible-tiles
  [rect
   {size :size
    has-underground? :has-underground?
    terrain :terrain}]
  (doall
   (for [i (range 0 (* (* size size)
                       (if has-underground? 2 1)))
         :let [x (mod i size)
               y (int (Math/ceil (quot i size)))
               tile (nth terrain i)]
         :when (rect/contain? x y rect)]
     (let [{river :river
            road :road
            terrain-image-index :terrain-image-index
            river-image-index :river-image-index
            road-image-index :road-image-index} tile]
       (assoc tile
              :x-position (* (dec x) consts/tile-size)
              :y-position (* (dec y) consts/tile-size)
              :render-terrain (assets/create-sprite
                               (assets/get-terrain
                                (tile->filename tile :terrain)
                                terrain-image-index)
                               false)
              :render-river (when (pos? river)
                              (assets/create-sprite
                               (assets/get-terrain
                                (tile->filename tile :river)
                                river-image-index)
                               false))
              :render-road (when (pos? road)
                             (assets/create-sprite
                              (assets/get-terrain
                               (tile->filename tile :road)
                               road-image-index)
                              false)))))))


(defn render-terrain-tiles
  [batch terrain-tiles]
  (doseq [tile terrain-tiles]
    (render-tile batch tile)))
