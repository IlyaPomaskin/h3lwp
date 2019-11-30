(ns h3m-lwp-clj.assets
  (:import [com.badlogic.gdx.graphics]
           [com.badlogic.gdx.graphics.g2d TextureAtlas TextureRegion]
           [com.badlogic.gdx.assets AssetManager]
           [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
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
        ^Array frames (.findRegions atlas (format "%s/%02d" name index))]
    (if (zero? (.size frames))
      (do
        (println "Terrain not found:" name index)
        (get-object "empty"))
      frames)))


(defn create-sprite
  [^Array frames random-start?]
  (let [frames-count (.size frames)
        initial-frame (if random-start?
                        (rand-int frames-count)
                        0)
        current-frame (volatile! initial-frame)]
    (fn []
      (when (> frames-count 1)
        (vswap! current-frame #(if (= %1 (dec frames-count))
                                 0
                                 (inc %1))))
      (.get frames @current-frame))))