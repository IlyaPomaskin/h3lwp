package com.homm3.livewallpaper.android.wallpaper

import android.content.Context
import android.content.res.Configuration
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.BuildConfig
import com.homm3.livewallpaper.android.MainActivity
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AndroidEngine(
    context: Context,
    private val prefs: Flow<WallpaperPreferences>,
) : Engine(
    prefs = prefs,
    onSettingsButtonClick = { _, _ -> context.startActivity(MainActivity.newTaskIntent(context)) },
    isDarkMode = {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightMode == Configuration.UI_MODE_NIGHT_YES
    },
), AndroidWallpaperListener {

    private val prefsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var useScroll = WallpaperPreferences.defaultUseScroll

    override fun create() {
        if (BuildConfig.DEBUG) Gdx.app.logLevel = Application.LOG_DEBUG
        super.create()
        prefsScope.launch {
            prefs.collect { useScroll = it.useScroll }
        }
        RunningEngine.register(this)
    }

    override fun dispose() {
        RunningEngine.unregister(this)
        prefsScope.cancel()
        super.dispose()
    }

    override fun offsetChange(
        xOffset: Float,
        yOffset: Float,
        xOffsetStep: Float,
        yOffsetStep: Float,
        xPixelOffset: Int,
        yPixelOffset: Int,
    ) {
        if (useScroll) moveCameraByOffset(xOffset)
    }

    override fun previewStateChange(isPreview: Boolean) {}

    override fun iconDropped(x: Int, y: Int) {}
}
