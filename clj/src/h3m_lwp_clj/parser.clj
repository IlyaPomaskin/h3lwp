(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file])
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Pixmap Pixmap$Format Color]
   [com.badlogic.gdx.graphics.g2d PixmapPackerIO PixmapPacker PixmapPacker$GuillotineStrategy]
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
          def-info (doall (h3m-parser/parse-def def-stream))
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
    (mapcat
      (fn [offset] (mapcat :data (get lines offset)))
      offsets)))


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
       (map #(update % :name clojure.string/lower-case))
       (filter #(clojure.string/ends-with? (:name %) ".def"))
       (remove #(clojure.string/ends-with? (:name %) "arrow.def"))
       (take 100)
       (map #(parse-def-from-lod % lod-in))
       (remove #(:legacy? %))
       (map map-def)))


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
              (float (/ (% 3) 255))))))


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


(defn find-index [fn coll]
  (->> coll
       (map-indexed vector)
       (filter #(fn (second %)))
       (take 1)
       (map first)))


(comment
  (let [packer (new PixmapPacker 1024 1024 Pixmap$Format/RGBA8888 0 false (new PixmapPacker$GuillotineStrategy))
        in (new FileInputStream (.file (.internal Gdx/files "Data/H3sprite.lod")))
        defs (time (read-lod in (:map def-file/def-type)))]
    (time
     (dorun
      (for [{name :name
             palette :palette
             frames :frames
             order :order} defs
            frame frames]
        (let [region-name (format "%s__%d" name (:offset frame))
              pixmap ^Pixmap (frame->pixmap (make-palette palette) frame)]
          ; (println "pack" region-name)
          (time (.pack packer region-name pixmap))
          (.dispose pixmap)))))
    (doto (new PixmapPackerIO)
      (.save (.local Gdx/files "test4.atlas") packer))))