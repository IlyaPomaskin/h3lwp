package com.homm3.livewallpaper.android.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.homm3.livewallpaper.android.ui.theme.H3WallpaperTheme

fun ComponentActivity.setScreenContent(content: @Composable () -> Unit) {
    enableEdgeToEdge()
    setContent {
        H3WallpaperTheme(content = content)
    }
}
