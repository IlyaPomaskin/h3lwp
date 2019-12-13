(ns h3m-lwp-clj.objects
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])
  (:require [h3m-lwp-clj.assets :as assets]
            [h3m-lwp-clj.rect :as rect]
            [h3m-lwp-clj.random :as random]
            [h3m-lwp-clj.consts :as consts]))


(defn get-placement-order
  [h3m-map object]
  (get-in h3m-map [:defs (:def-index object) :placement-order]))


(defn is-def-visitable
  [h3m-map object]
  (= [0 0]
     (get-in h3m-map [:defs (:def-index object) :active-cells])))


; TODO rewrite
(defn create-compare-objects
  [h3m-map]
  (fn
    [a b]
    (cond
      (not= (get-placement-order h3m-map a)
            (get-placement-order h3m-map b)) (if (>= (get-placement-order h3m-map a)
                                                     (get-placement-order h3m-map b)) 1 -1)
      (not= (:y a) (:y b)) (if (>= (:y a) (:y b)) -1 1)
      (and (not= (:object a) :hero) (= (:object b) :hero)) 1
      (and (not= (:object b) :hero) (= (:object a) :hero)) -1
      (and (false? (is-def-visitable h3m-map a))
           (true? (is-def-visitable h3m-map b))) -1
      (and (false? (is-def-visitable h3m-map b))
           (true? (is-def-visitable h3m-map a))) 1
      (<= (:x a) (:x b)) 1
      :else -1)))


(defn def->filename
  [def]
  (-> (get def :sprite-name)
      (clojure.string/lower-case)
      (clojure.string/replace #"\.def" "")))


(defn render-objects
  [^SpriteBatch batch objects]
  (doseq [{get-frame :get-frame
           x-position :x-position
           y-position :y-position} objects]
    (.draw
     batch
     ^TextureRegion (get-frame)
     (float x-position)
     (float y-position))))


(defn get-visible-objects
  [rect h3m-map]
  (->> (:objects h3m-map)
       (filterv #(and (zero? (:z %))
                      ; (rect/contain? (:x %) (:y %) rect)
                      ))
       (pmap #(assoc %1 :def (nth (:defs h3m-map) (:def-index %))))
       (sort (create-compare-objects h3m-map))
       (reverse)
       (pmap #(random/replace-random-objects %1))
       (pmap #(let [object-frames (assets/get-object (def->filename (:def %)))
                    get-frame (assets/create-sprite object-frames true)
                    ^TextureRegion frame (get-frame)
                    x-position (float (+ consts/tile-size
                                         (- (* consts/tile-size (:x %))
                                            (.getRegionWidth frame))))
                    y-position (float (+ consts/tile-size
                                         (- (* consts/tile-size (:y %))
                                            (.getRegionHeight frame))))]
                {:get-frame get-frame
                 :x-position x-position
                 :y-position y-position}))))
