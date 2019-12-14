(ns h3m-lwp-clj.random
  (:require [h3m-lwp-clj.consts :as consts]))


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


(defn get-random-hero
  []
  ; TODO
  ; (format "ah%02d_e.def" (rand-int 18))
  "empty")


(defn replace-random-objects
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
     :random-hero (get-random-hero)
     :random-dwelling (get-random-dwelling (rand-int (:max object)))
     :random-dwelling-lvl (get-random-dwelling)
     :random-dwelling-faction (get-random-dwelling)
     (get-in object [:def :sprite-name]))))

