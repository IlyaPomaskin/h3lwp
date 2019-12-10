(ns h3m-lwp-clj.objects
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
           [com.badlogic.gdx.utils Array])
  (:require [h3m-lwp-clj.assets :as assets]
            [h3m-lwp-clj.rect :as rect]
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


(defn sort-map-objects
  [h3m-map]
  (update-in
   h3m-map
   [:objects]
   #(->> %
         (sort (create-compare-objects h3m-map))
         (reverse))))


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


(defn get-random-resource
  []
  (rand-nth consts/resource))


(defn get-random-monster
  ([]
   (get-random-monster (rand-nth (keys consts/monster))))
  ([level]
   (rand-nth (get consts/monster level))))


(defn get-random-dwelling
  ([]
   (get-random-dwelling
    (rand-nth (keys consts/dwelling))
    (rand-int 6)))
  ([level]
   (get-random-dwelling
    (rand-nth (keys consts/dwelling))
    level))
  ([faction level]
   (get-in consts/dwelling [faction level])))


(defn get-random-town
  ([]
   (get-random-town (rand-nth (keys consts/town))))
  ([faction]
   (let [fort? (pos? (rand-int 1))
         factions-def (if fort? consts/town consts/village)
         factions-def-keys (keys factions-def)
         default-faction (nth factions-def-keys 0)
         key (nth factions-def-keys (or faction 0) default-faction)]
     (get factions-def key))))


(defn get-random-artifact
  ([]
   (format "AVA%04d.def" (+ 10 (rand-int 117))))
  ([relic?]
   (format
    "AVA%04d.def"
    (if relic?
      (+ 129 (rand-int 12))
      (+ 10 (rand-int 117))))))


(defn replace-random-item
  [object]
  (assoc-in
   object
   [:def :sprite-name]
   (case (get-in object [:def :object])
     :random-monster (get-random-monster)
     :random-monster-l1 (get-random-monster 1)
     :random-monster-l2 (get-random-monster 2)
     :random-monster-l3 (get-random-monster 3)
     :random-monster-l4 (get-random-monster 4)
     :random-monster-l5 (get-random-monster 5)
     :random-monster-l6 (get-random-monster 6)
     :random-monster-l7 (get-random-monster 7)

     :random-art (get-random-artifact)
     :random-treasure-art (get-random-artifact)
     :random-minor-art (get-random-artifact)
     :random-major-art (get-random-artifact)
     :random-relic-art (get-random-artifact true)

     :random-resource (get-random-resource)

     :random-town (get-random-town (:class-sub-id object))

     :random-dwelling (get-random-dwelling (rand-int (:max object)))
     :random-dwelling-lvl (get-random-dwelling)
     :random-dwelling-faction (get-random-dwelling)
     (get-in object [:def :sprite-name]))))



(defn get-visible-objects
  [rect
   {objects :objects
    defs :defs}]
  (->> objects
       (filterv #(and (zero? (:z %))
                      (rect/contain? (:x %) (:y %) rect)))
       (pmap #(assoc %1 :def (nth defs (:def-index %))))
       (pmap #(replace-random-item %1))
       (pmap #(let [object-frames (assets/get-object (def->filename (:def %)))
                    get-frame (assets/create-sprite object-frames true)
                    ^TextureRegion frame (get-frame)
                    x-position (float (- (* consts/tile-size (:x %))
                                         (.getRegionWidth frame)))
                    y-position (float (- (* consts/tile-size (:y %))
                                         (.getRegionHeight frame)))]
                {:get-frame get-frame
                 :x-position x-position
                 :y-position y-position}))))
