(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [com.badlogic.gdx.scenes.scene2d Stage Actor]
   [com.badlogic.gdx.scenes.scene2d.ui Skin Label Table TextButton]
   [com.badlogic.gdx.scenes.scene2d.utils ChangeListener ChangeListener$ChangeEvent]
   [com.badlogic.gdx.utils Align]))


(def ^String instruction
  "To use this app you need h3sprite.lod file")


(defn create-renderer
  [on-file-select-click-fn selected-file-path is-preview]
  (let [stage (new Stage (new ScreenViewport))
        skin (new Skin (.internal Gdx/files "sprites/skin/uiskin.json"))
        label (doto (new Label instruction skin)
                (.setWrap true)
                (.setAlignment Align/center))
        file-path-label (doto (new Label "path:" skin)
                          (.setWrap true)
                          (.setAlignment Align/center))
        is-preview-label (doto (new Label (format "is preview: %b" @is-preview) skin)
                           (.setWrap true)
                           (.setAlignment Align/center))
        on-click-listener (proxy [ChangeListener] []
                            (changed
                              [^ChangeListener$ChangeEvent event ^Actor actor]
                              (println "CLICKED11")
                              (@on-file-select-click-fn)))
        button (doto (new TextButton "Select file" skin "default")
                 (.addListener on-click-listener))
        table (doto (new Table skin)
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
                (.add is-preview-label)
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
                                 (/ (.getHeight t) 2))))))]
    (.addActor stage table)
    (.setInputProcessor (Gdx/input) stage)
    (add-watch
     selected-file-path
     :watcher
     (fn [_ _ _ new-state]
       (.setText file-path-label (format "path: %s" new-state))))
    (add-watch
     is-preview
     :watcher
     (fn [_ _ _ new-state]
       (.setText is-preview-label (format "is preview: %b" new-state))))
    (fn render-stage []
      (doto stage
        (.act (min (.getDeltaTime Gdx/graphics) (float (/ 1 30))))
        (.draw)))))