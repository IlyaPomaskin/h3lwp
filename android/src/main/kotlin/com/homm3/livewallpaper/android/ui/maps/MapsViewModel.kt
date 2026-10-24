package com.homm3.livewallpaper.android.ui.maps

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homm3.livewallpaper.android.data.GameDataRepository
import com.homm3.livewallpaper.android.data.MapImportResult
import com.homm3.livewallpaper.android.data.MapInfo
import com.homm3.livewallpaper.android.data.MapOrigin
import com.homm3.livewallpaper.android.data.MapsRepository
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val MAP_EXTENSION = ".h3m"

enum class MapGroupId { SOD_BASE, HOTA_BASE, USER }

data class MapItem(
    val fileName: String,
    val displayName: String,
    val isEnabled: Boolean,
    val isAvailable: Boolean,
    val canDelete: Boolean,
)

data class MapGroup(val id: MapGroupId, val maps: List<MapItem>) {
    private val availableMaps: List<MapItem> get() = maps.filter { it.isAvailable }

    val isAvailable: Boolean get() = availableMaps.isNotEmpty()
    val isMissingAssets: Boolean get() = maps.isNotEmpty() && availableMaps.isEmpty()
    val isEnabled: Boolean get() = availableMaps.any { it.isEnabled }
    val enabledCount: Int get() = availableMaps.count { it.isEnabled }
    val availableFileNames: List<String> get() = availableMaps.map { it.fileName }
}

data class MapsUiState(
    val groups: List<MapGroup> = emptyList(),
    val isImporting: Boolean = false,
    val importResults: List<MapImportResult> = emptyList(),
)

class MapsViewModel(
    private val repository: MapsRepository,
    gameDataRepository: GameDataRepository,
) : ViewModel() {

    private val importState = MutableStateFlow(MapsUiState())

    val uiState: StateFlow<MapsUiState> = combine(
        repository.maps,
        repository.disabledMaps,
        gameDataRepository.status,
        importState,
    ) { maps, disabledMaps, gameDataStatus, state ->
        val canDelete = maps.size > 1
        val items = maps.associateWith { map ->
            toMapItem(map, disabledMaps, isHotaInstalled = gameDataStatus.isHotaInstalled, canDelete = canDelete)
        }
        val mapsByGroup = maps.groupBy(::groupIdOf)
        val groups = MapGroupId.entries
            .map { groupId -> MapGroup(groupId, mapsByGroup[groupId].orEmpty().map(items::getValue)) }
            .filter { group -> group.id == MapGroupId.USER || group.maps.isNotEmpty() }
        state.copy(groups = groups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MapsUiState())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun importMaps(uris: List<Uri>) {
        if (uris.isEmpty()) return
        importState.update { it.copy(isImporting = true, importResults = emptyList()) }
        viewModelScope.launch {
            val results = uris.map { repository.import(it) }
            importState.update { it.copy(isImporting = false, importResults = results) }
        }
    }

    fun deleteMap(mapName: String) {
        viewModelScope.launch { repository.delete(mapName) }
    }

    fun setMapEnabled(mapName: String, isEnabled: Boolean) {
        viewModelScope.launch { repository.setMapsEnabled(listOf(mapName), isEnabled) }
    }

    fun setGroupEnabled(group: MapGroup, isEnabled: Boolean) {
        viewModelScope.launch { repository.setMapsEnabled(group.availableFileNames, isEnabled) }
    }

    fun onImportResultShown() {
        importState.update { it.copy(importResults = emptyList()) }
    }
}

private fun toMapItem(map: MapInfo, disabledMaps: Set<String>, isHotaInstalled: Boolean, canDelete: Boolean): MapItem {
    val isHotaMap = map.version == H3mVersion.HOTA
    val isUserMap = map.origin == MapOrigin.USER
    return MapItem(
        fileName = map.fileName,
        displayName = displayNameOf(map.fileName),
        isEnabled = map.fileName !in disabledMaps,
        isAvailable = isHotaInstalled || !isHotaMap,
        canDelete = isUserMap && canDelete,
    )
}

private fun groupIdOf(map: MapInfo): MapGroupId = when (map.origin) {
    MapOrigin.SOD_BASE -> MapGroupId.SOD_BASE
    MapOrigin.HOTA_BASE -> MapGroupId.HOTA_BASE
    MapOrigin.USER -> MapGroupId.USER
}

private fun displayNameOf(fileName: String): String = fileName
    .removePrefix(AssetPaths.SOD_MAP_PREFIX)
    .removePrefix(AssetPaths.HOTA_MAP_PREFIX)
    .removeSuffix(MAP_EXTENSION)
