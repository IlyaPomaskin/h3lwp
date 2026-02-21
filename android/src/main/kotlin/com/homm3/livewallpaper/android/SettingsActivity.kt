package com.homm3.livewallpaper.android

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.data.SettingsViewModel
import com.homm3.livewallpaper.android.data.SettingsViewModelFactory
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.data.dataStore
import com.homm3.livewallpaper.android.ui.settings.*
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences

class SettingsActivity : ComponentActivity() {

    private fun openMaps() {
        startActivity(Intent(this, MapsActivity::class.java))
    }

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

        val factory = SettingsViewModelFactory(WallpaperPreferencesRepository(dataStore))

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) darkColorScheme() else lightColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme) {
                val viewModel: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = viewModel,
                    onSetWallpaper = ::setWallpaper,
                    onOpenMaps = ::openMaps,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSetWallpaper: () -> Unit,
    onOpenMaps: () -> Unit,
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onSetWallpaper) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.wallpaper_change_button)
                )
            }
        },
    ) { contentPadding ->
        SettingsContainer(contentPadding) {
            item { SettingsCategory(text = stringResource(R.string.preferences_header)) }
            item { ScaleDropdown(prefs.scale, viewModel::setScale) }
            item { UpdateIntervalDropdown(prefs.mapUpdateInterval, viewModel::setMapUpdateInterval) }
            item { UseScrollToggle(prefs.useScroll, viewModel::toggleUseScroll) }
            item { BrightnessSlider(prefs.brightness, viewModel::setBrightness) }
            item {
                SettingsItem(
                    title = stringResource(R.string.maps_title),
                    onClick = onOpenMaps,
                )
            }
        }
    }
}

@Composable
private fun ScaleDropdown(selected: Scale, onSelected: (Scale) -> Unit) {
    val options = listOf(
        SettingsDropdownItem(Scale.X1, "x1"),
        SettingsDropdownItem(Scale.X2, "x2"),
        SettingsDropdownItem(Scale.X3, "x3"),
        SettingsDropdownItem(Scale.X4, "x4"),
        SettingsDropdownItem(Scale.X5, "x5"),
    )

    SettingsDropdown(
        title = stringResource(R.string.scale_title),
        subtitle = options.find { it.value == selected }?.title.orEmpty(),
        items = options,
        selectedItemValue = selected,
        onItemSelected = { onSelected(it.value) },
    )
}

@Composable
private fun UpdateIntervalDropdown(
    selected: MapUpdateInterval,
    onSelected: (MapUpdateInterval) -> Unit
) {
    val options = listOf(
        SettingsDropdownItem(MapUpdateInterval.EVERY_SWITCH, stringResource(R.string.update_timeout_every_switch)),
        SettingsDropdownItem(MapUpdateInterval.MINUTES_10, stringResource(R.string.update_timeout_10minutes)),
        SettingsDropdownItem(MapUpdateInterval.MINUTES_30, stringResource(R.string.update_timeout_30minutes)),
        SettingsDropdownItem(MapUpdateInterval.HOURS_2, stringResource(R.string.update_timeout_2hour)),
        SettingsDropdownItem(MapUpdateInterval.HOURS_24, stringResource(R.string.update_timeout_24hours)),
    )

    SettingsDropdown(
        title = stringResource(R.string.update_time_title),
        subtitle = options.find { it.value == selected }?.title.orEmpty(),
        items = options,
        selectedItemValue = selected,
        onItemSelected = { onSelected(it.value) },
    )
}

@Composable
private fun UseScrollToggle(useScroll: Boolean, onToggle: () -> Unit) {
    SettingsItem(
        title = stringResource(R.string.use_scroll_title),
        subtitle = stringResource(R.string.use_scroll_summary),
        onClick = onToggle,
    ) { interactionSource ->
        Switch(
            checked = useScroll,
            onCheckedChange = { onToggle() },
            interactionSource = interactionSource
        )
    }
}

@Composable
private fun BrightnessSlider(brightness: Float, onBrightnessSet: (Float) -> Unit) {
    var sliderValue by remember { mutableFloatStateOf(brightness) }

    LaunchedEffect(brightness) {
        sliderValue = brightness
    }

    SettingsItem(
        title = stringResource(R.string.brightness_title),
        nextLine = true,
    ) {
        Slider(
            value = sliderValue,
            valueRange = 0f..0.99f,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onBrightnessSet(sliderValue) }
        )
    }
}
