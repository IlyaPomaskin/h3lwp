(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file])
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.files FileHandle]
   [com.badlogic.gdx.graphics Pixmap Pixmap$Format Color]
   [com.badlogic.gdx.graphics.g2d PixmapPackerIO PixmapPackerIO$SaveParameters PixmapPacker]
   [java.io BufferedInputStream FileInputStream]
   [java.util.zip Inflater InflaterInputStream]))


(defn get-def-stream-from-lod
  [lod-def-info ^FileInputStream in]
  (let [{size :size
         compressed-size :compressed-size
         offset :offset} lod-def-info]
    (.position (.getChannel in) (long offset))
    (if (pos? compressed-size)
      (new InflaterInputStream in (new Inflater) compressed-size)
      (new BufferedInputStream in size))))


(defn parse-def-from-lod
  [lod-def-info in]
  (let [{name :name} lod-def-info
        def-stream (get-def-stream-from-lod lod-def-info in)
        def-info (h3m-parser/parse-def def-stream)
        ; TODO fix legacy check
        legacy? false ; (def-file/legacy? def-info def-stream uncompressed-size)
        ]
    (assoc
     def-info
     :legacy? legacy?
     :name name)))


(defn lines->bytes [frame]
  (let [{lines :lines
         offsets :offsets} frame]
    (->> offsets
         (mapcat (fn [offset] (mapcat :data (get lines offset))))
         (vec))))


(defn map-frame [frame]
  (-> frame
      (select-keys [:full-width
                    :full-height
                    :width
                    :height
                    :x
                    :y
                    :offset
                    :data])
      (update :data lines->bytes)))


(defn map-def [def-info]
  (-> def-info
      (select-keys [:name
                    :full-width
                    :full-height
                    :palette
                    :frames])
      (update :name clojure.string/replace #".def" "")
      (update :name clojure.string/lower-case)
      (update :frames #(map map-frame %))
      (assoc :order (get-in def-info [:groups 0 :offsets]))))


(defn read-lod [item-type ^FileInputStream lod-in]
  (->> (h3m-parser/parse-lod lod-in)
       :files
       (filter #(= (:type %) item-type))
       (pmap #(update % :name clojure.string/lower-case))
       (filter #(clojure.string/ends-with? (:name %) ".def"))
       (map #(parse-def-from-lod % lod-in))
       (remove #(:legacy? %))
       (pmap map-def)
       (vec)))


(defn make-palette [palette]
  (->> palette
       (map #(assoc % 3 0xff))
       (map-indexed
        (fn [index item]
          (case (int index)
            0 [0 0 0 0]
            1 [0 0 0 0x40]
            4 [0 0 0 0x80]
            5 [0 0 0 0]
            6 [0 0 0 0x80]
            7 [0 0 0 0x40]
            item)))
       (map #(Color/rgba8888
              (float (/ (% 0) 255))
              (float (/ (% 1) 255))
              (float (/ (% 2) 255))
              (float (/ (% 3) 255))))
       (vec)))


(defn frame->pixmap [palette frame]
  (let [{width :width
         height :height
         full-width :full-width
         full-height :full-height
         data :data} frame
        image (doto (new Pixmap
                         (int full-width)
                         (int full-height)
                         Pixmap$Format/RGBA8888)
                (.setColor 0 0 0 0)
                (.fill))]
    (dorun
     (for [x (range 0 width)
           y (range 0 height)
           :let [palette-index (nth data (+ (* width y) x))
                 color (nth palette palette-index)]]
       (.drawPixel
        image
        (+ x (:x frame))
        (+ y (:y frame))
        color)))
    image))


(defn pack-defs [^PixmapPacker packer defs]
  (dorun
   (for [{name :name
          palette :palette
          frames :frames} defs
         frame frames
         :let [region-name (format "%s_%d" name (:offset frame))
               pixmap ^Pixmap (frame->pixmap (make-palette palette) frame)]]
     (do
       (.pack packer region-name pixmap)
       (.dispose pixmap)))))


(defn save-packer [^PixmapPacker packer ^FileHandle out-file]
  (let [save-parameters (new PixmapPackerIO$SaveParameters)]
    (set! (.-useIndexes save-parameters) true)
    (doto (new PixmapPackerIO)
      (.save out-file packer save-parameters))))


(defn save-defs-info [file-name defs]
  (->> defs
       (mapcat #(vector (:name %) (dissoc % :palette :frames)))
       (apply hash-map)
       (pr-str)
       (spit file-name)))


(defn parse-objects [^FileHandle lod-file ^FileHandle out-file defs-info-file-name]
  (let [packer (new PixmapPacker 4096 4096 Pixmap$Format/RGBA8888 0 false)
        defs (read-lod (:map def-file/def-type) (new FileInputStream (.file lod-file)))]
    (pack-defs packer defs)
    (save-defs-info defs-info-file-name defs)
    (save-packer packer out-file)
    (.dispose packer)))


(comment
  (parse-objects
   (.internal Gdx/files "data/H3sprite.lod")
   (.local Gdx/files "sprites/objects.atlas")
   "sprites/objects.edn"))