(ns h3m-lwp-clj.assets
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format]
   [com.badlogic.gdx.graphics.g2d TextureRegion TextureAtlas TextureAtlas$AtlasRegion]
   [com.badlogic.gdx.assets AssetManager]
   [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
   [com.badlogic.gdx.utils Array])
  (:require
   [h3m-lwp-clj.consts :as consts]))


(defonce objects-info (atom {}))


(def ^AssetManager manager
  (doto (new AssetManager)
    (.load
      ^String consts/atlas-file-name
      TextureAtlas
      (new TextureAtlasLoader$TextureAtlasParameter true))))


(defn init []
  (reset!
   objects-info
;   (try
     ; TODO AssetLoader?
   (->> consts/edn-file-name
        (.internal Gdx/files)
        (.read)
        (slurp)
        (read-string)))
;     (catch Exception _
;       (println "Failed to read all.edn")
;       {})))
  (.finishLoading manager)
  (Texture/setAssetManager manager))


(defn get-sprite-info [def-name]
  (get @objects-info def-name))


(defn get-empty-texture-region-
  []
  (let [pixmap (doto (new Pixmap 1 1 Pixmap$Format/RGBA8888)
                 (.setColor 0 0 0 0)
                 (.fill))
        texture (new Texture pixmap)
        region (new TextureRegion texture)]
    region))
(def get-empty-texture-region (memoize get-empty-texture-region-))


(defn get-object-frame [def-name offset]
  ^TextureRegion
  (let [^TextureAtlas atlas (.get manager consts/atlas-file-name)
        ^TextureAtlas$AtlasRegion frame (.findRegion atlas def-name offset)]
    (if (nil? frame)
      (do
        (format "def %s with offset %d not found in atlas" def-name offset)
        (get-empty-texture-region))
      frame)))


(defn get-empty-terrain-sprite-
  []
  (doto (new Array 1)
    (.add (get-empty-texture-region))))
(def get-empty-terrain-sprite (memoize get-empty-terrain-sprite-))


(defn load-terrain-sprite [def-name index]
  (let [^TextureAtlas atlas (.get manager consts/atlas-file-name)
        sprite-info (get-sprite-info def-name)
        {frames-order :order} sprite-info
        offset (nth frames-order index)
        region-name (format "%s/%d" def-name offset)
        ^Array frames (.findRegions atlas region-name)]
    (if (.isEmpty frames)
      (do
        (println (format "def %s with index %d not found in atlas" def-name index))
        (get-empty-terrain-sprite))
      frames)))


(defn get-terrain-sprite [def-name index]
  ^Array
  (if (true? (.isLoaded manager consts/atlas-file-name))
    (load-terrain-sprite def-name index)
    (get-empty-terrain-sprite)))