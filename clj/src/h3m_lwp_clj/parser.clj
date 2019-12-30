(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file])
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Pixmap Pixmap$Format Color]
   [com.badlogic.gdx.graphics.g2d PixmapPackerIO PixmapPacker PixmapPacker$GuillotineStrategy]
   [java.io FileInputStream]))


(defn parse-def-from-lod
  [lod-def-info in]
  (let [{name :name} lod-def-info
        def-stream (h3m-parser/get-def-stream-from-lod lod-def-info in)
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
      (update :frames #(map map-frame %))
      (assoc :order (get-in def-info [:groups 0 :offsets]))))


(defn read-lod [^FileInputStream lod-in item-type]
  (->> (h3m-parser/parse-lod lod-in)
       :files
       (filter #(= (:type %) item-type))
       (filter #(clojure.string/ends-with? (:name %) "AvWAngl.def"))
       (filter #(clojure.string/ends-with? (:name %) ".def"))
       (map #(parse-def-from-lod % lod-in))
       (remove #(:legacy? %))
       (map map-def)
       (take 1)))


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


(defn frame->pixmap [palette frame]
  (let [{width :width
         height :height
         full-width :full-width
         full-height :full-height
         data :data} frame
        image (doto (new Pixmap (int full-width) (int full-height) Pixmap$Format/RGBA4444)
                (.setColor 0 0 0 0)
                (.fill))
        pixels (map #(nth palette %) data)]
    (dorun
     (for [x (range 0 width)
           y (range 0 height)]
       (.drawPixel
        image
        (+ x (:x frame))
        (+ y (:y frame))
        (nth pixels (+ (* full-width y) x)))))
    image))


(defn find-index [fn coll]
  (->> coll
       (map-indexed vector)
       (filter #(fn (second %)))
       (take 1)
       (map first)))


(comment
  (let [packer (new PixmapPacker 2048 2048 Pixmap$Format/RGBA4444 0 false (new PixmapPacker$GuillotineStrategy))
        in (new FileInputStream (.file (.internal Gdx/files "Data/H3sprite.lod")))]
    (doall
     (for [def-info (read-lod in (:map def-file/def-type))]
       (let [{name :name
              palette :palette
              frames :frames
              order :order} def-info
             pixmap-index (mapcat
                           (fn [offset] (find-index #(= (:offset %) offset) frames))
                           order)
             pixmaps (map
                      #(frame->pixmap (make-palette palette) %)
                      frames)]
         (dorun
          (for [i (range 0 (count pixmap-index))
                :let [index (nth pixmap-index i)]]
            (do
              (println (format "%s_%d" name i))
              (.pack
               packer
               (format "%s_%d" name i)
               (nth pixmaps index)))))
         def-info)))
    (doto (new PixmapPackerIO)
      (.save (.local Gdx/files "test2.atlas") packer))))
