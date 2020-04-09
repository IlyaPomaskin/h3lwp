(ns h3m-lwp-clj.objects
  (:import
   [com.badlogic.gdx.graphics OrthographicCamera]
   [com.badlogic.gdx.graphics.g2d SpriteBatch TextureAtlas$AtlasRegion]
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


(defn create-frame-getter
  [frames]
  (let [initial-time (TimeUtils/millis)
        frames-count (count frames)
        frame-offset (rand-int frames-count)]
    (fn frame-getter
      ^TextureAtlas$AtlasRegion
      []
      (nth
       frames
       (mod (+ (quot (- (TimeUtils/millis) initial-time)
                     (* 1000 consts/animation-interval))
               frame-offset)
            frames-count)))))


(defn get-frame-x
  [map-object ^TextureAtlas$AtlasRegion frame]
  (+ (- (* (inc (:x map-object)) consts/tile-size)
        (.-originalWidth frame))
     (.-offsetX frame)))


(defn get-frame-y
  [map-object ^TextureAtlas$AtlasRegion frame]
  (+ (- (* (inc (:y map-object)) consts/tile-size)
        (.-originalHeight frame))
     (.-offsetY frame)))


(defn create-sprite
  [map-object]
  (let [def-name (object->filename map-object)
        frames (assets/get-map-object-frames def-name)
        get-frame (create-frame-getter frames)]
    (if (empty? frames)
      (fn render-nil-sprite [_] nil)
      (fn render-sprite [^SpriteBatch batch]
        (let [^TextureAtlas$AtlasRegion frame (get-frame)]
          (.draw
           batch
           frame
           (float (get-frame-x map-object frame))
           (float (get-frame-y map-object frame))))))))


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
  [^SpriteBatch batch ^OrthographicCamera camera h3m-map]
  (fn render-objects []
    (.setProjectionMatrix batch (.-combined camera))
    (.update camera)
    (.begin batch)
    (mapv
     (fn [render-sprite] (render-sprite batch))
     (get-map-objects h3m-map camera))
    (.end batch)))