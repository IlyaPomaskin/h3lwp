(ns h3m-lwp-clj.assets
  (:import [com.badlogic.gdx.graphics Texture]
           [com.badlogic.gdx.graphics.g2d TextureAtlas TextureAtlas$AtlasRegion]
           [com.badlogic.gdx.assets AssetManager]
           [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]))


(def ^String objects-atlas "sprites/objects.atlas")


(defonce objects-info (atom {}))


(def ^AssetManager manager
  (doto (new AssetManager)
    (.load objects-atlas TextureAtlas (new TextureAtlasLoader$TextureAtlasParameter true))))


(defn init []
  (reset!
   objects-info
   (try
     ; TODO AssetLoader?
     (read-string (slurp "sprites/objects.edn"))
     (catch Exception _
       (println "Failed to read objects.edn")
       {})))
  (.finishLoading manager)
  (Texture/setAssetManager manager))


(defn get-object-frame [def-name offset]
  (let [^TextureAtlas atlas (.get manager objects-atlas)
        ^TextureAtlas$AtlasRegion frame (.findRegion atlas def-name offset)]
    (when (nil? frame)
      (throw (new Exception (format "def %s with offset %d not found in atlas" def-name offset))))
    frame))


(defn get-sprite-info [def-name]
  (get @objects-info def-name))
