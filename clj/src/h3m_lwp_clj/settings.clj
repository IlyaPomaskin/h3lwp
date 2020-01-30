(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [com.badlogic.gdx.scenes.scene2d Stage Actor]
   [com.badlogic.gdx.scenes.scene2d.ui
    Skin Label Table TextButton ProgressBar]
   [com.badlogic.gdx.scenes.scene2d.utils ChangeListener ChangeListener$ChangeEvent]
   [com.badlogic.gdx.utils Align]
   [java.io FileInputStream])
  (:require
   [h3m-lwp-clj.protocol :as protocol]
   [h3m-lwp-clj.consts :as consts]
   [h3m-lwp-clj.parser :as parser]))


(def ^String instruction
  "To use this app you need h3sprite.lod file")


(defn create-renderer
  [on-file-select-click-fn selected-file-path]
  (let [stage (new Stage (new ScreenViewport))
        skin (new Skin (.internal Gdx/files "sprites/skin/uiskin.json"))
        label (doto (new Label instruction skin)
                (.setWrap true)
                (.setAlignment Align/center))
        on-click-listener (proxy [ChangeListener] []
                            (changed
                              [^ChangeListener$ChangeEvent event ^Actor actor]
                              (@on-file-select-click-fn)))
        button (doto (new TextButton "Select file" skin "default")
                 (.addListener on-click-listener))
        file-path-label (doto (new Label "path:" skin)
                          (.setWrap true)
                          (.setAlignment Align/center))
        progress-bar (doto (new ProgressBar (float 0) (float 100) (float 1) false skin)
                       (.setAnimateDuration (float 1))
                       (.setVisible false)
                       (.setWidth 500))
        set-file-path-text
        (fn [path]
          (.setText file-path-label (format "path: %s" path))
          (.setDisabled button true)
          (let [lod-file (new FileInputStream (.file (.absolute Gdx/files path)))
                list (parser/get-lod-files-list lod-file)]
            (doto progress-bar
              (.setRange (float 0) (float (count list)))
              (.setValue (float 0))
              (.setVisible true))
            (future
              (parser/parse-map-sprites
               list
               lod-file
               (.local Gdx/files consts/atlas-file-name)
               (.local Gdx/files consts/edn-file-name)
               (fn [_] (.setValue progress-bar (inc (.getValue progress-bar)))))
              (.setDisabled button false))
            true))]
    (.addActor
     stage
     (doto (new Table skin)
       (.debug)
       (as-> t
             (-> t
                 (.defaults)
                 (.spaceBottom (float 10))))
       (as-> t
             (-> t
                 (.row)
                 (.fill)
                 (.expandX)))
       (.add label)
       (.row)
       (.add button)
       (.row)
       (.add file-path-label)
       (.row)
       (.add progress-bar)
       (.row)
       (.pack)
       (.setWidth 300)
       (.setHeight 300)
       (as-> t
             (.setPosition
              t
              (float (- (/ (.getWidth stage) 2)
                        (/ (.getWidth t) 2)))
              (float (- (/ (.getHeight stage) 2)
                        (/ (.getHeight t) 2)))))))
    (reify protocol/Renderer
      (start
        [this]
        (.setInputProcessor (Gdx/input) stage)
        (add-watch selected-file-path :settings-update #(set-file-path-text %4)))
      (stop
        [this]
        (remove-watch selected-file-path :settings-update))
      (render
        [this]
        (doto stage
          (.act (min (.getDeltaTime Gdx/graphics) (float (/ 1 30))))
          (.draw))))))