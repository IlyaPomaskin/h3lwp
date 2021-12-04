package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.core.Constants
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.GameScreen

class AndroidEngine(private val context: Context) : Engine(), AndroidWallpaperListener {
    private var useScroll = Constants.Preferences.USE_SCROLL_DEFAULT
    private val preferences = context.getSharedPreferences(Constants.Preferences.PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun getUseScrollPreference(): Boolean {
        return preferences
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
        if (useScroll && screens.containsKey(GameScreen::class.java)) {
            getScreen<GameScreen>().moveCameraByOffset(xOffset);
        }
    }
}