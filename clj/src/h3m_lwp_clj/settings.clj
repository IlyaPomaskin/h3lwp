(ns h3m-lwp-clj.settings
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.scenes.scene2d Stage InputEvent Touchable]
   [com.badlogic.gdx.scenes.scene2d.ui Skin Label TextButton VerticalGroup TextButton$TextButtonStyle ScrollPane]
   [com.badlogic.gdx.scenes.scene2d.utils ClickListener]
   [com.badlogic.gdx.utils.viewport Viewport]
   [com.badlogic.gdx.utils Align]
   (com.badlogic.gdx.graphics Color)))


(def ^String instruction
  "To use this app you must provide files from your copy of \"Heroes of Might and Magic 3: Shadow of the Death\"")
(def ^String permission-text
  "Allow storage permissions in Android settings")
(def ^String instructions-url "https://github.com/IlyaPomaskin/h3lwp")
(def ^String select-button-text "Select h3sprite.lod")
(def ^String permission-button-text "Open app settings")


(defn create-disabled-button-style-
  [^Skin skin]
  (let [default-button-style (.getStyle (new TextButton "" skin "default"))
        disabled-button-style (new TextButton$TextButtonStyle default-button-style)
        _ (set!
           (.-up disabled-button-style)
           (.newDrawable skin "default-round" Color/LIGHT_GRAY))]
    disabled-button-style))
(def create-disabled-button-style (memoize create-disabled-button-style-))


(defn create-pressed-button-style-
  [^Skin skin]
  (let [default-button-style (.getStyle (new TextButton "" skin "default"))
        pressed-button-style (new TextButton$TextButtonStyle default-button-style)
        _ (set!
           (.-up pressed-button-style)
           (.-down pressed-button-style))]
    pressed-button-style))
(def create-pressed-button-style (memoize create-pressed-button-style-))


(defn create-default-button-style-
  [^Skin skin]
  (.getStyle (new TextButton "" skin "default")))
(def create-default-button-style (memoize create-default-button-style-))


(defn settings-handler
  [{^Skin skin :skin
    ^TextButton select-button :select-button
    ^TextButton permission-button :permission-button
    ^Label label :label} settings prev-settings]
  (let [{state :state
         error :error} settings]

    (when (or (nil? prev-settings)
              (not= (:state prev-settings)
                    (:state settings)))
      (doto select-button
        (.setStyle (create-default-button-style skin))
        (.setText select-button-text)
        (.setTouchable Touchable/enabled))
      (.setText label ""))

    (println "state" state)
    (println "error" error)

    (condp = state
      :ready
      (doto select-button
        (.setStyle (create-default-button-style skin))
        (.setTouchable Touchable/enabled))

      :no-storage-permission
      (do
        (doto select-button
          (.setStyle (create-disabled-button-style skin))
          (.setTouchable Touchable/disabled))
        (.setVisible permission-button true)
        (doto label
          (.setText permission-text)
          (.setVisible true)))

      :parsing
      (doto select-button
        (.setStyle (create-pressed-button-style skin))
        (.setTouchable Touchable/disabled)
        (.setText "Parsing..."))

      :loading
      (doto select-button
        (.setStyle (create-pressed-button-style skin))
        (.setTouchable Touchable/disabled)
        (.setText "Loading..."))

      :error
      (.setText label (format "Something went wrong\n%s" (or error ""))))))


(defn set-settings-handler
  [widgets settings-atom]
  (settings-handler widgets @settings-atom nil)
  (remove-watch settings-atom :settings-change)
  (add-watch settings-atom :settings-change
             (fn [_ _ prev-settings next-settings]
               (settings-handler widgets next-settings prev-settings))))


(defn create-renderer
  [settings-atom ^Viewport viewport]
  (let [{on-file-select-click :on-file-select-click
         on-permission-click :on-permission-click} @settings-atom
        stage (new Stage viewport)
        skin (new Skin (.internal Gdx/files "sprites/skin/uiskin.json"))

        instructions-label
        (doto (new Label instruction skin)
          (.setWrap true)
          (.setAlignment Align/center))

        full-instructions-click-handler
        (proxy [ClickListener] []
          (clicked
            [^InputEvent event ^Float x ^Float y]
            (.openURI Gdx/net instructions-url)))

        full-instructions-button
        (doto (new TextButton "Open instructions" skin "default")
          (.addListener full-instructions-click-handler))

        on-click-listener
        (proxy [ClickListener] []
          (clicked
            [^InputEvent event ^Float x ^Float y]
            (on-file-select-click)))

        select-button
        (doto (new TextButton select-button-text skin "default")
          (.addListener on-click-listener))

        on-permission-click-listener
        (proxy [ClickListener] []
          (clicked
            [^InputEvent event ^Float x ^Float y]
            (on-permission-click)))

        permission-button
        (doto (new TextButton permission-button-text skin "default")
          (.setVisible false)
          (.addListener on-permission-click-listener))

        status-label
        (doto (new Label "" skin)
          (.setWrap true)
          (.setAlignment Align/center))

        instructions-group
        (doto (new VerticalGroup)
          (.grow)
          (.pad 20)
          (.addActor instructions-label))

        buttons-group
        (doto (new VerticalGroup)
          (.space 10)
          (.addActor full-instructions-button)
          (.addActor select-button)
          (.addActor status-label)
          (.addActor permission-button))

        groups
        (doto (new VerticalGroup)
          (.grow)
          (.center)
          (.addActor instructions-group)
          (.addActor buttons-group))]
    (set-settings-handler
      {:skin skin
       :select-button select-button
       :permission-button permission-button
       :label status-label}
      settings-atom)
    (.addActor
      stage
      (doto (new ScrollPane groups)
        (.setFillParent true)))
    (.setInputProcessor (Gdx/input) stage)
    (fn []
      (.apply viewport true)
      (doto stage
        (.act (min (.getDeltaTime Gdx/graphics) (float (/ 1 30))))
        (.draw)))))