(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file])
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format Color]
   [com.badlogic.gdx.graphics.g2d TextureAtlas TextureAtlas$AtlasRegion PixmapPackerIO PixmapPackerIO$SaveParameters PixmapPacker PixmapPacker$GuillotineStrategy]
   [java.io FileInputStream]))


(defn parse-def-from-lod
  [lod-def-info in]
  (try
    (let [{name :name} lod-def-info
          def-stream (h3m-parser/get-def-stream-from-lod lod-def-info in)
          _ (println "parse" name)
          def-info (h3m-parser/parse-def def-stream)
        ; TODO fix legacy check
          legacy? false ; (def-file/legacy? def-info def-stream uncompressed-size)
          ]
      (assoc
       def-info
       :legacy? legacy?
       :name name))
    (catch AssertionError e
      (println lod-def-info)
      (println "catch" e)
      (throw (new Exception "fail")))))


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
      ;  (filter #(clojure.string/ends-with? (:name %) "ADVMWIND.def"))
       (map #(update % :name clojure.string/lower-case))
       (filter #(clojure.string/ends-with? (:name %) "ava0013.def"))
       (filter #(clojure.string/ends-with? (:name %) ".def"))
      ;  (drop 40)
       (map #(do
               (println (:name %))
               (parse-def-from-lod % lod-in)))
      ;  (filter #(clojure.string/starts-with? (:name %) "advmwind"))
       (remove #(:legacy? %))
       (map map-def)
       (take 50)
       ))


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
       (map #(Color/rgba4444
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
        _ (println "image")
        _ (println full-width full-height)
        image (doto (new Pixmap
                         (int full-width)
                         (int full-height)
                         Pixmap$Format/RGBA4444)
                (.setColor 0 0 0 0)
                (.fill))
        _ (println "pixels")
        pixels (map #(nth palette %) data)]
    (dorun
     (for [x (range 0 width)
           y (range 0 height)
           :let [palette-index (nth data (+ (* width y) x))
                 color (nth palette palette-index)]]
       (try
         (.drawPixel
          image
          (+ x (:x frame))
          (+ y (:y frame))
          color)
         (catch Exception e
           (println "fail")
           (println color)
           (println (+ (* width y) x))
           (println (count pixels))
           (println "pixel" (nth pixels (+ (* width y) x)))
           (println e)
           (throw e)))))
    image))


(defn find-index [fn coll]
  (->> coll
       (map-indexed vector)
       (filter #(fn (second %)))
       (take 1)
       (map first)))


(comment
  (let [packer (new PixmapPacker 4096 4096 Pixmap$Format/RGBA4444 0 false (new PixmapPacker$GuillotineStrategy))
        in (new FileInputStream (.file (.internal Gdx/files "Data/H3sprite.lod")))
        defs (read-lod in (:map def-file/def-type))]
    (dorun
     (for [{name :name
            palette :palette
            frames :frames
            order :order} defs
           frame frames]
       (let [region-name (format "%s__%d" name (:offset frame))
             _ (println "pixmap" region-name)
             pixmap (frame->pixmap (make-palette palette) frame)
             _ (println "after" region-name)]
         (println "pack" region-name)
         (.pack packer region-name pixmap)
         (.dispose ^Pixmap pixmap)
         (println "assoc" region-name)
         (assoc frame :pixmap pixmap))))
    (doto (new PixmapPackerIO)
      (.save (.local Gdx/files "test2.atlas") packer))))