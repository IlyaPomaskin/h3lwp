(ns h3m-lwp-clj.utils
  (:import [com.badlogic.gdx.utils Array]))


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

