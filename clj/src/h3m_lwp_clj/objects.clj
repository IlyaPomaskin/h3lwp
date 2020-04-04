(ns h3m-lwp-clj.objects
  (:import
   [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
   [com.badlogic.gdx.graphics OrthographicCamera]
   [com.badlogic.gdx.utils TimeUtils])
  (:require
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.utils :as utils]
   [h3m-lwp-clj.orth-camera :as orth-camera]
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
        {frames :frames
         full-width :full-width
         full-height :full-height} (assets/get-map-object-frames filename)
        initial-time (TimeUtils/millis)
        frames-count (count frames)
        offset-frame (rand-int frames-count)]
    (if (empty? frames)
      (fn render-nil-sprite [_] nil)
      (fn render-sprite [^SpriteBatch batch]
        (.draw
         batch
         ^TextureRegion (nth frames (get-frame-index initial-time frames-count offset-frame))
         (float (- (* (inc (:x object)) consts/tile-size)
                   full-width))
         (float (- (* (inc (:y object)) consts/tile-size)
                   full-height)))))))


(defonce get-map-objects-cache
  (atom {:x nil :y nil :objects []}))


(defn get-map-objects-
  [h3m-map ^OrthographicCamera camera]
  (let [rectangle (utils/rect-increase (orth-camera/get-rect camera) 3)]
    (->> (:objects h3m-map)
         (filter
          #(and
            (utils/rect-contain? (:x %) (:y %) rectangle)
            (zero? (:z %))))
         (sort compare-objects)
         (mapv create-sprite))))


(defn get-map-objects
  [h3m-map ^OrthographicCamera camera]
  (let [current-x (.x (.position camera))
        current-y (.y (.position camera))
        {prev-x :x
         prev-y :y} @get-map-objects-cache]
    (when (or (not= current-x prev-x) (not= current-y prev-y))
      (swap! get-map-objects-cache assoc
             :x current-x
             :y current-y
             :objects (get-map-objects- h3m-map camera)))
    (:objects @get-map-objects-cache)))


(defn create-renderer
  [h3m-map]
  (let [batch (new SpriteBatch)]
    (fn render-objects [^OrthographicCamera camera]
      (.setProjectionMatrix batch (.-combined camera))
      (.update camera)
      (.begin batch)
      (mapv
       (fn [render-sprite] (render-sprite batch))
       (get-map-objects h3m-map camera))
      (.end batch))))