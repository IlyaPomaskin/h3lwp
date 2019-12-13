(ns h3m-lwp-clj.assets
  (:import [com.badlogic.gdx.graphics]
           [com.badlogic.gdx.graphics.g2d TextureAtlas]
           [com.badlogic.gdx.assets AssetManager]
           [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
           [com.badlogic.gdx.utils Array]))


(def ^String terrains-atlas "sprites/terrains.atlas")
(def ^String object-atlas "sprites/mapObjects.atlas")


(def ^AssetManager manager
  (doto (new AssetManager)
    (.load terrains-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))
    (.load object-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))))


(defn create-atlas-getter
  [^String atlas-name]
  (fn [^String region-name]
    ^Array
    (let [^TextureAtlas atlas (.get manager atlas-name)
          ^Array frames (.findRegions atlas region-name)]
      (when (zero? (.size frames))
        (throw (new Exception (format "Region %s not found in atlas %s %s" region-name atlas-name))))
      frames)))


(def get-object (create-atlas-getter object-atlas))
(def get-terrain (create-atlas-getter terrains-atlas))
