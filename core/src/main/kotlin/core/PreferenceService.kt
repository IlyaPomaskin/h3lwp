package core

import com.badlogic.gdx.Gdx
import com.homm3.livewallpaper.core.Constants
import ktx.preferences.set

class PreferenceService {
    private val prefs = Gdx.app.getPreferences(Constants.Preferences.PREFERENCES_NAME)

    var brightness: Int
        get() = prefs.getInteger(
            Constants.Preferences.BRIGHTNESS,
            Constants.Preferences.BRIGHTNESS_DEFAULT
        )
        set(value) {
            prefs[Constants.Preferences.BRIGHTNESS] = value
        }

    var mapUpdateInterval: Float
        get() = prefs.getFloat(
            Constants.Preferences.MAP_UPDATE_INTERVAL,
            Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL
        )
        set(value) {
            prefs[Constants.Preferences.MAP_UPDATE_INTERVAL] = value
        }

    var scale: Int
        get() = prefs.getInteger(
            Constants.Preferences.SCALE,
            Constants.Preferences.DEFAULT_SCALE
        )
        set(value) {
            prefs[Constants.Preferences.SCALE] = value
        }
}