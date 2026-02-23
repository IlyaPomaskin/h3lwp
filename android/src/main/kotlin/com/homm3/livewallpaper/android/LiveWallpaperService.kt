package com.homm3.livewallpaper.android

import android.view.SurfaceHolder
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.data.dataStore

class LiveWallpaperService : AndroidLiveWallpaperService() {
    private var androidEngine: AndroidEngine? = null

    override fun onCreateEngine(): Engine {
        return object : AndroidWallpaperEngine() {
            private var lastWidth = 0
            private var lastHeight = 0
            private var surfaceSizeChanged = false

            override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                if (lastWidth != 0 && (lastWidth != width || lastHeight != height)) {
                    surfaceSizeChanged = true
                }
                lastWidth = width
                lastHeight = height
                super.onSurfaceChanged(holder, format, width, height)
            }

            override fun onVisibilityChanged(visible: Boolean) {
                super.onVisibilityChanged(visible)
                if (visible) {
                    if (!surfaceSizeChanged) {
                        androidEngine?.onVisibilityChanged(true)
                    }
                    surfaceSizeChanged = false
                }
            }
        }
    }

    override fun onCreateApplication() {
        super.onCreateApplication()

        val prefs = WallpaperPreferencesRepository(dataStore)
        val engine = AndroidEngine(this, prefs.preferencesFlow)
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
