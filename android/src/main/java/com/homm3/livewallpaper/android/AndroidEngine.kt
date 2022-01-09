package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.screens.GameScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AndroidEngine(
    private val context: Context,
    private val prefs: Flow<WallpaperPreferences>
) : Engine(prefs), AndroidWallpaperListener {
    var useScroll = WallpaperPreferences.defaultUseScroll

    init {
        CoroutineScope(Dispatchers.Default).launch {
            prefs.collect { useScroll = it.useScroll }
        }
    }

    override fun onSettingsButtonClick() {
        context.startActivity(
            Intent()
                .setClass(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun previewStateChange(isPreview: Boolean) {}

    override fun iconDropped(x: Int, y: Int) {}

    override fun offsetChange(
        xOffset: Float, yOffset: Float,
        xOffsetStep: Float, yOffsetStep: Float,
        xPixelOffset: Int, yPixelOffset: Int
    ) {
        if (useScroll && screens.containsKey(GameScreen::class.java)) {
            moveCameraByOffset(xOffset);
        }
    }
}