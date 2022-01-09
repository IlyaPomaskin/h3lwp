package com.homm3.livewallpaper.android.ui

import androidx.lifecycle.*
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val wallpaperPreferencesRepository: WallpaperPreferencesRepository
) : ViewModel() {

    val initialSetupEvent = liveData {
        emit(wallpaperPreferencesRepository.fetchInitialPreferences())
    }

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
}

class SettingsViewModelFactory(
    private val preferencesRepository: WallpaperPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferencesRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}