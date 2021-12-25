package com.homm3.livewallpaper.android.data

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MapsViewModel(private val contentResolver: ContentResolver, root: File) : ViewModel() {
    private val mapsFolder = root.resolve("user-maps")

    private val _uiState = MutableStateFlow(mapsFolder)
    val mapsList: StateFlow<File> = _uiState

    init {
        if (!mapsFolder.exists()) {
            mapsFolder.mkdir()
        }
    }

    suspend fun addMap(map: File) {
        map.copyTo(mapsFolder, true)
        _uiState.emit(mapsFolder)
    }

    fun copyMap(uri: Uri) {
        viewModelScope.launch {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val outputStream = FileOutputStream(mapsFolder.resolve("file.h3m"))
                val buf = ByteArray(1024)
                var bytesCount = 0

                do {
                    bytesCount = inputStream.read(buf)
                    if (bytesCount > -1) {
                        outputStream.write(buf, 0, bytesCount)
                    }
                } while (bytesCount != -1)

                inputStream.close()
                outputStream.close()
            }

            _uiState.emit(mapsFolder)
        }
    }
}


class MapsViewModelFactory(private val contentResolver: ContentResolver, private val root: File) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapsViewModel(contentResolver, root) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

