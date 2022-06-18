package com.homm3.livewallpaper.android

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.homm3.livewallpaper.android.data.*
import com.homm3.livewallpaper.android.ui.components.NavigationHost


class MainActivity : ComponentActivity() {
    private fun setWallpaper() {
        startActivity(
            Intent()
                .setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(
                        applicationContext,
                        LiveWallpaperService::class.java
                    )
                )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapsViewModel: MapsViewModel by viewModels {
            MapsViewModelFactory(contentResolver, filesDir)
        }
        val settingsViewModel = SettingsViewModel(
            WallpaperPreferencesRepository(dataStore)
        )
        val parsingViewModel = ParsingViewModel(application)

//        FIXME delete
//        parsingViewModel.copyDefaultMap()

        setContent {
            NavigationHost(
                mapViewModel = mapsViewModel,
                settingsViewModel = settingsViewModel,
                parsingViewModel = parsingViewModel,
                onSetWallpaperClick = { setWallpaper() }
            )
        }
    }
}

