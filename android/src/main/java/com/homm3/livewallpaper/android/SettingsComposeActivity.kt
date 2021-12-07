package com.homm3.livewallpaper.android

import android.content.SharedPreferences
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

interface SettingsProviderInterface {
    var scale: String
        get() = TODO("implement")
        set(value) = TODO("implement")

    var updateInterval: String
        get() = TODO("implement")
        set(value) = TODO("implement")

    var useScroll: Boolean
        get() = TODO("implement")
        set(value) = TODO("implement")

    var brightness: Int
        get() = TODO("implement")
        set(value) = TODO("implement")
}

class AndroidSettingsProvider(private val prefs: SharedPreferences) : SettingsProviderInterface {
    fun getValue(name: String, default: String): String {
        return prefs.runCatching { getString(name, default) ?: default }.getOrDefault(default)
    }

    fun getValue(name: String, default: Int): Int {
        return prefs.runCatching { getInt(name, default) }.getOrDefault(default)
    }

    fun getValue(name: String, default: Boolean): Boolean {
        return prefs.runCatching { getBoolean(name, default) }.getOrDefault(default)
    }

    override var scale: String
        get() = getValue(
            Constants.Preferences.SCALE,
            Constants.Preferences.DEFAULT_SCALE
        )
        set(value) {
            prefs.edit().putString(Constants.Preferences.SCALE, value).apply()
        }

    override var updateInterval: String
        get() = getValue(
            Constants.Preferences.MAP_UPDATE_INTERVAL,
            Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL
        )
        set(value) {
            prefs.edit().putString(Constants.Preferences.MAP_UPDATE_INTERVAL, value).apply()
        }

    override var useScroll: Boolean
        get() = getValue(
            Constants.Preferences.USE_SCROLL,
            Constants.Preferences.USE_SCROLL_DEFAULT
        )
        set(value) {
            prefs.edit().putBoolean(Constants.Preferences.USE_SCROLL, value).apply()
        }

    override var brightness: Int
        get() = getValue(
            Constants.Preferences.BRIGHTNESS,
            Constants.Preferences.BRIGHTNESS_DEFAULT
        )
        set(value) {
            prefs.edit().putInt(Constants.Preferences.BRIGHTNESS, value).apply()
        }

}

class SettingsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = AndroidSettingsProvider(
            getSharedPreferences(Constants.Preferences.PREFERENCES_NAME, MODE_PRIVATE)
        )

        setContent {
            SettingsFun(settings = settings)
        }
    }
}

@Composable
fun SettingsFun(settings: SettingsProviderInterface) {
    var scale by remember { mutableStateOf(settings.scale) }
    val setScale = fun(nextValue: String) {
        scale = nextValue
        settings.scale = nextValue
    }

    var updateInterval by remember { mutableStateOf(settings.updateInterval) }
    val setUpdateInterval = fun(nextValue: String) {
        updateInterval = nextValue
        settings.updateInterval = nextValue
    }

    var useScroll by remember { mutableStateOf(settings.useScroll) }
    val toggleUseScroll = fun() {
        useScroll = !useScroll;
        settings.useScroll = !useScroll
    }

    var brightness by remember { mutableStateOf(settings.brightness) }
    val setBrightness = fun(nextValue: Int) { brightness = nextValue; }
    val saveBrightness = fun() { settings.brightness = brightness }

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