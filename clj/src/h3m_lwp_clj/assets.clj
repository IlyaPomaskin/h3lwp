(ns h3m-lwp-clj.assets
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format]
   [com.badlogic.gdx.graphics.g2d TextureRegion TextureAtlas TextureAtlas$AtlasRegion]
   [com.badlogic.gdx.assets AssetManager]
   [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
   [com.badlogic.gdx.utils Array])
  (:require
   [h3m-lwp-clj.consts :as consts]
   [clojure.string :as string]))


(defonce objects-info (atom {}))


(def ^AssetManager asset-manager
  (new AssetManager))


(defn file-exists?
  [file-path]
  (->> file-path
       (.local Gdx/files)
       (.exists)))


(defn load-objects-info
  []
  (reset!
   objects-info
   (->> consts/edn-file-name
        (.internal Gdx/files)
        (.read)
        (slurp)
        (read-string))))


(defn init
  []
  (Texture/setAssetManager asset-manager)
  (when (file-exists? consts/edn-file-name)
    (load-objects-info))
  (when (file-exists? consts/atlas-file-name)
    (doto asset-manager
      (.load
       consts/atlas-file-name
       TextureAtlas
       (new TextureAtlasLoader$TextureAtlasParameter true))
      (.finishLoadingAsset consts/atlas-file-name)
      (.finishLoading))))


(defn get-sprite-info
  [def-name]
  (let [sprite-info (get @objects-info def-name)]
    (when (nil? sprite-info)
      (println (format "Sprite info for %s not found" def-name)))
    sprite-info))


(defn get-empty-texture-region-
  []
  (let [pixmap (doto (new Pixmap 1 1 Pixmap$Format/RGBA8888)
                 (.setColor 0 0 0 0)
                 (.fill))
        texture (new Texture pixmap)
        region (new TextureRegion texture)]
    region))
(def get-empty-texture-region (memoize get-empty-texture-region-))


(defn get-object-frame
  [def-name offset]
  ^TextureRegion
  (let [^TextureAtlas atlas (.get asset-manager consts/atlas-file-name)
        ^TextureAtlas$AtlasRegion frame (.findRegion atlas def-name offset)]
    (if (nil? frame)
      (do
        (println (format "def %s with offset %d not found in atlas" def-name offset))
        (get-empty-texture-region))
      frame)))


(defn get-map-object-frames
  [filename]
  (let [{frames-order :order
         full-width :full-width
         full-height :full-height
         :or {frames-order []}} (get-sprite-info filename)
        frames (mapv #(get-object-frame filename %) frames-order)]
    {:full-width full-width
     :full-height full-height
     :frames frames}))


(defn get-empty-terrain-sprite-
  []
  (doto (new Array 1)
    (.add (get-empty-texture-region))))
(def get-empty-terrain-sprite (memoize get-empty-terrain-sprite-))


(defn load-terrain-sprite
  [def-name index]
  (let [sprite-info (get-sprite-info def-name)
        {frames-order :order} sprite-info
        offset (nth frames-order index)
        region-name (format "%s/%d" def-name offset)
        ^TextureAtlas atlas (.get asset-manager consts/atlas-file-name)
        ^Array frames (.findRegions atlas region-name)]
    (if (.isEmpty frames)
      (do
        (println (format "def %s with index %d not found in atlas" def-name index))
        (get-empty-terrain-sprite))
      frames)))


(defn get-terrain-sprite
  [def-name index]
  ^Array
  (if (true? (.isLoaded asset-manager consts/atlas-file-name))
    (load-terrain-sprite def-name index)
    (get-empty-terrain-sprite)))


(defn get-objects-info-count
  []
  (count @objects-info))


(defn get-uniq-atlas-regions-count
  []
  (let [atlas ^TextureAtlas (.get asset-manager consts/atlas-file-name)
        regions ^Array (.getRegions atlas)
        regions-names (map
                       #(string/replace
                         (.-name ^TextureAtlas$AtlasRegion %1)
                         #"\/\d+"
                         "")
                       regions)
        uniq-regions (set regions-names)]
    (count uniq-regions)))


(defn assets-ready?
  []
  (and
   (file-exists? consts/edn-file-name)
   (file-exists? consts/atlas-file-name)
   (true? (.isLoaded asset-manager consts/atlas-file-name))
   (> (get-objects-info-count)
      consts/minimal-lod-objects-count)
   (= (get-objects-info-count)
      (get-uniq-atlas-regions-count))))