(ns h3m-lwp-clj.parser
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-parser.def :as def-file]
   [h3m-lwp-clj.utils :as utils]
   [h3m-lwp-clj.random :as random]
   [clojure.string :as string])
  (:import
   [com.badlogic.gdx.files FileHandle]
   [com.badlogic.gdx.graphics Pixmap Pixmap$Format Color]
   [com.badlogic.gdx.graphics.g2d PixmapPackerIO PixmapPackerIO$SaveParameters PixmapPacker]
   [java.io BufferedInputStream FileInputStream InputStream]
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
  (let [set-default-alpha
        (mapv #(assoc % 3 0xff) palette)

        preset-palette
        (assoc
         set-default-alpha
         0 [0 0 0 0]
         1 [0 0 0 0x40]
         4 [0 0 0 0x80]
         5 [0x80 0x80 0x80 0xff]
         6 [0 0 0 0x80]
         7 [0 0 0 0x40])

        colored-palette
        (mapv
         #(Color/rgba8888
           (float (/ (% 0) 255))
           (float (/ (% 1) 255))
           (float (/ (% 2) 255))
           (float (/ (% 3) 255)))
         preset-palette)]
    colored-palette))


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
      (update :frames #(map map-frame %))
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
        def-info (h3m-parser/parse-def def-stream)
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
        ^Pixmap pixmap (doto (new Pixmap (int full-width) (int full-height) Pixmap$Format/RGBA8888)
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
  [packer defs items-count callback]
  (loop [item-index 0]
    (let [def-info (nth defs item-index)]
      (condp = (:type def-info)
        def-map (pack-map-object packer def-info)
        def-terrain (pack-terrain packer def-info))
      (callback items-count item-index)
      (if (= items-count item-index)
        nil
        (recur (inc item-index))))))


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
  (->> lod-file
       (h3m-parser/parse-lod)
       :files
       (filter #(utils/coll-includes? (:type %) [def-map def-terrain]))
       (filter #(re-matches #"(?i).*\.def" (:name %)))))


(defn parse-map-sprites
  [^FileInputStream lod-file
   ^FileHandle atlas-file
   ^FileHandle info-file
   item-callback
   done-callback]
  (let [packer (new PixmapPacker 4096 4096 Pixmap$Format/RGBA8888 0 false)
        lod-files-list (get-lod-files-list lod-file)
        files-count (dec (count lod-files-list))
        defs (map #(parse-def-from-lod % lod-file) lod-files-list)]
    (pack-defs packer defs files-count item-callback)
    (save-packer packer atlas-file)
    (.dispose packer)
    (save-defs-info defs info-file)
    (done-callback)))


(defn parse-map [^InputStream file]
  (let [h3m-map (h3m-parser/parse-h3m file)]
    (update
     h3m-map
     :objects
     (fn [objects]
       (->> objects
            (map #(assoc % :def (nth (:defs h3m-map) (:def-index %))))
            (map random/replace-random-objects))))))