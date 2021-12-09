package com.homm3.livewallpaper.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.homm3.livewallpaper.android.ui.components.SettingsScreen
import com.homm3.livewallpaper.core.Constants

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferencesService(
            getSharedPreferences(Constants.Preferences.PREFERENCES_NAME, MODE_PRIVATE)
        )
        setContent {
            SettingsScreen(preferences = preferences)
        }
    }
}