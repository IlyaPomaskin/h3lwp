(ns h3m-lwp-clj.core
  (:import [com.badlogic.gdx ApplicationAdapter Game Gdx Graphics Screen]
           [com.badlogic.gdx.graphics Texture Color GL20 OrthographicCamera]
           [com.badlogic.gdx.graphics.g2d BitmapFont SpriteBatch SpriteCache TextureRegion])
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


(def h3m-map (atom nil))
(def batch (atom nil))
(def cache (atom nil))
(def cache-id (atom nil))
(def camera (atom nil))
(def rect (atom {}))
(def terrain-tiles (atom []))
(def objects (atom []))


(defn create-camera
  [screen-width screen-height scale-factor]
  (let [camera (new OrthographicCamera screen-width screen-height)]
    (set! (.-zoom camera) (/ 1.0 scale-factor))
    (.setToOrtho camera true)
    camera))


(defn -create
  [^ApplicationAdapter this]
  (let [scale-factor 2
        screen-width (/ (.getWidth (Gdx/graphics)) scale-factor)
        screen-height (/ (.getHeight (Gdx/graphics)) scale-factor)
        map-file (.read (.internal (Gdx/files) "maps/invasion.h3m"))]
    (reset! h3m-map (h3m/parse-file map-file))
    (swap! h3m-map #(objects/sort-map-objects %))
    (reset! batch (new SpriteBatch))
    (reset! cache (new SpriteCache))
    (reset! camera (create-camera screen-width screen-height scale-factor))
    (add-watch
     rect
     :watcher
     (fn [_ _ _ next-rect]
       (reset! terrain-tiles (terrain/get-visible-tiles next-rect @h3m-map))
       (reset! objects (objects/get-visible-objects next-rect @h3m-map))
       (doto @cache
         (.beginCache)
         (terrain/render-terrain-tiles @terrain-tiles))
       (reset! cache-id (.endCache @cache))
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
       (println @rect)))
    (.finishLoading assets/manager)
    (reset! rect (rect/get-random
                  (:size @h3m-map)
                  screen-width
                  screen-height))))


(defn -render
  [^ApplicationAdapter this]
  (Thread/sleep (long 180))
  (doto Gdx/gl
    (.glClearColor 0 0 0 0)
    (.glEnable GL20/GL_BLEND)
    (.glBlendFunc GL20/GL_SRC_ALPHA GL20/GL_ONE_MINUS_SRC_ALPHA)
    (.glClear GL20/GL_COLOR_BUFFER_BIT))
  (.update @camera)
  (doto @cache
    (.setTransformMatrix (.-view @camera))
    (.setProjectionMatrix (.-projection @camera))
    (.begin)
    (.draw @cache-id)
    (.end))
  (doto @batch
    (.setTransformMatrix (.-view @camera))
    (.setProjectionMatrix (.-projection @camera))
    (.enableBlending)
    (.begin)
    (objects/render-objects @objects)
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

