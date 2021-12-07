package com.homm3.livewallpaper.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme
import com.homm3.livewallpaper.core.Constants

class SettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferencesService(
            getSharedPreferences(Constants.Preferences.PREFERENCES_NAME, MODE_PRIVATE)
        )
        setContent {
            SettingsFun(preferences = preferences)
        }
    }
}

@Composable
fun SettingsFun(preferences: PreferencesService) {
    var scale by remember { mutableStateOf(preferences.scale) }
    val setScale = fun(nextValue: String) {
        scale = nextValue
        preferences.scale = nextValue
    }

    var updateInterval by remember { mutableStateOf(preferences.updateInterval) }
    val setUpdateInterval = fun(nextValue: String) {
        updateInterval = nextValue
        preferences.updateInterval = nextValue
    }

    var useScroll by remember { mutableStateOf(preferences.useScroll) }
    val toggleUseScroll = fun() {
        useScroll = !useScroll;
        preferences.useScroll = !useScroll
    }

    var brightness by remember { mutableStateOf(preferences.brightness) }
    val setBrightness = fun(nextValue: Int) { brightness = nextValue; }
    val saveBrightness = fun() { preferences.brightness = brightness }

    H3lwpnextTheme {
        val scaleOptions = stringArrayResource(id = R.array.scale_values)
            .zip(stringArrayResource(id = R.array.scale_entries))
            .map { SettingsDropdownItem(it.first, it.second) }

        val updateIntervalOptions = stringArrayResource(id = R.array.update_timeout_values)
            .zip(stringArrayResource(id = R.array.update_timeout_entries))
            .map { SettingsDropdownItem(it.first, it.second) }

        ListContainer {
            item { SettingsCategory(text = "Wallpaper") }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.wallpaper_change_button),
                    onClick = { println("set wallpaper") }
                )
            }

            item { SettingsCategory(text = "Preferences") }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.scale_title),
                    subtitle = scaleOptions.find { it.value == scale }?.title.orEmpty(),
                    items = scaleOptions,
                    selectedItemKey = scale,
                    onItemSelected = { setScale(it.value) },
                )
            }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.update_time_title),
                    subtitle = updateIntervalOptions.find { it.value == updateInterval }?.title.orEmpty(),
                    items = updateIntervalOptions,
                    selectedItemKey = updateInterval,
                    onItemSelected = { setUpdateInterval(it.value) },
                )
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.use_scroll_title),
                    subtitle = stringResource(id = R.string.use_scroll_summary),
                    onClick = { toggleUseScroll() },
                ) { interactionSource ->
                    Switch(
                        checked = useScroll,
                        onCheckedChange = { toggleUseScroll() },
                        interactionSource = interactionSource
                    )
                }
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.brightness_title),
                    nextLine = true,
                    onClick = { },
                ) {
                    Slider(
                        value = brightness / 100f,
                        valueRange = 0f..1f,
                        onValueChangeFinished = { saveBrightness() },
                        onValueChange = { setBrightness((it * 100).toInt()) }
                    )
                }
            }

            item { SettingsCategory(text = "Credits") }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.credits_button),
                    onClick = { },
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview2() {
    Prev()
}