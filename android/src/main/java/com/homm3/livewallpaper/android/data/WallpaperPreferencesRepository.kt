package com.homm3.livewallpaper.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "wallpaper_preferences")

class WallpaperPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    companion object PreferencesKeys {
        val USE_SCROLL = booleanPreferencesKey("use_scroll")
        val SCALE = intPreferencesKey("scale")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val MAP_UPDATE_INTERVAL = intPreferencesKey("map_update_interval")
    }

    val preferencesFlow: Flow<WallpaperPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> mapUserPreferences(preferences) }

    suspend fun toggleUseScroll() {
        dataStore.edit { preferences ->
            preferences[USE_SCROLL] = preferences[USE_SCROLL]?.not() ?: false
        }
    }

    suspend fun setScale(value: Scale) {
        dataStore.edit { prefs -> prefs[SCALE] = value.value }
    }

    suspend fun setMapUpdateInterval(value: MapUpdateInterval) {
        dataStore.edit { prefs -> prefs[MAP_UPDATE_INTERVAL] = value.value }
    }

    suspend fun setBrightness(value: Float) {
        dataStore.edit { prefs -> prefs[BRIGHTNESS] = value }
    }

    suspend fun fetchInitialPreferences() =
        mapUserPreferences(dataStore.data.first().toPreferences())

    private fun mapUserPreferences(preferences: Preferences): WallpaperPreferences {
        val scale = Scale.fromInt(preferences[SCALE])
        val mapUpdateInterval = MapUpdateInterval.fromInt(preferences[MAP_UPDATE_INTERVAL])
        val useScroll = preferences[USE_SCROLL] ?: WallpaperPreferences.defaultUseScroll
        val brightness = preferences[BRIGHTNESS] ?: WallpaperPreferences.defaultBrightness

        return WallpaperPreferences(scale, mapUpdateInterval, useScroll, brightness)
    }

}