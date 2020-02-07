(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file]
   [h3m-lwp-clj.utils :as utils]
   [clojure.string :as string])
  (:import
   [com.badlogic.gdx.files FileHandle]
   [com.badlogic.gdx.graphics Pixmap Pixmap$Format Color]
   [com.badlogic.gdx.graphics.g2d PixmapPackerIO PixmapPackerIO$SaveParameters PixmapPacker]
   [java.io BufferedInputStream FileInputStream]
   [java.util.zip Inflater InflaterInputStream]))


(def rotations
  {"clrrvr" [[183 195] [195 201]]
   "mudrvr" [[183 189] [240 246]]
   "watrtl" [[229 241] [242 254]]
   "lavatl" [[246 254]]
   "lavrvr" [[240 248]]})
(def def-map (:map def-file/def-type))
(def def-terrain (:terrain def-file/def-type))


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
      (update :name string/lower-case)
      (update :name string/replace #".def" "")
      (update :frames #(doall (pmap map-frame %)))
      (assoc :order (get-in def-info [:groups 0 :offsets]))))


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
        def-info (try
                   (h3m-parser/parse-def def-stream)
                   (catch java.io.EOFException _
                     (println "eof fail" name))
                   (catch AssertionError _
                     (println "failed parse" name)))
        ; TODO fix legacy check
        legacy? false ; (def-file/legacy? def-info def-stream uncompressed-size)
        ]
    (-> def-info
        (assoc :legacy? legacy? :name name)
        (map-def))))


(defn fill-pixmap [^Pixmap pixmap palette frame]
  (let [{width :width
         height :height
         frame-x :x
         frame-y :y
         data :data} frame]
    (dorun
     (for [x (range 0 width)
           y (range 0 height)
           :let [palette-index (nth data (+ (* width y) x))
                 color (nth palette palette-index)]]
       (.drawPixel pixmap (+ x frame-x) (+ y frame-y) color)))))


(defn pack-frame [^PixmapPacker packer palette frame region-name]
  (let [{full-width :full-width
         full-height :full-height} frame
        ^Pixmap pixmap (doto (new Pixmap full-width full-height Pixmap$Format/RGBA8888)
                         (.setColor 0 0 0 0)
                         (.fill))]
    (fill-pixmap pixmap palette frame)
    (.pack packer region-name pixmap)
    (.dispose pixmap)))


(defn pack-map-object
  [^PixmapPacker packer
   {name :name
    palette :palette
    frames :frames}]
  (dorun
   (pmap
    (fn [frame]
      (pack-frame packer palette frame (format "%s_%d" name (:offset frame))))
    frames)))


(defn pack-terrain
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


(defn pack-defs
  [defs packer callback]
  (dorun
   (for [def-info defs]
     (do
       (condp = (:type def-info)
         def-map (pack-map-object packer def-info)
         def-terrain (pack-terrain packer def-info))
       (callback def-info)))))


(defn save-packer [^PixmapPacker packer ^FileHandle out-file]
  (let [save-parameters (new PixmapPackerIO$SaveParameters)]
    (set! (.-useIndexes save-parameters) true)
    (.save (new PixmapPackerIO) out-file packer save-parameters)))


(defn save-defs-info [defs ^FileHandle info-file]
  (.writeString
   info-file
   (->> defs
        (mapcat #(vector (:name %) (dissoc % :palette :frames)))
        (apply hash-map)
        (pr-str))
   false))


(defn get-lod-files-list
  [^FileInputStream lod-file]
  (doall
   (->>
    (:files (h3m-parser/parse-lod lod-file))
    (filter #(utils/coll-includes? (:type %) [def-map def-terrain]))
    (filter #(re-matches #"(?i).*\.def" (:name %))))))


(defn parse-map-sprites
  [lod-files-list
   ^FileInputStream lod-file
   ^FileHandle atlas-file
   ^FileHandle info-file
   callback]
  (let [packer (new PixmapPacker 4096 4096 Pixmap$Format/RGBA8888 0 false)
        defs (map #(parse-def-from-lod % lod-file) lod-files-list)]
    (pack-defs defs packer callback)
    (save-defs-info defs info-file)
    (save-packer packer atlas-file)
    (.dispose packer)))