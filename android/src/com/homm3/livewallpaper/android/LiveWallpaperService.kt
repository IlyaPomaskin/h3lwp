package com.homm3.livewallpaper.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService

class LiveWallpaperService : AndroidLiveWallpaperService() {
    lateinit var engine: com.homm3.livewallpaper.core.Engine
    var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.hasExtra("parsingDone") == true) {
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