(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.utils.viewport Viewport]
   [com.badlogic.gdx.scenes.scene2d Stage InputEvent]
   [com.badlogic.gdx.scenes.scene2d.ui Skin Label TextButton VerticalGroup]
   [com.badlogic.gdx.scenes.scene2d.utils ClickListener]
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
  [settings-atom ^TextButton button ^Label progress-label]
  (add-watch
   settings-atom
   :settings-change
   (fn [_ _ prev-settings next-settings]
     (let [prev-in-progress? (in-progress? prev-settings)
           next-in-progress? (in-progress? next-settings)
           done? (and prev-in-progress? (not next-in-progress?))]
       (when next-in-progress?
         (.setVisible button false)
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

        button
        (doto (new TextButton select-button-text skin "default")
          (.addListener on-click-listener))

        progress-label
        (doto (new Label "" skin)
          (.setWrap false)
          (.setAlignment Align/center))]
    (set-settings-handler settings-atom button progress-label)
    (.addActor
     stage
     (doto (new VerticalGroup)
       (.center)
       (.grow)
       (.space 10)
       (.pad 30)
       (.addActor instructions-label)
       (.addActor gog-button)
       (.addActor button)
       (.addActor progress-label)
       (.setFillParent true)))
    (fn []
      (.apply viewport true)
      (doto stage
        (.act (min (.getDeltaTime Gdx/graphics) (float (/ 1 30))))
        (.draw)))))