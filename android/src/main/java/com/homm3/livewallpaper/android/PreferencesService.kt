package com.homm3.livewallpaper.android

import android.content.SharedPreferences
import com.homm3.livewallpaper.core.Constants

class PreferencesService(private val prefs: SharedPreferences) {
    fun getValue(name: String, default: String): String {
        return prefs.runCatching { getString(name, default) ?: default }.getOrDefault(default)
    }

    fun getValue(name: String, default: Int): Int {
        return prefs.runCatching { getInt(name, default) }.getOrDefault(default)
    }

    fun getValue(name: String, default: Boolean): Boolean {
        return prefs.runCatching { getBoolean(name, default) }.getOrDefault(default)
    }

    var scale: String
        get() = getValue(
            Constants.Preferences.SCALE,
            Constants.Preferences.DEFAULT_SCALE
        )
        set(value) {
            prefs.edit().putString(Constants.Preferences.SCALE, value).apply()
        }

    var updateInterval: String
        get() = getValue(
            Constants.Preferences.MAP_UPDATE_INTERVAL,
            Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL
        )
        set(value) {
            prefs.edit().putString(Constants.Preferences.MAP_UPDATE_INTERVAL, value).apply()
        }

    var useScroll: Boolean
        get() = getValue(
            Constants.Preferences.USE_SCROLL,
            Constants.Preferences.USE_SCROLL_DEFAULT
        )
        set(value) {
            prefs.edit().putBoolean(Constants.Preferences.USE_SCROLL, value).apply()
        }

    var brightness: Int
        get() = getValue(
            Constants.Preferences.BRIGHTNESS,
            Constants.Preferences.BRIGHTNESS_DEFAULT
        )
        set(value) {
            prefs.edit().putInt(Constants.Preferences.BRIGHTNESS, value).apply()
        }
}
