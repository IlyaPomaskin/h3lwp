package com.homm3.livewallpaper.android.wallpaper

import android.content.Context
import androidx.core.app.TaskStackBuilder
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidWallpaperListener
import com.homm3.livewallpaper.BuildConfig
import com.homm3.livewallpaper.android.AssetSetupActivity
import com.homm3.livewallpaper.android.SettingsActivity
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
    onSettingsButtonClick = { _, _ -> openSettings(context) },
    onHotaButtonClick = { _, _ -> openAssetSetup(context) },
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
    }

    override fun dispose() {
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

private fun openSettings(context: Context) {
    TaskStackBuilder.create(context)
        .addNextIntent(SettingsActivity.intent(context))
        .startActivities()
}

private fun openAssetSetup(context: Context) {
    TaskStackBuilder.create(context)
        .addNextIntent(SettingsActivity.intent(context))
        .addNextIntent(AssetSetupActivity.intent(context, isInitialSetup = false))
        .startActivities()
}
