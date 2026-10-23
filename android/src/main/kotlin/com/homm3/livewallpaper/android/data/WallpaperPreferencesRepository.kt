package com.homm3.livewallpaper.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DATA_STORE_NAME = "wallpaper_preferences"

val Context.wallpaperDataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

class WallpaperPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val preferences: Flow<WallpaperPreferences> = dataStore.data
        .catch { exception ->
            if (exception !is IOException) throw exception
            emit(emptyPreferences())
        }
        .map(::toWallpaperPreferences)

    suspend fun setScale(scale: Scale) = edit { it[Keys.SCALE] = scale.value }

    suspend fun setMapUpdateInterval(interval: MapUpdateInterval) = edit { it[Keys.MAP_UPDATE_INTERVAL] = interval.value }

    suspend fun setUseScroll(useScroll: Boolean) = edit { it[Keys.USE_SCROLL] = useScroll }

    suspend fun setBrightness(brightness: Float) = edit { it[Keys.BRIGHTNESS] = brightness }

    private suspend fun edit(transform: (MutablePreferences) -> Unit) {
        dataStore.edit { transform(it) }
    }

    private fun toWallpaperPreferences(preferences: Preferences) = WallpaperPreferences(
        scale = Scale.fromInt(preferences[Keys.SCALE]),
        mapUpdateInterval = MapUpdateInterval.fromInt(preferences[Keys.MAP_UPDATE_INTERVAL]),
        useScroll = preferences[Keys.USE_SCROLL] ?: WallpaperPreferences.defaultUseScroll,
        brightness = preferences[Keys.BRIGHTNESS] ?: WallpaperPreferences.defaultBrightness,
    )

    private object Keys {
        val USE_SCROLL = booleanPreferencesKey("use_scroll")
        val SCALE = intPreferencesKey("scale")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val MAP_UPDATE_INTERVAL = intPreferencesKey("map_update_interval")
    }
}
