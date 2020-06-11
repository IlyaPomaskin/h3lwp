package com.homm3.livewallpaper.android

import android.content.*
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.homm3.livewallpaper.core.Constants
import java.lang.Exception

class LiveWallpaperService : AndroidLiveWallpaperService() {
    override fun onCreateEngine(): Engine {
        return AndroidWallpaperEngine()
    }

    override fun onCreateApplication() {
        super.onCreateApplication()

        SettingsActivity.convertOldPreferences(getSharedPreferences(Constants.Preferences.PREFERENCES_NAME, Context.MODE_PRIVATE))

        initialize(
            AndroidEngine(baseContext),
            AndroidApplicationConfiguration().apply {
                useAccelerometer = false
                useCompass = false
                disableAudio = true
            }
        )
    }
}