package com.homm3.livewallpaper.android

import android.app.Application
import android.content.Context
import com.homm3.livewallpaper.android.data.GameDataRepository
import com.homm3.livewallpaper.android.data.MapsRepository
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.data.wallpaperDataStore

class H3WallpaperApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    val preferencesRepository = WallpaperPreferencesRepository(context.wallpaperDataStore)
    val gameDataRepository = GameDataRepository(context)
    val mapsRepository = MapsRepository(context)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as H3WallpaperApplication).container
