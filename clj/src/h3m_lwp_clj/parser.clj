(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file]
   [h3m-lwp-clj.utils :as utils])
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


(defn lines->bytes
  [{lines :lines
    offsets :offsets
    data :data}
   compression]
  (if (= compression 0)
    data
    (->> offsets
         (mapcat (fn [offset] (mapcat :data (get lines offset))))
         (vec))))


(defn make-palette [palette]
  (->> palette
       (pmap #(assoc % 3 0xff))
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
       (pmap #(Color/rgba8888
               (float (/ (% 0) 255))
               (float (/ (% 1) 255))
               (float (/ (% 2) 255))
               (float (/ (% 3) 255))))
       (vec)))


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
      (update :data lines->bytes (:compression frame))))


(defn map-def [def-info]
  (-> def-info
      (select-keys [:type
                    :name
                    :full-width
                    :full-height
                    :palette
                    :frames])
      (update :palette make-palette)
      (update :name clojure.string/lower-case)
      (update :name clojure.string/replace #".def" "")
      (update :frames #(doall (pmap map-frame %)))
      (assoc :order (get-in def-info [:groups 0 :offsets]))))


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


(defn pack-frame [packer palette frame region-name]
  (let [pixmap ^Pixmap (frame->pixmap palette frame)]
    (.pack packer region-name pixmap)
    (.dispose pixmap)))


(defn pack-def-without-rotation
  [^PixmapPacker packer
   {name :name
    palette :palette
    frames :frames}]
  (dorun
   (pmap
    (fn [frame]
      (pack-frame packer palette frame (format "%s_%d" name (:offset frame))))
    frames)))


(def rotations
  {"clrrvr" [[183 195] [195 201]]
   "mudrvr" [[183 189] [240 246]]
   "watrtl" [[229 241] [242 254]]
   "lavatl" [[246 254]]
   "lavrvr" [[240 248]]})


(defn pack-def-with-rotation
  [^PixmapPacker packer
   {name :name
    palette :palette
    frames :frames}]
  (let [rotations (get rotations name)
        rotations-count (some->>
                         rotations
                         (map (fn [[from to]] (- to from)))
                         (apply max))]
    (dorun
     (for [frame frames
           rotation-index (range (or rotations-count 1))]
       (pack-frame
        packer
        (reduce
         (fn [acc [from to]] (utils/rotate-items acc from to rotation-index))
         palette
         rotations)
        frame
        (format "%s/%d_%d" name (:offset frame) rotation-index))))))


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


(defn parse-defs
  [^FileHandle lod-file ^FileHandle out-file defs-info-file-name]
  (let [packer (new PixmapPacker 4096 4096 Pixmap$Format/RGBA8888 0 false)
        in (new FileInputStream (.file lod-file))
        def-map (:map def-file/def-type)
        def-terrain (:terrain def-file/def-type)]
    (dorun
     (->>
      (:files (h3m-parser/parse-lod in))
      (filter #(utils/coll-includes? (:type %) [def-map def-terrain]))
      (filter #(re-matches #"(?i).*\.def" (:name %)))
      (map #(parse-def-from-lod % in))
      (remove #(:legacy? %))
      (pmap map-def)
      (pmap
       #(do
          (condp = (:type %)
            def-map (pack-def-without-rotation packer %)
            def-terrain (pack-def-with-rotation packer %))
          %))
      (save-defs-info defs-info-file-name)))
    (save-packer packer out-file)
    (.dispose packer)))