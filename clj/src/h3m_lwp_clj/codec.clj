(ns h3m-lwp-clj.codec
  (:require [org.clojars.smee.binary.core :as b]
            [clojure.pprint :as pp]
            [h3m-parser.objects :as h3m-objects])
  (:import org.clojars.smee.binary.core.BinaryIO
           ; import from smee/binary/java-src/impl
           impl.LittleEndianDataInputStream))


(defn reduce-kvs
  "Like `reduce-kv` but takes a flat sequence of kv pairs."
  [rf init kvs]
  (transduce (partition-all 2)
             (completing (fn [acc [k v]] (rf acc k v))) init kvs))


(defn cond-codec
  "Codec for selecting nested codec based on map value"
  [& kvs]
  (reify BinaryIO
    (read-data [_ big-in little-in]
      (reduce-kvs
       (fn [decoded-map param-key param-config]
         (if-some [codec (if (fn? param-config)
                           (param-config decoded-map)
                           param-config)]
           (try
             (some-> codec
                     (b/read-data big-in little-in)
                     (as-> v (assoc decoded-map param-key v)))
             (catch java.io.EOFException e
               (println ["param-key" param-key])
               (println ["decoded-map" decoded-map])
               (throw e)))
           decoded-map))
       {}
       kvs))

    (write-data [_ big-out little-out value-map]
      big-out)))


(def int-sized-string (b/string "ISO-8859-1" :prefix :int-le))


(def byte->bool (b/compile-codec :byte #(if (true? %1) 1 0) pos?))


(def int->object (b/compile-codec :int-le (constantly 99) #(get h3m-objects/object-by-id %1 :no-obj)))


(defn logger
  ([prefix]
   (logger prefix identity))
  ([prefix getter]
   (fn [data]
     (pp/pprint prefix)
     (pp/pprint (getter data))
     nil)))


(def reader-position
  (reify BinaryIO
    (read-data [codec big-in little-in]
      (.size ^LittleEndianDataInputStream little-in))
    (write-data [codec big-out little-out value]
      big-out)))

      
(defn constant [value]
  (reify BinaryIO
    (read-data [codec big-in little-in]
      value)
    (write-data [codec big-out little-out value]
      big-out)))


(defn offset-assert [expected-offset entity-name]
  (reify BinaryIO
    (read-data [codec big-in little-in]
      (let [current-position (b/read-data reader-position big-in little-in)]
        (when (not (= current-position expected-offset))
          (throw
           (new
            AssertionError
            (format
             "Wrong offset while parsing %s. Expected: %d, current %d"
             entity-name
             expected-offset
             current-position))))))
    (write-data [codec big-out little-out value]
      big-out)))

    
(defn move-cursor-forward [offset]
  (reify BinaryIO
    (read-data [codec big-in little-in]
      (let [current-position (b/read-data reader-position big-in little-in)
            skip-length (- offset current-position)]
        (when (pos? skip-length)
          (println "Need to move cursor. pos:" current-position "should be:" offset)
          (.skipBytes ^LittleEndianDataInputStream little-in skip-length))))
    (write-data [codec big-out little-out value]
      big-out)))


(defn read-lines [codec length-key initial-length]
  (reify BinaryIO
    (read-data [_ big-in little-in]
      (loop [length initial-length
             result []]
        (let [data (b/read-data codec big-in little-in)
              next-length (- length (get data length-key))
              next-result (conj result data)]
          (if (pos? next-length)
            (recur next-length next-result)
            next-result)))

      ; TODO rewrite:
      ; (loop [length initial-length
      ;        result []]
      ;   (if (pos? length)
      ;     (let [data (b/read-data codec big-in little-in)]
      ;       (recur (- length (get data length-key))
      ;              (conj result data)))
      ;     result))
      )
    (write-data [_ big-out little-out length]
      nil)))