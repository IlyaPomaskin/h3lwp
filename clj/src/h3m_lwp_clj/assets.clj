(ns h3m-lwp-clj.assets
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Texture]
   [com.badlogic.gdx.graphics.g2d TextureAtlas]
   [com.badlogic.gdx.assets AssetManager]
   [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
   [com.badlogic.gdx.utils Array])
  (:require
   [h3m-lwp-clj.consts :as consts]))


(def asset-manager (atom nil))


(defn file-exists?
  [file-path]
  (->> file-path
       (.local Gdx/files)
       (.exists)))


(defn assets-ready?
  []
  (and
   (file-exists? consts/atlas-file-name)
   (file-exists? consts/png-file-name)))


(defn load-atlas
  [^AssetManager asset-manager]
  (doto asset-manager
    (.load
     consts/atlas-file-name
     TextureAtlas
     (new TextureAtlasLoader$TextureAtlasParameter true))
    (.finishLoadingAsset consts/atlas-file-name)
    (.finishLoading)))


(defn validate-atlas
  [^AssetManager asset-manager]
  (let [^TextureAtlas atlas (.get asset-manager consts/atlas-file-name)
        regions (.getRegions atlas)
        assets-count (.-size regions)
        ; TODO check by defs used in map
        enough-assets? (>= assets-count consts/minimal-lod-objects-count)]
    (if (not enough-assets?)
      (do
        (.dispose asset-manager)
        nil)
      asset-manager)))


(defn try-load-atlas
  []
  (when (assets-ready?)
    (let [manager (some->
                   (new AssetManager)
                   (load-atlas)
                   (validate-atlas))]
      (when manager
        (Texture/setAssetManager manager)
        (reset! asset-manager manager)
        true))))


(defn get-def-frames
  ^Array
  [def-name]
  (let [^TextureAtlas atlas (.get ^AssetManager @asset-manager consts/atlas-file-name)
        ^Array frames (.findRegions atlas def-name)]
    (when (.isEmpty frames)
      (println (format "def %s not found in atlas" def-name)))
    frames))


(defn get-terrain-sprite
  ^Array
  [def-name index]
  (let [frames (get-def-frames def-name)]
    (if (.isEmpty frames)
      frames
      (doto (new Array 1)
        (.add (.get frames index))))))