package com.homm3.livewallpaper.android.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.core.Constants
import com.homm3.livewallpaper.parser.AssetsConverter
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread

sealed class ParsingState {
    object Initial : ParsingState()
    object InProgress : ParsingState()
    object Done : ParsingState()
    object Error : ParsingState()
}

class ParsingViewModel(
    private val application: Application
) : ViewModel() {
    var parsingStateUiModel by mutableStateOf<ParsingState>(ParsingState.Initial)
    var parsingError by mutableStateOf<Exception?>(null)

    fun clearParsingError() {
        parsingStateUiModel = ParsingState.Initial
    }

    fun isGameAssetsAvailable(): Boolean {
        return application
            .applicationContext
            .filesDir
            .resolve(Constants.Assets.ATLAS_PATH)
            .exists()
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
                parsingError = ex
                parsingStateUiModel = ParsingState.Error
            }
        }
    }
}
