package com.homm3.livewallpaper.android.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.core.Constants
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.parser.AssetsConverter
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread

sealed class ParsingState {
    object Initial : ParsingState()
    object InProgress : ParsingState()
    data class Error(val ex: Exception) : ParsingState()
    object Done : ParsingState()
}

class SettingsViewModel(
    private val application: Application,
    private val wallpaperPreferencesRepository: WallpaperPreferencesRepository
) : ViewModel() {

    val initialSetupEvent = liveData {
        emit(wallpaperPreferencesRepository.fetchInitialPreferences())
    }

    val settingsUiModel = wallpaperPreferencesRepository.preferencesFlow.asLiveData()

    var parsingStateUiModel by mutableStateOf<ParsingState>(ParsingState.Initial)

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

    private fun prepareInputStream(uri: Uri): InputStream {
        return application
            .applicationContext
            .contentResolver
            .openInputStream(uri) ?: throw Exception("Can't open selected file")
    }

    private fun prepareOutputFile(): File {
        return kotlin.runCatching {
            application
                .applicationContext
                .filesDir
                .resolve(Constants.Assets.ATLAS_FOLDER)
        }
            .mapCatching {
                if (it.exists()) {
                    it.deleteRecursively()
                }
                it.mkdirs()
                it
            }
            .getOrElse { throw Exception("Can't prepare output directory") }
    }

    fun parseFile(file: Uri) {
        GdxNativesLoader.load()

        thread {
            try {
                parsingStateUiModel = ParsingState.InProgress

                AssetsConverter(
                    prepareInputStream(file),
                    prepareOutputFile(),
                    Constants.Assets.ATLAS_NAME
                ).convertLodToTextureAtlas()

                parsingStateUiModel = ParsingState.Done
            } catch (ex: Exception) {
                parsingStateUiModel = ParsingState.Error(ex)
            }
        }
    }
}
