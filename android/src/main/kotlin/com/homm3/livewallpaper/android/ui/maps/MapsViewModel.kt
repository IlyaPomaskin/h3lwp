package com.homm3.livewallpaper.android.ui.maps

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homm3.livewallpaper.android.data.MapImportResult
import com.homm3.livewallpaper.android.data.MapsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class MapsUiState(
    val maps: List<String> = emptyList(),
    val isImporting: Boolean = false,
    val importResult: MapImportResult? = null,
) {
    val canDeleteMaps: Boolean get() = maps.size > 1
}

class MapsViewModel(private val repository: MapsRepository) : ViewModel() {

    private val importState = MutableStateFlow(MapsUiState())

    val uiState: StateFlow<MapsUiState> = combine(repository.maps, importState) { maps, state ->
        state.copy(maps = maps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MapsUiState())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun importMap(uri: Uri) {
        importState.update { it.copy(isImporting = true, importResult = null) }
        viewModelScope.launch {
            val result = repository.import(uri)
            importState.update { it.copy(isImporting = false, importResult = result) }
        }
    }

    fun deleteMap(mapName: String) {
        viewModelScope.launch { repository.delete(mapName) }
    }

    fun onImportResultShown() {
        importState.update { it.copy(importResult = null) }
    }
}
