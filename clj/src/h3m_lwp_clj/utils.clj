(ns h3m-lwp-clj.utils
  (:import
   [com.badlogic.gdx.utils Array]))


(defn rect-contain?
  [x y {x1 :x1
        y1 :y1
        x2 :x2
        y2 :y2}]
  (and (>= x x1)
       (<= x x2)
       (>= y y1)
       (<= y y2)))


(defn rect-increase [rect amount]
  {:x1 (- (:x1 rect) amount)
   :y1 (- (:y1 rect) amount)
   :width (+ (:width rect) (* 2 amount))
   :height (+ (:height rect) (* 2 amount))
   :x2 (+ (:x2 rect) amount)
   :y2 (+ (:y2 rect) amount)})


(defn map-libgdx-array
  ^Array
  [fn ^Array array]
  (let [next-array (new Array)]
    (loop [index 0]
      (if (< index (.size array))
        (do
          (.add next-array (fn (.get array index)))
          (recur (unchecked-inc index)))
        next-array))))


(defn rotate-right [amount list]
  (let [offset (rem amount (count list))
        tail (take-last offset list)
        head (drop-last offset list)]
    (concat tail head)))


(defn rotate-items [list from to amount]
  (vec
   (concat
    (subvec list 0 from)
    (rotate-right amount (subvec list from to))
    (subvec list to))))


(defn coll-includes? [item coll]
  (some #(= % item) coll))