package com.homm3.livewallpaper.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Gold = Color(0xFF8A6D1F)
private val GoldLight = Color(0xFFE9C46A)
private val Crimson = Color(0xFF8E2C2C)
private val CrimsonLight = Color(0xFFFFB4A9)

private val LightColors = lightColorScheme(
    primary = Gold,
    secondary = Crimson,
)

private val DarkColors = darkColorScheme(
    primary = GoldLight,
    secondary = CrimsonLight,
)

@Composable
fun H3WallpaperTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme(isDarkTheme),
        content = content,
    )
}

@Composable
private fun colorScheme(isDarkTheme: Boolean): ColorScheme {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (!supportsDynamicColor) return if (isDarkTheme) DarkColors else LightColors

    val context = LocalContext.current
    return if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
