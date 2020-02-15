(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [com.badlogic.gdx.scenes.scene2d Stage Touchable InputEvent]
   [com.badlogic.gdx.scenes.scene2d.ui Skin Label Table TextButton ProgressBar]
   [com.badlogic.gdx.scenes.scene2d.utils ClickListener]
   [com.badlogic.gdx.utils Align]))


(def ^String instruction
  "To use this app you need h3sprite.lod file")


(defn create-renderer
  [state]
  (let [stage (new Stage (new ScreenViewport))
        skin (new Skin (.internal Gdx/files "sprites/skin/uiskin.json"))
        label (doto (new Label instruction skin)
                (.setWrap true)
                (.setAlignment Align/center))
        on-click-listener (proxy [ClickListener] []
                            (clicked
                              [^InputEvent event ^Float x ^Float y]
                              (let [callback (:on-file-select-click @state)]
                                (when (fn? callback)
                                  (callback)))))
        button (doto (new TextButton "Select file" skin "default")
                 (.addListener on-click-listener))
        progress-bar (doto (new ProgressBar (float 0) (float 0) (float 1) false skin)
                       (.setAnimateDuration (float 1))
                       (.setVisible false)
                       (.setWidth 500))]
    (add-watch
     state
     :state-change
     (fn [_ _ _ next-value]
       (let [progress-bar-length (:progress-bar-length next-value)
             progress-bar-value (:progress-bar-value next-value)
             in-progress? (not= progress-bar-length progress-bar-value)
             done? (and (not= 0 progress-bar-length) (not in-progress?))]
         (println "UPDATE" progress-bar-length progress-bar-value)
         (doto progress-bar
           (.setRange (float 0) (float progress-bar-length))
           (.setValue (float progress-bar-value))
           (.setVisible true))
         (doto button
           (.setText (if in-progress? "Parsing..." "Select file"))
           (.setTouchable (if in-progress? Touchable/disabled Touchable/enabled))
           (.setDisabled in-progress?)
           (.setChecked in-progress?)))))
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
    (fn []
      (doto stage
        (.act (min (.getDeltaTime Gdx/graphics) (float (/ 1 30))))
        (.draw)))))