package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidEngine(private val context: Context) :
    Engine(
        prefs = MutableStateFlow(WallpaperPreferences()),
        onSettingsButtonClick = { _, _ ->
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    ),
    AndroidWallpaperListener {

    override fun offsetChange(
        xOffset: Float,
        yOffset: Float,
        xOffsetStep: Float,
        yOffsetStep: Float,
        xPixelOffset: Int,
        yPixelOffset: Int
    ) {
        moveCameraByOffset(xOffset)
    }

    override fun previewStateChange(isPreview: Boolean) {}
    override fun iconDropped(x: Int, y: Int) {}
}
