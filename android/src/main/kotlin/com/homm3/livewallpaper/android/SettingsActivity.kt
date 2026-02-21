package com.homm3.livewallpaper.android

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.data.SettingsViewModel
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.data.dataStore
import com.homm3.livewallpaper.android.ui.settings.*
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences

class SettingsActivity : ComponentActivity() {

    private fun setWallpaper() {
        startActivity(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, LiveWallpaperService::class.java)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = SettingsViewModel(
            WallpaperPreferencesRepository(dataStore),
            setWallpaper = ::setWallpaper
        )

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) darkColorScheme() else lightColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme) {
                SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: SettingsViewModel) {
    val scaleOptions = listOf(
        SettingsDropdownItem(Scale.DPI, stringResource(R.string.scale_by_density)),
        SettingsDropdownItem(Scale.X2, "x2"),
        SettingsDropdownItem(Scale.X3, "x3"),
        SettingsDropdownItem(Scale.X4, "x4"),
    )
    val mapUpdateIntervalOptions = listOf(
        SettingsDropdownItem(
            MapUpdateInterval.EVERY_SWITCH,
            stringResource(R.string.update_timeout_every_switch)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.MINUTES_10,
            stringResource(R.string.update_timeout_10minutes)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.MINUTES_30,
            stringResource(R.string.update_timeout_30minutes)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.HOURS_2,
            stringResource(R.string.update_timeout_2hour)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.HOURS_24,
            stringResource(R.string.update_timeout_24hours)
        ),
    )

    val prefs by viewModel.settingsUiModel.observeAsState(WallpaperPreferences())
    var brightnessSliderValue by remember { mutableStateOf(prefs.brightness) }

    if (brightnessSliderValue == WallpaperPreferences.Defaults.defaultBrightness
        && prefs.brightness != WallpaperPreferences.Defaults.defaultBrightness
    ) {
        brightnessSliderValue = prefs.brightness
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onSetWallpaper() }
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.wallpaper_change_button)
                )
            }
        },
    ) { contentPadding ->
        SettingsContainer {
            item { SettingsCategory(text = stringResource(R.string.preferences_header)) }
            item {
                SettingsDropdown(
                    title = stringResource(R.string.scale_title),
                    subtitle = scaleOptions.find { it.value == prefs.scale }?.title.orEmpty(),
                    items = scaleOptions,
                    selectedItemValue = prefs.scale,
                    onItemSelected = { viewModel.setScale(it.value) },
                )
            }
            item {
                SettingsDropdown(
                    title = stringResource(R.string.update_time_title),
                    subtitle = mapUpdateIntervalOptions.find { it.value == prefs.mapUpdateInterval }?.title.orEmpty(),
                    items = mapUpdateIntervalOptions,
                    selectedItemValue = prefs.mapUpdateInterval,
                    onItemSelected = { viewModel.setMapUpdateInterval(it.value) },
                )
            }
            item {
                SettingsItem(
                    title = stringResource(R.string.use_scroll_title),
                    subtitle = stringResource(R.string.use_scroll_summary),
                    onClick = { viewModel.toggleUseScroll() },
                ) { interactionSource ->
                    Switch(
                        checked = prefs.useScroll,
                        onCheckedChange = { viewModel.toggleUseScroll() },
                        interactionSource = interactionSource
                    )
                }
            }
            item {
                SettingsItem(
                    title = stringResource(R.string.brightness_title),
                    nextLine = true,
                ) {
                    Slider(
                        value = brightnessSliderValue,
                        valueRange = 0f..0.99f,
                        onValueChange = { brightnessSliderValue = it },
                        onValueChangeFinished = { viewModel.setBrightness(brightnessSliderValue) }
                    )
                }
            }
        }
    }
}
