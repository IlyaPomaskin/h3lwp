(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [com.badlogic.gdx.graphics Color]
   [com.badlogic.gdx.scenes.scene2d Stage Touchable InputEvent]
   [com.badlogic.gdx.scenes.scene2d.ui Slider Skin Label Table TextButton ProgressBar ProgressBar$ProgressBarStyle]
   [com.badlogic.gdx.scenes.scene2d.utils ClickListener ChangeListener ChangeListener$ChangeEvent]
   [com.badlogic.gdx.utils Align]))


(def ^String instruction
  "To use this app you need h3sprite.lod file")


(defn create-renderer
  [settings]
  (let [{on-scale-change :on-scale-change
         on-file-select-click :on-file-select-click
         scale :scale} @settings
        stage (new Stage (new ScreenViewport))
        skin (new Skin (.internal Gdx/files "sprites/skin/uiskin.json"))

        on-click-listener
        (proxy [ClickListener] []
          (clicked
            [^InputEvent event ^Float x ^Float y]
            (on-file-select-click)))

        button
        (doto (new TextButton "Select file" skin "default")
          (.addListener on-click-listener))

        progress-bar-style
        (new
         ProgressBar$ProgressBarStyle
         (.newDrawable skin "default-slider" Color/BLACK)
         (.newDrawable skin "default-slider" Color/WHITE))
        _ (set!
           (.-knobBefore progress-bar-style)
           (.newDrawable skin "default-slider" Color/WHITE))

        progress-bar
        (doto (new ProgressBar (float 0) (float 0) (float 1) false progress-bar-style)
          (.setAnimateDuration (float 1))
          (.setVisible false))

        scale-slider
        (doto (new Slider (float 0.5) (float 1.0) (float 0.5) false skin)
          (.setValue scale)
          (.addListener
           (proxy [ChangeListener] []
             (changed
               [^ChangeListener$ChangeEvent event ^Slider actor]
               (on-scale-change (.getValue actor))))))]
    (add-watch
     settings
     :settings-change
     (fn [_ _ prev-settings next-settings]
       (let [{progress-bar-length :progress-bar-length
              progress-bar-value :progress-bar-value
              scale :scale} next-settings
             prev-progress-bar-value (:progress-bar-value prev-settings)
             in-progress? (and
                           (pos? progress-bar-length)
                           (not= progress-bar-length progress-bar-value))
             done? (and
                    (pos? progress-bar-length)
                    (= progress-bar-length progress-bar-value)
                    (not= prev-progress-bar-value progress-bar-value))]
         (doto progress-bar
           (.setRange (float 0) (float progress-bar-length))
           (.setValue (float progress-bar-value))
           (.setVisible true))
         (doto button
           (.setText (if in-progress? "Parsing..." "Select file"))
           (.setTouchable (if in-progress? Touchable/disabled Touchable/enabled))
           (.setDisabled in-progress?))
         (when done?
           (.setVisible progress-bar false)
           (doto button
             (.setText "Done!")
             (.setTouchable Touchable/disabled)
             (.setDisabled true)))
         (.setValue scale-slider scale))))
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
       (.add
        (doto (new Label instruction skin)
          (.setWrap true)
          (.setAlignment Align/center)))
       (.row)
       (.add button)
       (.row)
       (.add progress-bar)
       (.row)
       (.add
        (doto (new Label "Select scale" skin)
          (.setWrap true)
          (.setAlignment Align/center)))
       (.row)
       (.add scale-slider)
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