(ns h3m-lwp-clj.objects
  (:import
   [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
   [com.badlogic.gdx.graphics OrthographicCamera]
   [com.badlogic.gdx.utils TimeUtils])
  (:require
   [h3m-lwp-clj.assets :as assets]
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


(defn create-sprite [^SpriteBatch batch object]
  (let [filename (object->filename object)
        sprite-info (assets/get-sprite-info filename)
        {frames-order :order} sprite-info
        frames (mapv #(assets/get-object-frame filename %) frames-order)
        initial-time (TimeUtils/millis)
        frames-count (count frames-order)
        offset-frame (rand-int frames-count)]
    (if (nil? sprite-info)
      (do
        (println "NOT FOUND" filename)
        (fn render-nil-sprite [] nil))
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
           (float (- (* (inc (:x object)) consts/tile-size)
                     (:full-width sprite-info)))
           (float (- (* (inc (:y object)) consts/tile-size)
                     (:full-height sprite-info)))))))))


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
  (let [rectangle (rect/add (rect/get-camera-rect camera) 3)]
    (filter
     #(rect/contain? (:x %) (:y %) rectangle)
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
      (.setTransformMatrix batch (.-view camera))
      (.setProjectionMatrix batch (.-projection camera))
      (.begin batch)
      (dorun
       (->> sprites
            (get-visible-sprites camera)
            (map (fn [{render-sprite :render-sprite}]
                   (render-sprite)))))
      (.end batch))))
