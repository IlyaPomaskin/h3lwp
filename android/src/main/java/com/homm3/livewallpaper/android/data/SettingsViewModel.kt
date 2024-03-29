package com.homm3.livewallpaper.android.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val wallpaperPreferencesRepository: WallpaperPreferencesRepository,
    private val setWallpaper: () -> Unit,
    private val openIconAuthorUrl: () -> Unit
) : ViewModel() {
    val settingsUiModel = wallpaperPreferencesRepository.preferencesFlow.asLiveData()

    fun toggleUseScroll() {
        viewModelScope.launch {
            wallpaperPreferencesRepository.toggleUseScroll()
        }
    }

    fun setScale(value: Scale) {
        viewModelScope.launch {
            wallpaperPreferencesRepository.setScale(value)
        }
    }

    fun setMapUpdateInterval(value: MapUpdateInterval) {
        viewModelScope.launch {
            wallpaperPreferencesRepository.setMapUpdateInterval(value)
        }
    }

    fun setBrightness(value: Float) {
        viewModelScope.launch {
            wallpaperPreferencesRepository.setBrightness(value)
        }
    }

    fun onSetWallpaper() {
        setWallpaper()
    }

    fun onOpenIconAuthorUrl() {
        openIconAuthorUrl()
    }
}
