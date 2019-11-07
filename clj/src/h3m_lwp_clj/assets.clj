(ns h3m-lwp-clj.assets
  (:import [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.graphics.g2d TextureAtlas Animation]
           [com.badlogic.gdx.assets AssetManager]
           [com.badlogic.gdx.assets.loaders TextureAtlasLoader TextureAtlasLoader$TextureAtlasParameter]))


(def terrains-atlas "sprites/terrains.atlas")
(def object-atlas "sprites/mapObjects.atlas")


(def manager
  (doto (new AssetManager)
    (.load terrains-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))
    (.load object-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))
    (Texture/setAssetManager)))


(defn get-object-
  [name]
  (let [frames (-> manager
                   (.get object-atlas)
                   (.findRegions name))]
    (if (zero? (.size frames))
      (do
        (println "Sprite not found:" name)
        (get-object- "empty"))
      frames)))


(def get-object (memoize get-object-))


(defn get-terrain-
  [name index]
  (-> manager
      (.get terrains-atlas)
      (.findRegions name)
      (.get index)))


(def get-terrain (memoize get-terrain-))