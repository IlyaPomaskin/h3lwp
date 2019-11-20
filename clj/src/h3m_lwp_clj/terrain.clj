(ns h3m-lwp-clj.terrain
  (:import [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.graphics.g2d TextureRegion])
  (:require [h3m-lwp-clj.assets :as assets]
            [h3m-lwp-clj.rect :as rect]
            [h3m-lwp-clj.consts :as consts]))


(defn tile->filename
  [tile part]
  (get-in consts/terrain [part (get tile part)]))


(defn create-terrain-texture-region-
  [images-list flip-x flip-y]
  (doto (new TextureRegion images-list)
    (.flip flip-x flip-y)))


(def create-terrain-texture-region (memoize create-terrain-texture-region-))


(defn render-tile
  [batch tile]
  (let [{mirror-config :mirror-config
         x-position :x-position
         y-position :y-position
         river :river
         road :road
         terrain-image-index :terrain-image-index
         river-image-index :river-image-index
         road-image-index :road-image-index} tile]
    (.add
     batch
     (create-terrain-texture-region
      (assets/get-terrain (tile->filename tile :terrain) terrain-image-index)
      (bit-test mirror-config 0)
      (bit-test mirror-config 1))
     (float x-position)
     (float y-position))
    (when (pos? river)
      (.add
       batch
       (create-terrain-texture-region
        (assets/get-terrain (tile->filename tile :river) river-image-index)
        (bit-test mirror-config 2)
        (bit-test mirror-config 3))
       (float x-position)
       (float y-position)))
    (when (pos? road)
      (.add
       batch
       (create-terrain-texture-region
        (assets/get-terrain (tile->filename tile :road) road-image-index)
        (bit-test mirror-config 4)
        (bit-test mirror-config 5))
       (float x-position)
       (float y-position)))))


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
     (assoc tile
            :x-position (* (dec x) consts/tile-size)
            :y-position (* (dec y) consts/tile-size)))))


(defn render-terrain-tiles
  [batch terrain-tiles]
  (doseq [tile terrain-tiles]
    (render-tile batch tile)))
