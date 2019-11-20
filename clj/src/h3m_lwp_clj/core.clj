(ns h3m-lwp-clj.core
  (:import [com.badlogic.gdx ApplicationAdapter Gdx]
           [com.badlogic.gdx.graphics Texture GL20 OrthographicCamera]
           [com.badlogic.gdx.graphics.g2d SpriteBatch SpriteCache]
           [com.badlogic.gdx.utils Timer Timer$Task])
  (:require
   [h3m-parser.core :as h3m]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.rect :as rect]
   [h3m-lwp-clj.consts :as consts]))


(gen-class
 :name h3m.LwpCore
 :extends com.badlogic.gdx.ApplicationAdapter)


(def update-rect-interval (* 60 5))
(def sprite-render-interval 180)
(def scale-factor 2)


(def h3m-map (atom nil))
(def batch (atom nil))
(def cache (atom nil))
(def cache-id (atom nil))
(def camera (atom nil))
(def rect (atom {}))
(def visible-objects (atom []))


(defn create-camera
  [screen-width screen-height scale-factor]
  (let [camera (new OrthographicCamera screen-width screen-height)]
    (set! (.-zoom camera) (/ 1.0 scale-factor))
    (.setToOrtho camera true)
    camera))


(defn update-terrain-cache
  [next-rect]
  (doto @cache
    (.beginCache)
    (terrain/render-terrain-tiles
     (terrain/get-visible-tiles next-rect @h3m-map)))
  (reset! cache-id (.endCache @cache)))


(defn rect-watcher
  [next-rect]
  (reset! visible-objects (objects/get-visible-objects next-rect @h3m-map))
  (update-terrain-cache next-rect)
  (.set (.position @camera)
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


(defn -create
  [^ApplicationAdapter this]
  (let [screen-width (/ (.getWidth (Gdx/graphics)) scale-factor)
        screen-height (/ (.getHeight (Gdx/graphics)) scale-factor)]
    (reset! h3m-map (-> Gdx/files
                        (.internal "maps/invasion.h3m")
                        (.read)
                        h3m/parse-file
                        objects/sort-map-objects))
    (reset! batch (new SpriteBatch))
    (reset! cache (new SpriteCache))
    (reset! camera (create-camera screen-width screen-height scale-factor))
    (add-watch rect :watcher #(rect-watcher %4))
    (.finishLoading assets/manager)
    (.scheduleTask
     (new Timer)
     (proxy [Timer$Task] []
       (run []
         (reset!
          rect
          (rect/get-random
           (:size @h3m-map)
           screen-width
           screen-height))))
     (float 0)
     (float update-rect-interval))))


(defn -render
  [^ApplicationAdapter this]
  (Thread/sleep (long sprite-render-interval))
  (doto Gdx/gl
    (.glClearColor 0 0 0 0)
    (.glEnable GL20/GL_BLEND)
    (.glBlendFunc GL20/GL_SRC_ALPHA GL20/GL_ONE_MINUS_SRC_ALPHA)
    (.glClear GL20/GL_COLOR_BUFFER_BIT))
  (.update @camera)
  (when @cache-id
    (doto @cache
      (.setTransformMatrix (.-view @camera))
      (.setProjectionMatrix (.-projection @camera))
      (.begin)
      (.draw @cache-id)
      (.end)))
  (doto @batch
    (.setTransformMatrix (.-view @camera))
    (.setProjectionMatrix (.-projection @camera))
    (.enableBlending)
    (.begin)
    (objects/render-objects @visible-objects)
    (.end)))


(defmacro repl
  "Executes body on main Gdx thread"
  [body]
  `(.postRunnable
    Gdx/app
    (proxy [Runnable] []
      (run []
        (try
          ~body
          (catch Exception e# (do
                                (println (str "repl macro exception: " (.getMessage e#)))
                                (println (.printStackTrace e#)))))))))

