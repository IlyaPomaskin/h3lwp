(ns h3m-lwp-clj.objects
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
           [com.badlogic.gdx.graphics OrthographicCamera]
           [com.badlogic.gdx.utils Array TimeUtils])
  (:require [h3m-lwp-clj.assets :as assets]
            [h3m-lwp-clj.rect :as rect]
            [h3m-lwp-clj.random :as random]
            [h3m-lwp-clj.consts :as consts]))


(defn get-placement-order
  [object]
  (get-in object [:def :placement-order]))


(defn def-visitable?
  [object]
  (= [0 0]
     (get-in object [:def :active-cells])))


; TODO rewrite
(defn compare-objects
  [a b]
  (cond
    (not= (get-placement-order a)
          (get-placement-order b)) (if (>= (get-placement-order a)
                                           (get-placement-order b)) 1 -1)
    (not= (:y a) (:y b)) (if (>= (:y a) (:y b)) -1 1)
    (and (not= (:object a) :hero) (= (:object b) :hero)) 1
    (and (not= (:object b) :hero) (= (:object a) :hero)) -1
    (and (false? (def-visitable? a))
         (true? (def-visitable? b))) -1
    (and (false? (def-visitable? b))
         (true? (def-visitable? a))) 1
    (<= (:x a) (:x b)) 1
    :else -1))


(defn object->filename
  [object]
  (-> (get-in object [:def :sprite-name])
      (clojure.string/lower-case)
      (clojure.string/replace #"\.def" "")))


(defn get-frame-index
  [initial-time current-time frames-count offset-frame]
  (mod (+ (quot (- current-time initial-time)
                (* 1000 consts/animation-interval))
          offset-frame)
       frames-count))


(defn create-sprite
  [^Array frames]
  (let [initial-time (TimeUtils/millis)
        frames-count (.size frames)
        offset-frame (rand-int frames-count)]
    (fn render-sprite []
      (.get
       frames
       (get-frame-index
        initial-time
        (TimeUtils/millis)
        frames-count
        offset-frame)))))


(defn get-sprites
  [h3m-map]
  (->> (:objects h3m-map)
       (filter #(zero? (:z %)))
       (pmap #(assoc % :def (nth (:defs h3m-map) (:def-index %))))
       (pmap random/replace-random-objects)
       (sort compare-objects)
       (reverse)
       (pmap #(hash-map
               :render-sprite (create-sprite (assets/get-object (object->filename %)))
               :x (inc (:x %))
               :y (inc (:y %))))))


(defn get-visible-sprites
  [^OrthographicCamera camera sprites]
  (let [rectangle (rect/add (rect/get-camera-rect camera) 3)]
    (filterv
     #(rect/contain? (:x %) (:y %) rectangle)
     sprites)))


(defn render-sprites
  [^SpriteBatch batch ^OrthographicCamera camera sprites]
  (doall
   (for [sprite (get-visible-sprites camera sprites)]
     (let [{render-sprite :render-sprite
            x :x
            y :y} sprite
           ^TextureRegion frame (render-sprite)
           x-position (float (- (* x consts/tile-size)
                                (.getRegionWidth frame)))
           y-position (float (- (* y consts/tile-size)
                                (.getRegionHeight frame)))]
       (.draw
        batch
        frame
        x-position
        y-position)))))


(defn create-renderer
  [h3m-map]
  (let [batch (new SpriteBatch)
        sprites (get-sprites h3m-map)]
    (fn render-objects [camera]
      (.setTransformMatrix batch (.-view ^OrthographicCamera camera))
      (.setProjectionMatrix batch (.-projection ^OrthographicCamera camera))
      (.begin batch)
      (render-sprites batch camera sprites)
      (.end batch))))
