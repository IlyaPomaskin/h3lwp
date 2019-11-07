(ns h3m-lwp-clj.terrain
  (:import [com.badlogic.gdx ApplicationAdapter Game Gdx Graphics Screen]
           [com.badlogic.gdx.graphics Texture Color GL20 OrthographicCamera]
           [com.badlogic.gdx.graphics.g2d BitmapFont SpriteBatch SpriteCache TextureRegion])
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
  (.add
   batch
   (create-terrain-texture-region
    (assets/get-terrain (tile->filename tile :terrain) (:terrain-image-index tile))
    (bit-test (:mirror-config tile) 0)
    (bit-test (:mirror-config tile) 1))
   (float (:x-position tile))
   (float (:y-position tile)))
  (when (pos? (:river tile))
    (.add
     batch
     (create-terrain-texture-region
      (assets/get-terrain (tile->filename tile :river) (:river-image-index tile))
      (bit-test (:mirror-config tile) 2)
      (bit-test (:mirror-config tile) 3))
     (float (:x-position tile))
     (float (:y-position tile))))
  (when (pos? (:road tile))
    (.add
     batch
     (create-terrain-texture-region
      (assets/get-terrain (tile->filename tile :road) (:road-image-index tile))
      (bit-test (:mirror-config tile) 4)
      (bit-test (:mirror-config tile) 5))
     (float (:x-position tile))
     (float (:y-position tile)))))


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
