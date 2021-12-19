package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.android.data.WallpaperPreferences
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.data.dataStore
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.screens.GameScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AndroidEngine(private val context: Context) : Engine(), AndroidWallpaperListener {
    private var prefs = WallpaperPreferences()
    private val prefsRepository = WallpaperPreferencesRepository(context.dataStore)

    private fun updatePreferences() {
        CoroutineScope(Dispatchers.Default).launch {
            prefsRepository.preferencesFlow.collect { prefs = it }
        }
    }

    override fun onSettingsButtonClick() {
        context.startActivity(
            Intent()
                .setClass(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun create() {
        super.create()
        updatePreferences()
    }

    override fun resume() {
        super.resume()
        updatePreferences()
    }

    override fun previewStateChange(isPreview: Boolean) {}

    override fun iconDropped(x: Int, y: Int) {}

    override fun offsetChange(
        xOffset: Float, yOffset: Float,
        xOffsetStep: Float, yOffsetStep: Float,
        xPixelOffset: Int, yPixelOffset: Int
    ) {
        if (prefs.useScroll && screens.containsKey(GameScreen::class.java)) {
            moveCameraByOffset(xOffset);
        }
    }
}