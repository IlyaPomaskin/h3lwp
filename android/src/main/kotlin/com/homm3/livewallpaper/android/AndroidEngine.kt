package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AndroidEngine(
    private val context: Context,
    private val prefs: Flow<WallpaperPreferences>
) :
    Engine(
        prefs = prefs,
        onSettingsButtonClick = { _, _ ->
            val intent = Intent(context, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        },
        onHotaButtonClick = { _, _ ->
            val intent = Intent(context, AssetSetupActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    ),
    AndroidWallpaperListener {

    private var useScroll = WallpaperPreferences.defaultUseScroll

    init {
        CoroutineScope(Dispatchers.Default).launch {
            prefs.collect { useScroll = it.useScroll }
        }
    }

    override fun offsetChange(
        xOffset: Float,
        yOffset: Float,
        xOffsetStep: Float,
        yOffsetStep: Float,
        xPixelOffset: Int,
        yPixelOffset: Int
    ) {
        if (useScroll) {
            moveCameraByOffset(xOffset)
        }
    }

    override fun previewStateChange(isPreview: Boolean) {}
    override fun iconDropped(x: Int, y: Int) {}
}
