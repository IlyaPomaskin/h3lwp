(ns h3m-lwp-clj.desktop
  (:import
   [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglApplicationConfiguration]
   [h3m LwpCore]))

(defn -main []
  (let [config (new LwjglApplicationConfiguration)]
    (set! (.-height config) 1136)
    (set! (.-width config) 640)
    (new LwjglApplication (new LwpCore) config)))
