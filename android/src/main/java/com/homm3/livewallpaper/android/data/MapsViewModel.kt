package com.homm3.livewallpaper.android.data

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homm3.livewallpaper.parser.formats.H3m
import com.homm3.livewallpaper.parser.formats.H3mReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed class MapReadingException : Throwable() {
    object CantOpenStream : MapReadingException()
    object CantParseMap : MapReadingException()
    object CantCopyMap : MapReadingException()
}

class MapsViewModel(private val contentResolver: ContentResolver, root: File) : ViewModel() {
    private val mapsFolder = root.resolve("user-maps")

    private val _mapsListState = MutableStateFlow(emptyList<File>())
    private val _mapReadingErrorState = MutableStateFlow<MapReadingException?>(null)
    val mapsList: StateFlow<List<File>> = _mapsListState
    val mapReadingError: StateFlow<MapReadingException?> = _mapReadingErrorState

    init {
        if (!mapsFolder.exists()) {
            mapsFolder.mkdir()
        }

        viewModelScope.launch { updateFilesList() }
    }

    private suspend fun updateFilesList() {
        val files = mapsFolder.listFiles()

        if (files !== null) {
            _mapsListState.emit(files.toList())
        }
    }

    private fun copyMapToFile(stream: InputStream, filename: String) {
        try {
            stream.copyTo(FileOutputStream(mapsFolder.resolve(filename)))
        } catch (ex: Exception) {
            throw MapReadingException.CantCopyMap
        }
    }

    private fun openStream(uri: Uri): InputStream {
        try {
            val stream = contentResolver.openInputStream(uri)

            if (stream === null) {
                throw MapReadingException.CantOpenStream
            }

            return stream
        } catch (ex: Exception) {
            throw MapReadingException.CantOpenStream
        }
    }

    private fun parseMap(stream: InputStream): H3m {
        try {
            return H3mReader(stream).read()
        } catch (ex: Exception) {
            throw MapReadingException.CantParseMap
        }
    }

    fun resetCopyMapError() {
        viewModelScope.launch {
            _mapReadingErrorState.emit(null)
        }
    }

    fun copyMap(uri: Uri) {
        viewModelScope.launch {
            try {
                val h3mStream = openStream(uri)
                val h3m = parseMap(h3mStream)
                h3mStream.close()

                val fileStream = openStream(uri)
                copyMapToFile(fileStream, "${h3m.header.title}.h3m")
                h3mStream.close()
                updateFilesList()
            } catch (ex: MapReadingException) {
                _mapReadingErrorState.emit(ex)
            }
        }
    }

    fun removeMap(name: String) {
        viewModelScope.launch {
            mapsFolder.resolve(name).delete()

            updateFilesList()
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

