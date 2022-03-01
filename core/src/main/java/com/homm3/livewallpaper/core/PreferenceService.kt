package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import ktx.preferences.set

class PreferenceService {
    private val prefs = Gdx.app.getPreferences(Constants.Preferences.PREFERENCES_NAME)

    var brightness: Float
        get() = prefs.getFloat(
            Constants.Preferences.BRIGHTNESS,
            Constants.Preferences.BRIGHTNESS_DEFAULT
        )
        set(value) {
            prefs[Constants.Preferences.BRIGHTNESS] = value
        }

    var mapUpdateInterval: Float
        get() = prefs.getString(
            Constants.Preferences.MAP_UPDATE_INTERVAL,
            Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL.toString()
        ).toFloat()
        set(value) {
            prefs[Constants.Preferences.MAP_UPDATE_INTERVAL] = value
        }

    var scale: Int
        get() = prefs.getString(
            Constants.Preferences.SCALE,
            Constants.Preferences.DEFAULT_SCALE.toString()
        ).toInt()
        set(value) {
            prefs[Constants.Preferences.SCALE] = value.toString()
        }
}