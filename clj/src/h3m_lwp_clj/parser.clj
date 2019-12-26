(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m])
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Texture$TextureFilter Pixmap Pixmap$Format PixmapIO Color]
   [com.badlogic.gdx.graphics.g2d TextureAtlas PixmapPackerIO PixmapPackerIO$ImageFormat PixmapPackerIO$SaveParameters PixmapPacker PixmapPacker$Page PixmapPacker$SkylineStrategy PixmapPacker$GuillotineStrategy]
   [com.badlogic.gdx.assets AssetManager]
   [com.badlogic.gdx.assets.loaders TextureAtlasLoader$TextureAtlasParameter]
   [com.badlogic.gdx.utils Array]
   [java.io RandomAccessFile]))


(defn get-frame-pixels [frame]
  (->> (:segments frame)
       (flatten)
       (map #(:data %))
       (apply concat)))


(defn get-pixels-from-palette [palette pixels]
  (map #(nth palette %) pixels))


(defn make-palette [palette]
  (->> palette
       (map-indexed
        (fn [index item]
          (case (int index)
            0 [0 0 0 0]
            1 [0 0 0 0x40]
            4 [0 0 0 0x80]
            5 [0 0 0 0]
            6 [0 0 0 0x80]
            7 [0 0 0 0x40]
            (assoc item 3 0xff))))
       (map #(Color/rgba8888
              (float (/ (% 0) 255))
              (float (/ (% 1) 255))
              (float (/ (% 2) 255))
              (float (/ (% 3) 255))))))


(defn read-lod []
  (let [lod-file (.internal Gdx/files "Data/H3sprite.lod")
        lod-info (h3m/parse-lod (.read lod-file))]
    lod-info))


(defn read-def
  [^PixmapPacker packer
   in
   ^RandomAccessFile raf]
  (let [def-file (.internal Gdx/files "Data/AvWAngl.def")
        def-path (.path def-file)
        def-info (h3m/parse-def (.read def-file))
        frame-count (get-in def-info [:groups 0 :frame-count])
        {palette :palette
         ^Integer full-width :full-width
         ^Integer full-height :full-height} def-info
        used-names (atom #{})]
    (dorun
     (for [i (range 0 frame-count)
           :let [name (get-in def-info [:groups 0 :names i])
                 frame (h3m/parse-def-frame (.path def-file) 0 i)
                 pixels (->> frame
                             (get-frame-pixels)
                             (get-pixels-from-palette (make-palette palette)))
                 image (doto (new Pixmap full-width full-height Pixmap$Format/RGBA4444)
                         (.setColor 0 0 0 0)
                         (.fill))]]
       (do
         (swap! used-names conj name)
         (dorun
          (for [x (range 0 (:width frame))
                y (range 0 (:height frame))]
            (.drawPixel
             image
             (+ x (:x frame))
             (+ y (:y frame))
             (nth pixels (+ (* full-width y) x)))))
         (.pack packer (format "%s_%d" name i) image))))
    (println @used-names)
         ;
    ))


(let [packer (new
              PixmapPacker
              256
              256
              Pixmap$Format/RGBA4444
              0
              false
              (new PixmapPacker$GuillotineStrategy))
      in (.read (.internal Gdx/files "Data/H3sprite.lod"))
      lod-info (read)
      ]

  (read-def packer)
  (doto (new PixmapPackerIO)
    (.save (.local Gdx/files "test1.atlas") packer)))
