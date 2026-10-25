package com.homm3.livewallpaper.android.data

import android.content.Context
import android.net.Uri
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.parser.h3m.H3mHeaderReader
import com.homm3.livewallpaper.parser.h3m.H3mReader
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import com.homm3.livewallpaper.parser.h3m.readUnpackedH3m
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface MapImportResult {
    data class Success(val mapName: String) : MapImportResult

    sealed interface Failure : MapImportResult
    data object CannotOpen : Failure
    data object TooSmall : Failure
    data class UnknownVersion(val versionHex: String) : Failure
    data object UnsupportedVersion : Failure
    data class Invalid(val details: String) : Failure
}

enum class MapOrigin { SOD_BASE, HOTA_BASE, USER }

data class MapInfo(val fileName: String, val version: H3mVersion, val origin: MapOrigin, val size: Int)

class MapsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val mapsDir = appContext.filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)
    private val disabledMapsFile = appContext.filesDir.resolve(AssetPaths.DISABLED_MAPS_FILE)
    private val disabledMapsLock = Mutex()

    private val mutableMaps = MutableStateFlow<List<MapInfo>>(emptyList())
    val maps: StateFlow<List<MapInfo>> = mutableMaps.asStateFlow()

    private val mutableDisabledMaps = MutableStateFlow<Set<String>>(emptySet())
    val disabledMaps: StateFlow<Set<String>> = mutableDisabledMaps.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutableDisabledMaps.value = readDisabledMaps()
        mutableMaps.value = readMaps()
    }

    suspend fun setMapsEnabled(fileNames: Collection<String>, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        disabledMapsLock.withLock {
            val disabled = readDisabledMaps()
            val disabledMaps = if (isEnabled) disabled - fileNames.toSet() else disabled + fileNames
            disabledMapsFile.writeText(disabledMaps.joinToString("\n"))
            mutableDisabledMaps.value = disabledMaps
        }
    }

    suspend fun import(uri: Uri): MapImportResult = withContext(Dispatchers.IO) {
        val bytes = try {
            appContext.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: IOException) {
            null
        } ?: return@withContext MapImportResult.CannotOpen

        val failure = validate(bytes)
        if (failure != null) return@withContext failure

        val mapName = toMapFileName(appContext.queryDisplayName(uri))
        mapsDir.mkdirs()
        mapsDir.resolve(mapName).writeBytes(bytes)
        mutableMaps.value = readMaps()
        MapImportResult.Success(mapName)
    }

    suspend fun delete(mapName: String) = withContext(Dispatchers.IO) {
        val isKnownMap = mapName in readMapNames()
        if (!isKnownMap) return@withContext

        mapsDir.resolve(mapName).delete()
        mutableMaps.value = readMaps()
        setMapsEnabled(listOf(mapName), isEnabled = true)
    }

    private fun readMapNames(): List<String> = readMapFiles().map { it.name }

    private fun readMapFiles(): List<File> =
        mapsDir.listFiles { file -> file.extension.equals(MAP_EXTENSION, ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()

    private fun readMaps(): List<MapInfo> = readMapFiles().mapNotNull { file ->
        val version = file.inputStream().use { H3mHeaderReader.readVersion(it) }
        val isSupportedVersion = version != null && version in GameConfig.SUPPORTED_MAP_VERSIONS
        if (!isSupportedVersion) return@mapNotNull null
        val size = file.inputStream().use { H3mHeaderReader.readMapSize(it) }
        MapInfo(fileName = file.name, version = version, origin = originOf(file.name), size = size)
    }

    private fun originOf(fileName: String): MapOrigin = when {
        fileName.startsWith(AssetPaths.HOTA_MAP_PREFIX) -> MapOrigin.HOTA_BASE
        fileName.startsWith(AssetPaths.SOD_MAP_PREFIX) -> MapOrigin.SOD_BASE
        else -> MapOrigin.USER
    }

    private fun readDisabledMaps(): Set<String> {
        if (!disabledMapsFile.exists()) return emptySet()
        return disabledMapsFile.readLines().filter { it.isNotBlank() }.toSet()
    }

    private fun validate(bytes: ByteArray): MapImportResult.Failure? {
        val decompressed = try {
            bytes.inputStream().use { it.readUnpackedH3m() }
        } catch (e: IOException) {
            return MapImportResult.Invalid(e.message.orEmpty())
        }
        if (decompressed.size < VERSION_HEADER_SIZE) return MapImportResult.TooSmall

        val version = ByteBuffer.wrap(decompressed, 0, VERSION_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).int
        val knownVersion = H3mVersion.entries.find { it.value == version }
            ?: return MapImportResult.UnknownVersion(version.toString(HEX_RADIX))
        val isSupportedVersion = knownVersion in GameConfig.SUPPORTED_MAP_VERSIONS
        if (!isSupportedVersion) return MapImportResult.UnsupportedVersion

        try {
            H3mReader.read(bytes.inputStream())
        } catch (e: Exception) {
            return MapImportResult.Invalid(e.message.orEmpty())
        }
        return null
    }

    private fun toMapFileName(displayName: String?): String {
        val baseName = displayName?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: DEFAULT_MAP_NAME
        val hasMapExtension = baseName.endsWith(".$MAP_EXTENSION", ignoreCase = true)
        if (hasMapExtension) return baseName
        return "$baseName.$MAP_EXTENSION"
    }

    private companion object {
        const val MAP_EXTENSION = "h3m"
        const val DEFAULT_MAP_NAME = "map.h3m"
        const val VERSION_HEADER_SIZE = 4
        const val HEX_RADIX = 16
    }
}
