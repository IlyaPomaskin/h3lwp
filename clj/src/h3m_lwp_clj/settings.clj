(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils.viewport ScreenViewport]
   [com.badlogic.gdx.graphics Color]
   [com.badlogic.gdx.scenes.scene2d Stage Touchable InputEvent]
   [com.badlogic.gdx.scenes.scene2d.ui Skin Label Table TextButton ProgressBar ProgressBar$ProgressBarStyle]
   [com.badlogic.gdx.scenes.scene2d.utils ClickListener]
   [com.badlogic.gdx.utils Align]))


(def ^String instruction
  "To use this app you must provide files from your copy of the game
  The only supported version is Heroes of Might and Magic 3: Shadow of the Death")
(def ^String gog-url "https://www.gog.com/game/heroes_of_might_and_magic_3_complete_edition")
(def ^String buy-at-gog
  "If you dont have a copy
   buy at GOG.com")
(def ^String open-gog-text
  "Open GOG.com")
(def ^String select-button-text "Select h3sprite.lod")


(defn set-settings-handler
  [settings-atom ^TextButton button ^ProgressBar progress-bar]
  (add-watch
   settings-atom
   :settings-change
   (fn [_ _ prev-settings next-settings]
     (let [{progress-bar-length :progress-bar-length
            progress-bar-value :progress-bar-value} next-settings
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
         (.setText (if in-progress? "Parsing..." select-button-text))
         (.setTouchable (if in-progress? Touchable/disabled Touchable/enabled))
         (.setDisabled in-progress?))
       (when done?
         (.setVisible progress-bar false)
         (doto button
           (.setText "Done!")
           (.setTouchable Touchable/disabled)
           (.setDisabled true)))))))


(defn create-renderer
  [settings]
  (let [{on-file-select-click :on-file-select-click} @settings
        stage (new Stage (new ScreenViewport))
        skin (new Skin (.internal Gdx/files "sprites/skin/uiskin.json"))

        instructions-label
        (doto (new Label instruction skin)
          (.setWrap true)
          (.setAlignment Align/center))

        gog-click-handler
        (proxy [ClickListener] []
          (clicked
            [^InputEvent event ^Float x ^Float y]
            (.openURI Gdx/net gog-url)))

        gog-button
        (doto (new TextButton "Open GOG.com" skin "default")
          (.addListener gog-click-handler))

        on-click-listener
        (proxy [ClickListener] []
          (clicked
            [^InputEvent event ^Float x ^Float y]
            (on-file-select-click)))

        button
        (doto (new TextButton select-button-text skin "default")
          (.addListener on-click-listener))

        progress-bar-style
        (new
         ProgressBar$ProgressBarStyle
         (.newDrawable skin "default-slider" (new Color))
         (.newDrawable skin "default-slider" (new Color)))
        _ (set!
           (.-knobBefore progress-bar-style)
           (.newDrawable skin "default-slider" Color/WHITE))

        progress-bar
        (doto (new ProgressBar (float 0) (float 0) (float 1) false progress-bar-style)
          (.setAnimateDuration (float 1))
          (.setVisible false))]
    (set-settings-handler settings button progress-bar)
    (.addActor
     stage
     (doto (new Table skin)
       (.debug)
       (as-> t
             (-> t
                 (.defaults)
                 (.spaceBottom (float 10))
                 (.padLeft (float 30))
                 (.padRight (float 30))))
       (as-> t
             (-> t
                 (.row)
                 (.fill)
                 (.expandX)))
       (.add instructions-label)
       (.row)
       (.add gog-button)
       (.row)
       (.add button)
       (as-> t
             (-> t
                 (.row)
                 (.fill)
                ;;  (.expandX)
                 (.height (float 30))))
       (.add progress-bar)
       (.row)
       (.pack)
       (.setFillParent true)))
    (fn []
      (doto stage
        (.act (min (.getDeltaTime Gdx/graphics) (float (/ 1 30))))
        (.draw)))))