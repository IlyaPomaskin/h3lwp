package com.homm3.livewallpaper.android

import android.content.*
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.core.Constants.Companion.SCROLL_OFFSET
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.PREFERENCES_NAME
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.USE_SCROLL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.USE_SCROLL_DEFAULT
import com.homm3.livewallpaper.core.Engine as CoreEngine
import java.lang.Exception

class LiveWallpaperService : AndroidLiveWallpaperService() {
    companion object {
        const val PARSING_DONE_MESSAGE = "parsingDone"
    }

    private var engine: CoreEngine? = null
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.hasExtra(PARSING_DONE_MESSAGE) == true) {
                engine?.updateVisibleScreen()
            }
        }
    }

    override fun onCreateEngine(): Engine {
        return AndroidWallpaperEngine()
    }

    override fun onCreateApplication() {
        super.onCreateApplication()

        registerReceiver(receiver, IntentFilter(packageName))

        engine = object : CoreEngine(), AndroidWallpaperListener {
            override var onSettingsButtonClick = {
                startActivity(
                    Intent()
                        .setClass(baseContext, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            var useScroll = USE_SCROLL_DEFAULT

            private fun getUseScrollPreference(): Boolean {
                return getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .runCatching { getBoolean(USE_SCROLL, USE_SCROLL_DEFAULT) }
                    .getOrDefault(USE_SCROLL_DEFAULT)
            }

            override fun create() {
                super.create()
                useScroll = getUseScrollPreference()
            }

            override fun resume() {
                super.resume()
                useScroll = getUseScrollPreference()
            }

            override fun previewStateChange(isPreview: Boolean) {}
            override fun iconDropped(x: Int, y: Int) {}
            override fun offsetChange(xOffset: Float, yOffset: Float,
                                      xOffsetStep: Float, yOffsetStep: Float,
                                      xPixelOffset: Int, yPixelOffset: Int) {
                if (useScroll) {
                    camera.position.x = cameraPoint.x + xOffset * SCROLL_OFFSET
                    camera.position.y = cameraPoint.y + yOffset * SCROLL_OFFSET
                }
            }
        }

        initialize(
            engine,
            AndroidApplicationConfiguration().apply {
                useAccelerometer = false
                useCompass = false
            }
        )
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }
}