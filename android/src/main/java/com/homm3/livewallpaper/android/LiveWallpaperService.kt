package com.homm3.livewallpaper.android

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService

class LiveWallpaperService : AndroidLiveWallpaperService() {
    override fun onCreateEngine(): Engine {
        return AndroidWallpaperEngine()
    }

    override fun onCreateApplication() {
        super.onCreateApplication()

        initialize(
            AndroidEngine(this),
            AndroidApplicationConfiguration().apply {
                useAccelerometer = false
                useCompass = false
                disableAudio = true
            }
        )


    }
}