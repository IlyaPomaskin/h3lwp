package com.homm3.livewallpaper.android

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.data.*
import com.homm3.livewallpaper.android.ui.components.NavigationHost
import kotlinx.coroutines.flow.first


class MainActivity() : ComponentActivity() {
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

    private fun openIconAuthorUrl() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.icon_author_url))
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapsViewModel: MapsViewModel by viewModels {
            MapsViewModelFactory(contentResolver, filesDir)
        }
        val settingsViewModel = SettingsViewModel(
            WallpaperPreferencesRepository(dataStore),
            setWallpaper = ::setWallpaper,
            openIconAuthorUrl = ::openIconAuthorUrl
        )
        val parsingViewModel = ParsingViewModel(application)

        setContent {
            LaunchedEffect(true) {
                if (mapsViewModel.mapsList.first().isEmpty()) {
                    parsingViewModel.copyDefaultMap()
                }
            }

            NavigationHost(
                mapViewModel = mapsViewModel,
                settingsViewModel = settingsViewModel,
                parsingViewModel = parsingViewModel,
            )
        }
    }
}

