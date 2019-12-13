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


(def update-rect-interval (* 60 15))
(def sprite-render-interval 180)
(def scale-factor 2)


(defonce h3m-map (atom nil))
(defonce batch (atom nil))
(defonce camera (atom nil))
(defonce rect (atom {:x1 20 :y1 20 :x2 40 :y2 40}))
(defonce visible-objects (atom []))
(defonce terrain-renderer (atom nil))


(defn create-camera
  [scale-factor]
  (let [camera (new OrthographicCamera)]
    (set! (.-zoom camera) (/ 1.0 scale-factor))
    (.setToOrtho camera true)
    (.update camera)
    camera))


(defn rect-watcher
  [next-rect]
  (reset! visible-objects (objects/get-visible-objects next-rect @h3m-map))
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


(defn is-desktop?
  []
  (= (.getType (Gdx/app))
     (Application$ApplicationType/Desktop)))


(def input-processor
  (proxy [InputProcessor] []
    (keyDown
      [keycode]
      (when (is-desktop?)
        (case keycode
          69  (set! (.-zoom ^OrthographicCamera @camera) (+ (.-zoom ^OrthographicCamera @camera) 0.1))
          70  (set! (.-zoom ^OrthographicCamera @camera) (- (.-zoom ^OrthographicCamera @camera) 0.1))
          19  (.translate ^OrthographicCamera @camera 0 -3 0)
          20  (.translate ^OrthographicCamera @camera 0 3 0)
          21  (.translate ^OrthographicCamera @camera -3 0 0)
          22  (.translate ^OrthographicCamera @camera 3 0 0)
          62  (let [screen-width (/ (.getWidth (Gdx/graphics)) scale-factor)
                    screen-height (/ (.getHeight (Gdx/graphics)) scale-factor)]
                (update-rect screen-width screen-height))
          nil))
      true)
    (keyTyped [keycode] true)
    (keyUp [keycode] true)
    (mouseMoved [x y] true)
    (scrolled [amount] true)
    (touchDown [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button] true)
    (touchUp
      [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button]
      (when (is-desktop?)
        (let [screen-width (/ (.getWidth (Gdx/graphics)) scale-factor)
              screen-height (/ (.getHeight (Gdx/graphics)) scale-factor)]
          (update-rect screen-width screen-height)))
      true)
    (touchDragged
      [^Integer screen-x ^Integer screen-y ^Integer pointer]
      (when (is-desktop?)
        (.translate
         ^OrthographicCamera @camera
         (.getDeltaX (Gdx/input))
         (.getDeltaY (Gdx/input))))
      true)))


(defn -create
  [^ApplicationAdapter _]
  (.setInputProcessor (Gdx/input) input-processor)
  (Texture/setAssetManager assets/manager)
  (.finishLoading assets/manager)
  (reset!
   h3m-map
   (-> Gdx/files
       (.internal "maps/arr.h3m")
       (.read)
       (h3m/parse-file)))
  (reset! batch (new SpriteBatch))
  (reset! camera (create-camera scale-factor))
  (reset! terrain-renderer (terrain/create-renderer @h3m-map))
  (reset! visible-objects (objects/get-visible-objects nil @h3m-map))
  (comment
    (add-watch rect :watcher #(rect-watcher %4))
    (.scheduleTask
     (new Timer)
     (proxy [Timer$Task] []
       (run [] (update-rect screen-width screen-height)))
     (float 0)
     (float update-rect-interval))))


(comment
  (repl
   (reset! terrain-renderer (terrain/create-renderer @h3m-map))))
       

(defn -render
  [^ApplicationAdapter _]
  (doto Gdx/gl
    (.glClearColor 0 0 0 0)
    (.glEnable GL20/GL_BLEND)
    (.glBlendFunc GL20/GL_SRC_ALPHA GL20/GL_ONE_MINUS_SRC_ALPHA)
    (.glClear GL20/GL_COLOR_BUFFER_BIT))
  (.update @camera)
  (doto @terrain-renderer
    (.setView @camera)
    (.render))
  (doto @batch
    (.setTransformMatrix (.-view @camera))
    (.setProjectionMatrix (.-projection @camera))
    (.enableBlending)
    (.begin)
    (objects/render-objects @visible-objects)
    (.end)))

