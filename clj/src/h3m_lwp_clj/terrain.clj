(ns h3m-lwp-clj.terrain
  (:import [com.badlogic.gdx.maps.tiled TiledMap TiledMapTileLayer TiledMapTileLayer$Cell]
           [com.badlogic.gdx.maps.tiled.tiles AnimatedTiledMapTile StaticTiledMapTile]
           [com.badlogic.gdx.maps.tiled.renderers OrthogonalTiledMapRenderer]
           [com.badlogic.gdx.utils Array])
  (:require [h3m-lwp-clj.assets :as assets]
            [h3m-lwp-clj.consts :as consts]
            [h3m-lwp-clj.utils :as utils]))


(defn tile->filename
  [tile part]
  (get-in consts/terrain [part (get tile part)]))


(defn frames->animated-tile
  [^Array frames]
  (new
   AnimatedTiledMapTile
   (float 0.180)
   (utils/map-libgdx-array #(new StaticTiledMapTile %1) frames)))


(defn tile->settings
  [type]
  (case type
    :terrain {:index :terrain-image-index
              :flip-x 0
              :flip-y 1}
    :river {:index :river-image-index
            :flip-x 2
            :flip-y 3}
    :road {:index :road-image-index
           :flip-x 4
           :flip-y 5}
    nil))


(defn create-tile [tile type]
  (let [{mirror-config :mirror-config} tile
        {image-index-field :index
         flip-x-field :flip-x
         flip-y-field :flip-y} (tile->settings type)
        flip-x (bit-test mirror-config flip-x-field)
        flip-y (bit-test mirror-config flip-y-field)]
    (doto (new TiledMapTileLayer$Cell)
      (.setTile
       (frames->animated-tile
        (assets/get-terrain
         (tile->filename tile :terrain)
         (get tile image-index-field))))
      (.setFlipHorizontally flip-x)
      (.setFlipVertically flip-y))))


(defn create-layers
  [{size :size
    terrain :terrain}]
  (let [terrain-layer (new TiledMapTileLayer size size consts/tile-size consts/tile-size)
        road-layer (new TiledMapTileLayer size size consts/tile-size consts/tile-size)
        river-layer (new TiledMapTileLayer size size consts/tile-size consts/tile-size)]
    (doall
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
    (doall
     (for [layer layers]
       (.add (.getLayers tiled-map) layer)))
    tiled-map))


(defn create-renderer
  [h3m-map]
  (new
   OrthogonalTiledMapRenderer
   (create-tiled-map (create-layers h3m-map))))