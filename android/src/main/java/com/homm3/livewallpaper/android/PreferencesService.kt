package com.homm3.livewallpaper.android

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    val abc = MutableLiveData(0)
}

class SuperheroesViewModel(val prefs: SharedPreferences) : ViewModel() {
    val brightness: MutableLiveData<Int> = MutableLiveData()

    fun setBrightness(nextValue: Int) {
        prefs.edit().putInt(Constants.Preferences.BRIGHTNESS, nextValue).apply()
        brightness.value = nextValue
    }

    var useScroll: Boolean
        get() = getValue(
            Constants.Preferences.USE_SCROLL,
            Constants.Preferences.USE_SCROLL_DEFAULT
        )
        set(value) {
            prefs.edit().putBoolean(Constants.Preferences.USE_SCROLL, value).apply()
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

//
//    // Added a delay of 2 seconds to emulate a network request. This method just sets the list of
//    // superheroes to the livedata after 2 seconds.
//    suspend fun loadSuperheroes(): List<Person> {
//        delay(2000)
//        return getSuperheroList()
//    }
}
