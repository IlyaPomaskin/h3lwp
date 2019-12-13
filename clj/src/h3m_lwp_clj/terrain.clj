(ns h3m-lwp-clj.terrain
  (:import [com.badlogic.gdx.maps.tiled TiledMap TiledMapTileLayer TiledMapTileLayer$Cell]
           [com.badlogic.gdx.maps.tiled.tiles AnimatedTiledMapTile StaticTiledMapTile]
           [com.badlogic.gdx.maps.tiled.renderers OrthogonalTiledMapRenderer]
           [com.badlogic.gdx.graphics.g2d TextureRegion]
           [com.badlogic.gdx.utils Array])
  (:require [h3m-lwp-clj.assets :as assets]
            [h3m-lwp-clj.consts :as consts]
            [h3m-lwp-clj.utils :as utils]))


(defn tile->filename
  [tile type]
  (let
   [tile-def-name (get-in consts/tile-types [type :names (get tile type)])
    def-image-index (get tile (get-in consts/tile-types [type :index]))]
    (format "%s/%02d" tile-def-name def-image-index)))


(defn frames->animated-tile
  [^Array frames]
  (new
   AnimatedTiledMapTile
   (float consts/animation-interval)
   (utils/map-libgdx-array
    (fn [frame] (new StaticTiledMapTile ^TextureRegion frame))
    frames)))


(defn create-tile [tile type]
  (let [mirror-config (get tile :mirror-config)
        flip-x (bit-test mirror-config (get-in consts/tile-types [type :flip-x]))
        flip-y (bit-test mirror-config (get-in consts/tile-types [type :flip-y]))]
    (doto (new TiledMapTileLayer$Cell)
      (.setTile
       (frames->animated-tile
        (assets/get-terrain
         (tile->filename tile type))))
      (.setFlipHorizontally flip-x)
      (.setFlipVertically flip-y))))


(defn create-layers
  [{size :size
    terrain :terrain}]
  (let [terrain-layer (new TiledMapTileLayer size size consts/tile-size consts/tile-size)
        road-layer (new TiledMapTileLayer size size consts/tile-size consts/tile-size)
        river-layer (new TiledMapTileLayer size size consts/tile-size consts/tile-size)]
    (dorun
     (for [x (range 0 size)
           y (range 0 size)
           :let [index (+ (* size y) x)
                 tile (nth terrain index)]]
       (do
         (.setCell terrain-layer x y (create-tile tile :terrain))
         (when (pos? (:road tile))
           (.setCell road-layer x y (create-tile tile :road)))
         (when (pos? (:river tile))
           (.setCell river-layer x y (create-tile tile :river))))))
    [terrain-layer
     road-layer
     river-layer]))


(defn create-tiled-map
  [layers]
  (let [tiled-map (new TiledMap)]
    (dorun
     (for [layer layers]
       (.add (.getLayers tiled-map) layer)))
    tiled-map))


(defn create-renderer
  [h3m-map]
  (new
   OrthogonalTiledMapRenderer
   (create-tiled-map (create-layers h3m-map))))
