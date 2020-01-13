(ns h3m-lwp-clj.input-processor
  (:import
   [com.badlogic.gdx Gdx InputProcessor Application$ApplicationType]
   [com.badlogic.gdx.graphics OrthographicCamera]
   [com.badlogic.gdx Input$Keys])
  (:require
   [h3m-lwp-clj.consts :as consts]))


(defn is-desktop?
  []
  (= (.getType (Gdx/app))
     (Application$ApplicationType/Desktop)))


(defn pressed?
  [key _]
  (.isKeyPressed (Gdx/input) key))


(defn create
  [^OrthographicCamera camera]
  (proxy [InputProcessor] []
    (keyDown [keycode] true)
    (keyTyped
      [keycode]
      (when (is-desktop?)
        (condp pressed? keycode
          Input$Keys/NUM_0 (set! (.-zoom camera) 1)
          Input$Keys/EQUALS (set! (.-zoom camera) (- (.-zoom camera) 0.1))
          Input$Keys/MINUS (set! (.-zoom camera) (+ (.-zoom camera) 0.1))
          Input$Keys/UP (.translate camera 0 (- consts/tile-size) 0)
          Input$Keys/DOWN (.translate camera 0 consts/tile-size 0)
          Input$Keys/LEFT (.translate camera (- consts/tile-size) 0 0)
          Input$Keys/RIGHT (.translate camera consts/tile-size 0 0)
          Input$Keys/SPACE (set-random-camera-position camera (:size @h3m-map))
          nil))
      true)
    (keyUp [keycode] true)
    (mouseMoved [x y] true)
    (scrolled [amount] true)
    (touchDown [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button] true)
    (touchUp [^Integer screen-x ^Integer screen-y ^Integer pointer ^Integer button] true)
    (touchDragged
      [^Integer screen-x ^Integer screen-y ^Integer pointer]
      (when (is-desktop?)
        (.translate camera (.getDeltaX (Gdx/input)) (.getDeltaY (Gdx/input))))
      true)))