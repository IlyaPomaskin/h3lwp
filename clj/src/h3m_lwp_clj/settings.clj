(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.scenes.scene2d Stage InputEvent]
   [com.badlogic.gdx.scenes.scene2d.ui Skin Label TextButton VerticalGroup]
   [com.badlogic.gdx.scenes.scene2d.utils ClickListener]
   [com.badlogic.gdx.utils.viewport Viewport]
   [com.badlogic.gdx.utils Align]))


(def ^String instruction
  "To use this app you must provide files from your copy of Heroes of Might and Magic 3: Shadow of the Death")
(def ^String gog-url "https://www.gog.com/game/heroes_of_might_and_magic_3_complete_edition")
(def ^String open-gog-text
  "Buy game on GOG.com")
(def ^String select-button-text "Select h3sprite.lod")


(defn in-progress?
  [{progress-bar-length :progress-bar-length
    progress-bar-value :progress-bar-value}]
  (and
   (pos? progress-bar-length)
   (not= progress-bar-length progress-bar-value)))


(defn progress-percents
  [{progress-bar-length :progress-bar-length
    progress-bar-value :progress-bar-value}]
  (int
   (* (/ progress-bar-value
         progress-bar-length)
      100)))


(defn set-settings-handler
  [settings-atom
   ^VerticalGroup buttons-group
   ^TextButton select-button
   ^Label progress-label]
  (remove-watch settings-atom :settings-change)
  (add-watch
   settings-atom
   :settings-change
   (fn [_ _ prev-settings next-settings]
     (let [prev-in-progress? (in-progress? prev-settings)
           next-in-progress? (in-progress? next-settings)
           start? (and (not prev-in-progress?) next-in-progress?)
           done? (and prev-in-progress? (not next-in-progress?))]
       (when start?
         (.swapActor buttons-group select-button progress-label)
         (.setVisible select-button false))
       (when next-in-progress?
         (.setText
          progress-label
          (format "Parsing: %d%%" (progress-percents next-settings))))
       (when done?
         (.setText progress-label "Loading..."))))))


(defn create-renderer
  [settings-atom ^Viewport viewport]
  (let [{on-file-select-click :on-file-select-click} @settings-atom
        stage (new Stage viewport)
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

        select-button
        (doto (new TextButton select-button-text skin "default")
          (.addListener on-click-listener))

        progress-label
        (doto (new Label "" skin)
          (.setWrap false)
          (.setAlignment Align/center))

        instructions-group
        (doto (new VerticalGroup)
          (.grow)
          (.pad 20)
          (.addActor instructions-label))

        buttons-group
        (doto (new VerticalGroup)
          (.space 10)
          (.addActor gog-button)
          (.addActor select-button)
          (.addActor progress-label))]
    (set-settings-handler
     settings-atom
     buttons-group
     select-button
     progress-label)
    (.addActor
     stage
     (doto (new VerticalGroup)
       (.setFillParent true)
       (.center)
       (.grow)
       (.addActor instructions-group)
       (.addActor buttons-group)))
    (.setInputProcessor (Gdx/input) stage)
    (fn []
      (.apply viewport true)
      (doto stage
        (.act (min (.getDeltaTime Gdx/graphics) (float (/ 1 30))))
        (.draw)))))