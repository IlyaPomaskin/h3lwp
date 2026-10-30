package com.homm3.livewallpaper.android

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.badlogic.gdx.backends.android.NonBlockingResumeLiveWallpaper
import com.homm3.livewallpaper.android.wallpaper.AndroidEngine

class LiveWallpaperService : AndroidLiveWallpaperService() {
    private var androidEngine: AndroidEngine? = null

    override fun onCreateEngine(): Engine {
        return object : AndroidWallpaperEngine() {
            private var isShown = false

            override fun onVisibilityChanged(visible: Boolean) {
                val isHiding = isShown && !visible
                isShown = visible
                if (isHiding) androidEngine?.prepareForHide()
                super.onVisibilityChanged(visible)
            }

            override fun onZoomChanged(zoom: Float) {
                super.onZoomChanged(zoom)
                val isActiveEngine = linkedEngine === this
                if (!isActiveEngine) return
                app?.postRunnable { androidEngine?.setLauncherZoom(zoom) }
            }
        }
    }

    override fun onCreateApplication() {
        super.onCreateApplication()
        app = NonBlockingResumeLiveWallpaper(this)

        val engine = AndroidEngine(this, appContainer.preferencesRepository.preferences)
        androidEngine = engine

        initialize(
            engine,
            AndroidApplicationConfiguration().apply {
                useAccelerometer = false
                useCompass = false
                disableAudio = true
            }
        )
    }
}
