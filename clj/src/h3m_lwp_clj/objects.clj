(ns h3m-lwp-clj.objects
  (:import
   [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
   [com.badlogic.gdx.graphics OrthographicCamera]
   [com.badlogic.gdx.utils TimeUtils])
  (:require
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.utils :as utils]
   [h3m-lwp-clj.orth-camera :as orth-camera]
   [h3m-lwp-clj.random :as random]
   [h3m-lwp-clj.consts :as consts]
   [clojure.string :as string]))


(defn get-order
  [object]
  (get-in object [:def :placement-order]))


(defn def-visitable?
  [object]
  (= [0 0]
     (get-in object [:def :active-cells])))


(defn compare-objects
  [a b]
  (cond
    (not= (get-order a) (get-order b)) (> (get-order a) (get-order b))
    (not= (:y a) (:y b)) (< (:y a) (:y b))
    (and (not= (:object a) :hero) (= (:object b) :hero)) true
    (and (not= (:object b) :hero) (= (:object a) :hero)) false
    (and (not (def-visitable? a)) (def-visitable? b)) false
    (and (not (def-visitable? b)) (def-visitable? a)) true
    (< (:x a) (:x b)) false
    :else false))


(defn object->filename
  [object]
  (-> (get-in object [:def :sprite-name])
      (string/lower-case)
      (string/replace #"\.def" "")))


(defn get-frame-index
  [initial-time frames-count offset-frame]
  (mod (+ (quot (- (TimeUtils/millis) initial-time)
                (* 1000 consts/animation-interval))
          offset-frame)
       frames-count))


(defn create-sprite [object]
  (let [filename (object->filename object)
        {x :x
         y :y} object
        {frames :frames
         full-width :full-width
         full-height :full-height} (assets/get-map-object-frames filename)
        initial-time (TimeUtils/millis)
        frames-count (count frames)
        offset-frame (rand-int frames-count)]
    (if (empty? frames)
      {:x x
       :y y
       :render-sprite
       (fn render-nil-sprite [_] nil)}
      {:x x
       :y y
       :render-sprite
       (fn render-sprite [^SpriteBatch batch]
         (.draw
          batch
          ^TextureRegion (nth frames (get-frame-index initial-time frames-count offset-frame))
          (float (- (* (inc x) consts/tile-size)
                    full-width))
          (float (- (* (inc y) consts/tile-size)
                    full-height))))})))


(defn get-map-objects
  [h3m-map]
  (doall
   (->> (:objects h3m-map)
        (filter #(zero? (:z %)))
        (pmap #(assoc % :def (nth (:defs h3m-map) (:def-index %))))
        (pmap random/replace-random-objects)
        (sort compare-objects)
        ;; (reverse)
        (pmap create-sprite))))


(defonce get-visible-sprites-cache (atom {:x nil :y nil :objects []}))


(defn get-visible-sprites-
  [^OrthographicCamera camera objects]
  (let [rectangle (utils/rect-increase (orth-camera/get-rect camera) 3)]
    (filter
     #(utils/rect-contain? (:x %) (:y %) rectangle)
     objects)))


(defn get-visible-sprites
  [^OrthographicCamera camera objects]
  (let [x (.x (.position camera))
        y (.y (.position camera))
        {prev-x :x
         prev-y :y} @get-visible-sprites-cache]
    (when (or (not= x prev-x) (not= y prev-y))
      (swap! get-visible-sprites-cache assoc
             :x x
             :y y
             :objects (get-visible-sprites- camera objects)))
    (:objects @get-visible-sprites-cache)))


(defn create-renderer
  [h3m-map]
  (let [batch (new SpriteBatch)
        sprites (get-map-objects h3m-map)]
    (fn render-objects [^OrthographicCamera camera]
      (.setProjectionMatrix batch (.-combined camera))
      (.update camera)
      (.begin batch)
      (->> sprites
           (get-visible-sprites camera)
           (mapv (fn [{render-sprite :render-sprite}]
                   (render-sprite batch))))
      (.end batch))))
