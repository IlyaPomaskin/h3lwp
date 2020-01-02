(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file])
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Pixmap Pixmap$Format Color]
   [com.badlogic.gdx.graphics.g2d PixmapPackerIO PixmapPacker]
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


(defn read-lod [^FileInputStream lod-in item-type]
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


(comment
  (let [packer (new PixmapPacker 4096 4096 Pixmap$Format/RGBA8888 0 false)
        in (new FileInputStream (.file (.internal Gdx/files "data/H3sprite.lod")))
        defs (read-lod in (:map def-file/def-type))]
    (time
     (dorun
      (pmap
       (fn [{name :name
             palette :palette
             frames :frames
             order :order}]
         (dorun
          (pmap
           (fn [frame]
             (let [region-name (format "%s__%d" name (:offset frame))
                   pixmap ^Pixmap (frame->pixmap (make-palette palette) frame)]
               (.pack packer region-name pixmap)
               (.dispose pixmap)))
           frames)))
       defs)))
    (doto (new PixmapPackerIO)
      (.save (.local Gdx/files "test1.atlas") packer))))