(ns h3m-lwp-clj.assets
  (:import [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.graphics.g2d TextureRegion TextureAtlas TextureAtlas$AtlasRegion]
           [com.badlogic.gdx.assets AssetManager]
           [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
           [com.badlogic.gdx.utils Array]))


(def ^String objects-atlas "sprites/all.atlas")


(defonce objects-info (atom {}))


(def ^AssetManager manager
  (doto (new AssetManager)
    (.load objects-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))))


(defn init []
  (reset!
   objects-info
   (try
     ; TODO AssetLoader?
     (read-string (slurp "sprites/all.edn"))
     (catch Exception _
       (println "Failed to read all.edn")
       {})))
  (.finishLoading manager)
  (Texture/setAssetManager manager))


(defn get-object-frame [def-name offset]
  ^TextureRegion
  (let [^TextureAtlas atlas (.get manager objects-atlas)
        ^TextureAtlas$AtlasRegion frame (.findRegion atlas def-name offset)]
    (when (nil? frame)
      (throw (new Exception (format "def %s with offset %d not found in atlas" def-name offset))))
    frame))


(defn get-terrain-sprite [def-name index]
  ^Array
  (let [^TextureAtlas atlas (.get manager objects-atlas)
        region-name (format "%s/%02d" def-name index)
        ^Array frames (.findRegions atlas region-name)]
    (when (.isEmpty frames)
      (throw (new Exception (format "def %s with index %d not found in atlas" def-name index))))
    frames))


(defn get-sprite-info [def-name]
  (get @objects-info def-name))
