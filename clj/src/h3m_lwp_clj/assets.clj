(ns h3m-lwp-clj.assets
  (:import [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.graphics.g2d TextureAtlas Animation TextureRegion]
           [com.badlogic.gdx.assets AssetManager]
           [com.badlogic.gdx.assets.loaders TextureAtlasLoader TextureAtlasLoader$TextureAtlasParameter]
           [com.badlogic.gdx.utils Array]))


(def ^String terrains-atlas "sprites/terrains.atlas")
(def ^String object-atlas "sprites/mapObjects.atlas")


(def ^AssetManager manager
  (doto (new AssetManager)
    (.load terrains-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))
    (.load object-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))))


(defn get-object
  ^Array
  [name]
  (let [^TextureAtlas atlas (.get manager object-atlas)
        ^Array frames (.findRegions atlas name)]
    (if (zero? (.size frames))
      (do
        (println "Sprite not found:" name)
        (get-object "empty"))
      frames)))


(defn get-terrain
  ^TextureRegion
  [name index]
  (let [^TextureAtlas atlas (.get manager terrains-atlas)
        ^Array regions (.findRegions atlas name)
        terrain (.get regions index)]
    terrain))
