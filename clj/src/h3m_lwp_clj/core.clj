(ns h3m-lwp-clj.core
  (:import [com.badlogic.gdx ApplicationAdapter Gdx InputProcessor Application$ApplicationType]
           [com.badlogic.gdx.graphics GL20 OrthographicCamera]
           [com.badlogic.gdx.maps.tiled.renderers OrthogonalTiledMapRenderer]
           [com.badlogic.gdx.utils Timer Timer$Task]
           [com.badlogic.gdx Input$Keys])
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.rect :as rect]
   [h3m-lwp-clj.consts :as consts])
  (:gen-class
   :name h3m.LwpCore
   :extends com.badlogic.gdx.ApplicationAdapter))


(defmacro repl
  [body]
  `(.postRunnable
    Gdx/app
    (reify Runnable
      (run [_]
        (try
          ~body
          (catch
           Exception
           e#
            (do
              (println (str "repl exception: " (.getMessage e#)))
              (println (.printStackTrace e#)))))))))


(def camera-position-update-interval (* 60 15))
(def scale-factor 0.5)


(defonce h3m-map (atom nil))
(defonce camera (atom nil))
(defonce terrain-renderer (atom nil))
(defonce objects-renderer (atom nil))


(defn create-camera
  [scale-factor]
  (let [camera (new OrthographicCamera)]
    (set! (.-zoom camera) scale-factor)
    (.setToOrtho camera true)
    (.update camera)
    camera))


(defn set-random-camera-position
  [^OrthographicCamera camera map-size]
  (let [box (rect/get-camera-rect camera)
        x-offset (Math/floor (/ (:width box) 2))
        y-offset (Math/floor (/ (:height box) 2))
        next-x (* consts/tile-size
                  (+ x-offset (rand-int (- map-size (:width box)))))
        next-y (* consts/tile-size
                  (+ y-offset (rand-int (- map-size (:height box)))))]
    (.set (.position camera) next-x next-y 0)))


(defn is-desktop?
  []
  (= (.getType (Gdx/app))
     (Application$ApplicationType/Desktop)))


(def input-processor
  (proxy [InputProcessor] []
    (keyDown [keycode] true)
    (keyTyped
     [keycode]
     (when (is-desktop?)
       (let [pressed? (fn [key _] (.isKeyPressed (Gdx/input) key))
             ^OrthographicCamera camera (deref camera)]
         (condp pressed? keycode
           Input$Keys/NUM_0 (set! (.-zoom camera) 1)
           Input$Keys/EQUALS (set! (.-zoom camera) (- (.-zoom camera) 0.1))
           Input$Keys/MINUS (set! (.-zoom camera) (+ (.-zoom camera) 0.1))
           Input$Keys/UP (.translate camera 0 (- consts/tile-size) 0)
           Input$Keys/DOWN (.translate camera 0 consts/tile-size 0)
           Input$Keys/LEFT (.translate camera (- consts/tile-size) 0 0)
           Input$Keys/RIGHT (.translate camera consts/tile-size 0 0)
           Input$Keys/SPACE (set-random-camera-position camera (:size @h3m-map))
           nil)
         true)))
    (keyUp [keycode] true)
    (mouseMoved [x y] true)
    (scrolled [amount] true)
    (touchDown [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button] true)
    (touchUp [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button] true)
    (touchDragged
      [^Integer screen-x ^Integer screen-y ^Integer pointer]
      (let [^OrthographicCamera camera (deref camera)]
        (when (is-desktop?)
          (.translate camera (.getDeltaX (Gdx/input)) (.getDeltaY (Gdx/input)))))
      true)))


(defn -create
  [^ApplicationAdapter _]
  (.finishLoading assets/manager)
  (.setInputProcessor (Gdx/input) input-processor)
  (reset! h3m-map (h3m-parser/parse-h3m (.read (.internal Gdx/files "maps/invasion.h3m"))))
  (reset! camera (create-camera scale-factor))
  (reset! terrain-renderer (terrain/create-renderer @h3m-map))
  (reset! objects-renderer (objects/create-renderer @h3m-map))
  (.scheduleTask
   (new Timer)
   (proxy [Timer$Task] []
     (run [] (set-random-camera-position @camera (:size @h3m-map))))
   (float 0)
   (float camera-position-update-interval)))


(comment
  (repl
   (reset! terrain-renderer (terrain/create-renderer @h3m-map))))

(comment
  (repl
   (reset! objects-renderer (objects/create-renderer @h3m-map))))


(defn -render
  [^ApplicationAdapter _]
  (let [^OrthographicCamera camera (deref camera)
        terrain-renderer ^OrthogonalTiledMapRenderer (deref terrain-renderer)
        objects-renderer (deref objects-renderer)]
    (doto Gdx/gl
      (.glClearColor 0 0 0 0)
      (.glClear GL20/GL_COLOR_BUFFER_BIT))
    (.update camera)
    (doto terrain-renderer
      (.setView camera)
      (.render))
    (objects-renderer camera)))

