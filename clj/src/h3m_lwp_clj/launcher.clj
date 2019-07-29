; (ns h3m-lwp-clj.launcher
;   (:import [com.badlogic.gdx.backends.android AndroidApplication]
;            [com.badlogic.gdx.backends.android AndroidApplicationConfiguration]
;            [h3m Core]))


; (gen-class
;  :name h3m.Launcher
;  :extends com.badlogic.gdx.backends.android.AndroidApplication
;  :exposes-methods {onCreate parentOnCreate
;                    initialize parentInitialize})


; (defn -onCreate
;   [^AndroidApplication this savedInstanceState]
;   (doto this
;     (.parentOnCreate savedInstanceState)
;     (.parentInitialize (new Core) (new AndroidApplicationConfiguration))))
