package com.homm3.livewallpaper.android

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    fun onDestroy() {
        TODO("implement")
    }
}

class AndroidSettingsProvider(private val prefs: SharedPreferences) :
    SettingsProviderInterface,
    SharedPreferences.OnSharedPreferenceChangeListener {

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        println("onSharedPreferenceChanged $key ${sharedPreferences?.all?.get(key)}")

        if (key == Constants.Preferences.IS_ASSETS_READY_KEY) {
//            findPreference<Preference>("select_file")?.isEnabled = !isAssetsReady()
//            findPreference<Preference>("wallpaper_change")?.isVisible = isAssetsReady()
        }
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}

class SettingsComposeActivity : ComponentActivity() {
    lateinit var settings: SettingsProviderInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = AndroidSettingsProvider(
            getSharedPreferences(
                Constants.Preferences.PREFERENCES_NAME,
                MODE_PRIVATE
            )
        )

        setContent {
            SettingsFun(settings = settings)
        }
    }

    override fun onDestroy() {
        settings.onDestroy()
        super.onDestroy()
    }
}

@Composable
fun SettingsFun(settings: SettingsProviderInterface) {

    val scale = remember { mutableStateOf(settings.scale) }
    val updateInterval = remember { mutableStateOf(settings.updateInterval) }
    val useScroll = remember { mutableStateOf(settings.useScroll) }
    val brightness = remember { mutableStateOf(settings.brightness) }

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
                    subtitle = scaleOptions.find { it.value == scale.value }?.title.orEmpty(),
                    items = scaleOptions,
                    selectedItemKey = scale.value,
                    onItemSelected = { scale.value = it.value },
                )
            }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.update_time_title),
                    subtitle = updateIntervalOptions.find { it.value == updateInterval.value }?.title.orEmpty(),
                    items = updateIntervalOptions,
                    selectedItemKey = updateInterval.value,
                    onItemSelected = { updateInterval.value = it.value },
                )
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.use_scroll_title),
                    subtitle = stringResource(id = R.string.use_scroll_summary),
                    onClick = { useScroll.value = !useScroll.value },
                ) { interactionSource ->
                    Switch(
                        checked = settings.useScroll,
                        onCheckedChange = { useScroll.value = !useScroll.value },
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
                        value = brightness.value / 100f,
                        valueRange = 0f..1f,
                        onValueChange = { brightness.value = (it * 100).toInt() }
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