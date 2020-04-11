(ns h3m-lwp-clj.terrain
  (:import
   [com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format OrthographicCamera]
   [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch TextureAtlas$AtlasRegion]
   [com.badlogic.gdx.maps.tiled TiledMap TiledMapTileLayer TiledMapTileLayer$Cell]
   [com.badlogic.gdx.maps.tiled.tiles AnimatedTiledMapTile StaticTiledMapTile]
   [com.badlogic.gdx.maps.tiled.renderers OrthogonalTiledMapRenderer]
   [com.badlogic.gdx.utils Array])
  (:require
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.utils :as utils]))


(defn frames->animated-tile
  [^Array frames]
  (new
   AnimatedTiledMapTile
   (float consts/animation-interval)
   (utils/map-libgdx-array
    (fn [^TextureAtlas$AtlasRegion frame]
      (doto (new StaticTiledMapTile frame)
        (.setOffsetY (.-offsetY frame))
        (.setOffsetX (.-offsetX frame))))
    frames)))


(defn create-tile [tile type]
  (let [mirror-config (get tile :mirror-config)
        flip-x (bit-test mirror-config (get-in consts/tile-types [type :flip-x]))
        flip-y (bit-test mirror-config (get-in consts/tile-types [type :flip-y]))]
    (doto (new TiledMapTileLayer$Cell)
      (.setTile
       (frames->animated-tile
        (assets/get-terrain-sprite
         (get-in consts/tile-types [type :names (get tile type)])
         (get tile (get-in consts/tile-types [type :index])))))
      (.setFlipHorizontally flip-x)
      (.setFlipVertically flip-y))))


(defn create-debug-layer
  [map-size]
  (let [debug-layer (new TiledMapTileLayer map-size map-size consts/tile-size consts/tile-size)
        debug-tile (->>
                    (doto (new Pixmap 32 32 Pixmap$Format/RGBA4444)
                      (.setColor (float 1) (float 0) (float 0) (float 0.5))
                      (.drawRectangle 0 0 32 32))
                    (new Texture)
                    (new TextureRegion)
                    (new StaticTiledMapTile))
        debug-cell (doto (new TiledMapTileLayer$Cell)
                     (.setTile debug-tile))]
    (dorun
     (for [x (range 0 map-size)
           y (range 0 map-size)]
       (.setCell debug-layer x y debug-cell)))
    debug-layer))


(defn create-layers
  [map-size terrain-tiles]
  (let [terrain-layer (new TiledMapTileLayer map-size map-size consts/tile-size consts/tile-size)
        river-layer (new TiledMapTileLayer map-size map-size consts/tile-size consts/tile-size)
        road-layer (new TiledMapTileLayer map-size map-size consts/tile-size consts/tile-size)]
    (.setOffsetY road-layer (- (/ consts/tile-size 2)))
    (dorun
     (for [x (range 0 map-size)
           y (range 0 map-size)
           :let [index (+ (* map-size y) x)
                 tile (nth terrain-tiles index)]]
       (do
         (.setCell terrain-layer x y (create-tile tile :terrain))
         (when (pos? (:river tile))
           (.setCell river-layer x y (create-tile tile :river)))
         (when (pos? (:road tile))
           (.setCell road-layer x y (create-tile tile :road))))))
    [terrain-layer
     river-layer
     road-layer]))


(defn create-tiled-map
  ^TiledMap
  [layers]
  (let [tiled-map (new TiledMap)]
    (dorun
     (for [layer layers]
       (.add (.getLayers tiled-map) layer)))
    tiled-map))


(defn create-renderer
  [^SpriteBatch batch ^OrthographicCamera camera h3m-map]
  (let [layers (create-layers (:size h3m-map) (:terrain h3m-map))
        tiled-map (create-tiled-map layers)
        renderer (new OrthogonalTiledMapRenderer tiled-map batch)]
    (fn []
      (doto renderer
        (.setView camera)
        (.render)))))