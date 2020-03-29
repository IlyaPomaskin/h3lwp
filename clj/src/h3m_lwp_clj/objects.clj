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
      (string/lower-case)
      (string/replace #"\.def" "")))


(defn get-frame-index
  [initial-time current-time frames-count offset-frame]
  (mod (+ (quot (- current-time initial-time)
                (* 1000 consts/animation-interval))
          offset-frame)
       frames-count))


(defn create-sprite [^SpriteBatch batch object]
  (let [filename (object->filename object)
        {frames :frames
         full-width :full-width
         full-height :full-height} (assets/get-map-object-frames filename)
        initial-time (TimeUtils/millis)
        frames-count (count frames)
        offset-frame (rand-int frames-count)]
    (if (empty? frames)
      (fn render-nil-sprite [] nil)
      (fn render-sprite []
        (let [^TextureRegion
              frame (nth
                     frames
                     (get-frame-index
                      initial-time
                      (TimeUtils/millis)
                      frames-count
                      offset-frame))]
          (.draw
           batch
           frame
           (float (- (* (inc (:x object))
                        consts/tile-size)
                     full-width))
           (float (- (* (inc (:y object))
                        consts/tile-size)
                     full-height))))))))


(defn get-map-objects
  [h3m-map]
  (->> (:objects h3m-map)
       (filter #(zero? (:z %)))
       (pmap #(assoc % :def (nth (:defs h3m-map) (:def-index %))))
       (pmap random/replace-random-objects)
       (sort compare-objects)
       (reverse)))


; TODO memoize by camera position
(defn get-visible-sprites
  [^OrthographicCamera camera objects]
  (let [rectangle (utils/rect-increase (orth-camera/get-rect camera) 3)]
    (filter
     #(utils/rect-contain? (:x %) (:y %) rectangle)
     objects)))


(defn create-renderer
  [h3m-map]
  (let [batch (new SpriteBatch)
        sprites (->> (get-map-objects h3m-map)
                     (map
                      #(hash-map
                        :render-sprite (create-sprite batch %)
                        :x (inc (:x %))
                        :y (inc (:y %)))))]
    (fn render-objects [^OrthographicCamera camera]
      (.setProjectionMatrix batch (.-combined camera))
      (.update camera)
      (.begin batch)
      (dorun
       (->> sprites
            (get-visible-sprites camera)
            (map (fn [{render-sprite :render-sprite}]
                   (render-sprite)))))
      (.end batch))))
