package com.homm3.livewallpaper.android

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.data.dataStore

class LiveWallpaperService : AndroidLiveWallpaperService() {
    private var androidEngine: AndroidEngine? = null

    override fun onCreateEngine(): Engine {
        return object : AndroidWallpaperEngine() {
            override fun onVisibilityChanged(visible: Boolean) {
                super.onVisibilityChanged(visible)
                androidEngine?.onVisibilityChanged(visible)
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
