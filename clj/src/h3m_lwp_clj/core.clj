(ns h3m-lwp-clj.core
  (:import
   [com.badlogic.gdx ApplicationAdapter Gdx]
   [com.badlogic.gdx.graphics GL20 OrthographicCamera]
   [com.badlogic.gdx.utils Timer Timer$Task])
  (:require
   [h3m-parser.core :as h3m-parser]
   [h3m-lwp-clj.settings :as settings]
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.parser :as parser]
   [h3m-lwp-clj.assets :as assets]
   [h3m-lwp-clj.terrain :as terrain]
   [h3m-lwp-clj.objects :as objects]
   [h3m-lwp-clj.orth-camera :as orth-camera]
   [h3m-lwp-clj.input-processor :as input-processor])
  (:gen-class
   :name h3m.LwpCore
   :extends com.badlogic.gdx.ApplicationAdapter
   :methods [[onFileSelectClick [Runnable] void]
             [setFilePath [String] void]]))


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
(defonce settings-renderer (atom nil))
(defonce on-file-select-click-fn (atom (fn [] (println "UNSET FN"))))
(defonce selected-file-path (atom ""))


(defn -create
  [^ApplicationAdapter _]
  #_(time
     (parser/parse-map-sprites
      (.internal Gdx/files "data/H3sprite.lod")
      (.local Gdx/files consts/atlas-file-name)
      (.local Gdx/files consts/edn-file-name)))
  (assets/init)
  (reset! h3m-map (h3m-parser/parse-h3m (.read (.internal Gdx/files "maps/invasion.h3m"))))
  (reset! camera (orth-camera/create scale-factor))
  (reset! terrain-renderer (terrain/create-renderer @h3m-map))
  (reset! objects-renderer (objects/create-renderer @h3m-map))
  (reset! settings-renderer (settings/create-renderer on-file-select-click-fn selected-file-path))
  (.setContinuousRendering (Gdx/graphics) false)
  #_(.setInputProcessor (Gdx/input) (input-processor/create @camera (:size @h3m-map)))
  (.scheduleTask
   (new Timer)
   (proxy [Timer$Task] []
     (run [] (.requestRendering (Gdx/graphics))))
   (float 0)
   (float consts/animation-interval))
  (.scheduleTask
   (new Timer)
   (proxy [Timer$Task] []
     (run [] (orth-camera/set-random-position @camera (:size @h3m-map))))
   (float 0)
   (float camera-position-update-interval)))


(defn -onFileSelectClick
  [^ApplicationAdapter _ ^Runnable callback]
  (reset! on-file-select-click-fn (fn [] (.run callback))))


(defn -setFilePath
  [^ApplicationAdapter _ ^String path]
  (reset! selected-file-path path))


(defn -render
  [^ApplicationAdapter _]
  (let [^OrthographicCamera camera (deref camera)
        ;terrain-renderer (deref terrain-renderer)
        ;objects-renderer (deref objects-renderer)
        ]
    (doto Gdx/gl
      (.glClearColor 0 0 0 0)
      (.glClear GL20/GL_COLOR_BUFFER_BIT))
    (.update camera)
    ;(terrain-renderer camera)
    ;(objects-renderer camera)
    (@settings-renderer)))
