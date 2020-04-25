package com.heroes3.livewallpaper.android

import android.content.Intent
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService

class LiveWallpaper : AndroidLiveWallpaperService() {
    override fun onCreateEngine(): Engine {
        return AndroidWallpaperEngine()
    }

    override fun onCreateApplication() {
        super.onCreateApplication()

        val config = AndroidApplicationConfiguration()
        config.useAccelerometer = false
        config.useCompass = false
        val engine = com.heroes3.livewallpaper.core.Engine()
        engine.onSettingButtonClick = {
            startActivity(
                Intent()
                    .setClass(this, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        initialize(engine, config)
    }
}