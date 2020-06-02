package com.homm3.livewallpaper.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.core.Constants
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperScreen
import java.lang.Exception

class AndroidEngine(private val context: Context) : Engine(), AndroidWallpaperListener {
    companion object {
        const val PARSING_DONE_MESSAGE = "parsingDone"
    }

    private var parsingDoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.hasExtra(PARSING_DONE_MESSAGE) == true) {
                updateVisibleScreen()
            }
        }
    }
    private var useScroll = Constants.Preferences.USE_SCROLL_DEFAULT

    private fun getUseScrollPreference(): Boolean {
        return context.getSharedPreferences(Constants.Preferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .runCatching { getBoolean(Constants.Preferences.USE_SCROLL, Constants.Preferences.USE_SCROLL_DEFAULT) }
            .getOrDefault(Constants.Preferences.USE_SCROLL_DEFAULT)
    }

    override fun onSettingsButtonClick() {
        context.startActivity(
            Intent()
                .setClass(context, SettingsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun create() {
        super.create()
        useScroll = getUseScrollPreference()
        context.registerReceiver(parsingDoneReceiver, IntentFilter(context.packageName))
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
        if (useScroll && screens.containsKey(WallpaperScreen::class.java)) {
            getScreen<WallpaperScreen>().camera.position.x = cameraPoint.x + xOffset * Constants.SCROLL_OFFSET
        }
    }

    override fun dispose() {
        super.dispose()
        try {
            context.unregisterReceiver(parsingDoneReceiver)
        } catch (ex: Exception) {
        }
    }
}