package com.homm3.livewallpaper.android

import android.content.*
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.core.Constants.Companion.SCROLL_OFFSET
import java.lang.Exception

class LiveWallpaperService : AndroidLiveWallpaperService() {
    companion object {
        const val PARSING_DONE_MESSAGE = "parsingDone"
    }

    val engine: com.homm3.livewallpaper.core.Engine = object : com.homm3.livewallpaper.core.Engine(), AndroidWallpaperListener {
        override var onSettingsButtonClick = {
            startActivity(
                Intent()
                    .setClass(baseContext, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        override fun previewStateChange(isPreview: Boolean) {}
        override fun iconDropped(x: Int, y: Int) {}
        override fun offsetChange(xOffset: Float, yOffset: Float,
                                  xOffsetStep: Float, yOffsetStep: Float,
                                  xPixelOffset: Int, yPixelOffset: Int) {
            camera.position.x = cameraPoint.x + xOffset * SCROLL_OFFSET
            camera.position.y = cameraPoint.y + yOffset * SCROLL_OFFSET
        }
    }

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

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
        }
        registerReceiver(receiver, IntentFilter(packageName))
        initialize(engine, config)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }
}