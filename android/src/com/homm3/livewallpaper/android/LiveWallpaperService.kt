package com.homm3.livewallpaper.android

import android.content.*
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService

class LiveWallpaperService : AndroidLiveWallpaperService() {
    companion object {
        const val PARSING_DONE_MESSAGE = "parsingDone"
    }

    lateinit var engine: com.homm3.livewallpaper.core.Engine
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.hasExtra(PARSING_DONE_MESSAGE) == true) {
                engine.updateVisibleScreen()
            }
        }
    }

    override fun onCreateEngine(): Engine {
        return AndroidWallpaperEngine()
    }

    override fun onCreateApplication() {
        super.onCreateApplication()

        engine = com.homm3.livewallpaper.core.Engine().apply {
            onSettingsButtonClick = {
                startActivity(
                    Intent()
                        .setClass(baseContext, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
        }
        registerReceiver(receiver, IntentFilter(packageName))
        initialize(engine, config)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}