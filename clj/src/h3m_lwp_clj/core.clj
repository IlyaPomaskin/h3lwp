(ns h3m-lwp-clj.core
  (:import [com.badlogic.gdx ApplicationAdapter Gdx InputProcessor Application$ApplicationType]
           [com.badlogic.gdx.graphics Texture GL20 OrthographicCamera]
           [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [com.badlogic.gdx.utils Timer Timer$Task])
  (:require
   [h3m-parser.core :as h3m]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.rect :as rect]
   [h3m-lwp-clj.consts :as consts])
  (:gen-class
   :name h3m.LwpCore
   :extends com.badlogic.gdx.ApplicationAdapter))


(def update-rect-interval (* 60 15))
(def sprite-render-interval 180)
(def scale-factor 2)


(def h3m-map (atom nil))
(def batch (atom nil))
(def camera (atom nil))
(def rect (atom {}))
(def visible-objects (atom []))
(def terrain-tiles (atom []))


(defn create-camera
  [screen-width screen-height scale-factor]
  (let [camera (new OrthographicCamera screen-width screen-height)]
    (set! (.-zoom camera) (/ 1.0 scale-factor))
    (.setToOrtho camera true)
    camera))


(defn rect-watcher
  [next-rect]
  (reset! visible-objects (objects/get-visible-objects next-rect @h3m-map))
  (reset! terrain-tiles (terrain/get-visible-tiles next-rect @h3m-map))
  (.set (.position ^OrthographicCamera @camera)
        (* consts/tile-size
           (+ (:x1 next-rect)
              (quot (- (:x2 next-rect)
                       (:x1 next-rect))
                    2)))
        (* consts/tile-size
           (+ (:y1 next-rect)
              (quot (- (:y2 next-rect)
                       (:y1 next-rect))
                    2)))
        0)
  (println @rect))


(defn update-rect
  [screen-width screen-height]
  (reset!
   rect
   (rect/get-random (:size @h3m-map) screen-width screen-height)))


(def input-processor-proxy
  (proxy [InputProcessor] []
    (keyDown [keycode] true)
    (keyTyped [keycode] true)
    (keyUp [keycode] true)
    (mouseMoved [x y] true)
    (scrolled [amount] true)
    (touchDown [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button] true)
    (touchUp
      [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button]
      (when (= (.getType (Gdx/app)) (Application$ApplicationType/Desktop))
        (let [screen-width (/ (.getWidth (Gdx/graphics)) scale-factor)
              screen-height (/ (.getHeight (Gdx/graphics)) scale-factor)]
          (update-rect screen-width screen-height)))
      true)
    (touchDragged
      [^Integer screen-x ^Integer screen-y ^Integer pointer]
      (when (= (.getType (Gdx/app)) (Application$ApplicationType/Desktop))
        (.translate
         ^OrthographicCamera @camera
         (.getDeltaX (Gdx/input))
         (.getDeltaY (Gdx/input))))
      true)))


(defn -create
  [^ApplicationAdapter _]
  (let [screen-width (/ (.getWidth (Gdx/graphics)) scale-factor)
        screen-height (/ (.getHeight (Gdx/graphics)) scale-factor)]
    (reset! h3m-map (-> Gdx/files
                        (.internal "maps/invasion.h3m")
                        (.read)
                        (h3m/parse-file)
                        (objects/sort-map-objects)))
    (reset! batch (new SpriteBatch))
    (reset! camera (create-camera screen-width screen-height scale-factor))
    (.setInputProcessor (Gdx/input) input-processor-proxy)
    (add-watch rect :watcher #(rect-watcher %4))
    (Texture/setAssetManager assets/manager)
    (.finishLoading assets/manager)
    (.scheduleTask
     (new Timer)
     (proxy [Timer$Task] []
       (run [] (update-rect screen-width screen-height)))
     (float 0)
     (float update-rect-interval))))


(defn -render
  [^ApplicationAdapter _]
  (let [^OrthographicCamera camera (deref camera)
        ^SpriteBatch batch (deref batch)]
    (Thread/sleep (long sprite-render-interval))
    (doto Gdx/gl
      (.glClearColor 0 0 0 0)
      (.glEnable GL20/GL_BLEND)
      (.glBlendFunc GL20/GL_SRC_ALPHA GL20/GL_ONE_MINUS_SRC_ALPHA)
      (.glClear GL20/GL_COLOR_BUFFER_BIT))
    (.update camera)
    (doto batch
      (.setTransformMatrix (.-view camera))
      (.setProjectionMatrix (.-projection camera))
      (.enableBlending)
      (.begin)
      (terrain/render-terrain-tiles @terrain-tiles)
      (objects/render-objects @visible-objects)
      (.end))))


(defmacro repl
  "Executes body on main Gdx thread"
  [body]
  `(.postRunnable
    Gdx/app
    (proxy [Runnable] []
      (run []
        (try
          ~body
          (catch
           Exception
           e#
            (do
              (println (str "repl macro exception: " (.getMessage e#)))
              (println (.printStackTrace e#)))))))))

