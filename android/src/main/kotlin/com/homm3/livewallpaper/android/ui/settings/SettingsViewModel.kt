package com.homm3.livewallpaper.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

class SettingsViewModel(private val repository: WallpaperPreferencesRepository) : ViewModel() {

    val preferences: StateFlow<WallpaperPreferences?> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun setScale(scale: Scale) = launchUpdate { repository.setScale(scale) }

    fun setMapUpdateInterval(interval: MapUpdateInterval) = launchUpdate { repository.setMapUpdateInterval(interval) }

    fun setUseScroll(useScroll: Boolean) = launchUpdate { repository.setUseScroll(useScroll) }

    fun setBrightness(brightness: Float) = launchUpdate { repository.setBrightness(brightness) }

    private fun launchUpdate(update: suspend () -> Unit) {
        viewModelScope.launch { update() }
    }
}
