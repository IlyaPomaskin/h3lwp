; (ns h3m-lwp-clj.service
;   (:import [com.badlogic.gdx ApplicationListener]
;            [com.badlogic.gdx.backends.android AndroidApplicationConfiguration]
;            [com.badlogic.gdx.backends.android AndroidLiveWallpaperService]
;            [h3m Core]))


; (gen-class
;  :name h3m.LiveWallpaperService
;  :extends com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
;  :exposes-methods {onCreateApplication parentOnCreateApplication
;                    initialize parentInitialize})


; (defn onCreateApplication
;   [^AndroidLiveWallpaperService this]
;   (doto this
;     (.parentOnCreateApplication)
;     (.parentInitialize (new Core))))
