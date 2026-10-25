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
private val ORIGIN_PREFIXES = listOf(AssetPaths.SOD_MAP_PREFIX, AssetPaths.HOTA_MAP_PREFIX)
private val SIZE_LABELS = mapOf(
    36 to "S",
    72 to "M",
    108 to "L",
    144 to "XL",
    180 to "H",
    216 to "XH",
    252 to "G",
)

enum class MapGroupId { SOD_BASE, HOTA_BASE, USER }

data class MapItem(
    val fileName: String,
    val displayName: String,
    val isEnabled: Boolean,
    val isMissingAssets: Boolean,
    val isInvalid: Boolean,
    val canDelete: Boolean,
    val canToggle: Boolean,
) {
    val isAvailable: Boolean get() = !isMissingAssets && !isInvalid
    val isActive: Boolean get() = isAvailable && isEnabled
}

data class MapGroup(val id: MapGroupId, val maps: List<MapItem>, val activeMapCount: Int) {
    private val availableMaps: List<MapItem> get() = maps.filter { it.isAvailable }

    val isAvailable: Boolean get() = availableMaps.isNotEmpty()
    val isMissingAssets: Boolean get() = maps.isNotEmpty() && maps.all { it.isMissingAssets }
    val isEnabled: Boolean get() = availableMaps.any { it.isEnabled }
    val enabledCount: Int get() = availableMaps.count { it.isEnabled }
    val availableFileNames: List<String> get() = availableMaps.map { it.fileName }
    val canToggle: Boolean get() = isAvailable && (!isEnabled || enabledCount < activeMapCount)
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
        val isHotaInstalled = gameDataStatus.isHotaInstalled
        val activeMapCount = maps.count { it.fileName !in disabledMaps && isAvailable(it, isHotaInstalled) }
        val context = MapItemContext(disabledMaps, isHotaInstalled, activeMapCount)
        val items = maps.associateWith { map -> toMapItem(map, context) }
        val mapsByGroup = maps.sortedWith(MAP_ORDER).groupBy(::groupIdOf)
        val groups = MapGroupId.entries
            .map { groupId -> MapGroup(groupId, mapsByGroup[groupId].orEmpty().map(items::getValue), activeMapCount) }
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

private class MapItemContext(val disabledMaps: Set<String>, val isHotaInstalled: Boolean, val activeMapCount: Int)

private fun toMapItem(map: MapInfo, context: MapItemContext): MapItem {
    val isUserMap = map.origin == MapOrigin.USER
    val isEnabled = map.fileName !in context.disabledMaps
    val isMissingAssets = isMissingAssets(map, context.isHotaInstalled)
    val isAvailable = isAvailable(map, context.isHotaInstalled)
    val isLastActiveMap = isEnabled && isAvailable && context.activeMapCount <= 1
    return MapItem(
        fileName = map.fileName,
        displayName = displayNameOf(map),
        isEnabled = isEnabled,
        isMissingAssets = isMissingAssets,
        isInvalid = map.isInvalid,
        canDelete = isUserMap && !isLastActiveMap,
        canToggle = isAvailable && !isLastActiveMap,
    )
}

private fun isAvailable(map: MapInfo, isHotaInstalled: Boolean): Boolean =
    !isMissingAssets(map, isHotaInstalled) && !map.isInvalid

private fun isMissingAssets(map: MapInfo, isHotaInstalled: Boolean): Boolean =
    !isHotaInstalled && map.version == H3mVersion.HOTA

private fun groupIdOf(map: MapInfo): MapGroupId = when (map.origin) {
    MapOrigin.SOD_BASE -> MapGroupId.SOD_BASE
    MapOrigin.HOTA_BASE -> MapGroupId.HOTA_BASE
    MapOrigin.USER -> MapGroupId.USER
}

private val MAP_ORDER = compareBy<MapInfo> { it.size }.thenBy(String.CASE_INSENSITIVE_ORDER, ::baseNameOf)

private fun displayNameOf(map: MapInfo): String {
    val name = baseNameOf(map)
    val sizeLabel = SIZE_LABELS[map.size] ?: return name
    return "[$sizeLabel] $name"
}

private fun baseNameOf(map: MapInfo): String = stripOriginPrefixes(map.fileName).removeSuffix(MAP_EXTENSION)

private tailrec fun stripOriginPrefixes(fileName: String): String {
    val prefix = ORIGIN_PREFIXES.find(fileName::startsWith) ?: return fileName
    return stripOriginPrefixes(fileName.removePrefix(prefix))
}
