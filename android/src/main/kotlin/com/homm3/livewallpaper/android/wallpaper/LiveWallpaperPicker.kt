package com.homm3.livewallpaper.android.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.homm3.livewallpaper.android.LiveWallpaperService

fun Context.openLiveWallpaperPicker(): Boolean {
    val previewIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
        ComponentName(this, LiveWallpaperService::class.java),
    )
    val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)

    return listOf(previewIntent, chooserIntent).any { intent ->
        try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
