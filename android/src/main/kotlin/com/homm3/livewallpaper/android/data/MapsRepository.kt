package com.homm3.livewallpaper.android.data

import android.content.Context
import android.net.Uri
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.parser.h3m.H3mReader
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

sealed interface MapImportResult {
    data class Success(val mapName: String) : MapImportResult

    sealed interface Failure : MapImportResult
    data object CannotOpen : Failure
    data object TooSmall : Failure
    data class UnknownVersion(val versionHex: String) : Failure
    data class TooLarge(val size: Int, val maxSize: Int) : Failure
    data class Invalid(val details: String) : Failure
}

class MapsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val mapsDir = appContext.filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)

    private val mutableMaps = MutableStateFlow<List<String>>(emptyList())
    val maps: StateFlow<List<String>> = mutableMaps.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutableMaps.value = readMapNames()
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
        mutableMaps.value = readMapNames()
        MapImportResult.Success(mapName)
    }

    suspend fun delete(mapName: String) = withContext(Dispatchers.IO) {
        val isKnownMap = mapName in readMapNames()
        if (!isKnownMap) return@withContext

        mapsDir.resolve(mapName).delete()
        mutableMaps.value = readMapNames()
    }

    private fun readMapNames(): List<String> =
        mapsDir.listFiles { file -> file.extension.equals(MAP_EXTENSION, ignoreCase = true) }
            ?.map { it.name }
            ?.sorted()
            .orEmpty()

    private fun validate(bytes: ByteArray): MapImportResult.Failure? {
        val decompressed = try {
            GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
        } catch (e: IOException) {
            return MapImportResult.Invalid(e.message.orEmpty())
        }
        if (decompressed.size < VERSION_HEADER_SIZE) return MapImportResult.TooSmall

        val version = ByteBuffer.wrap(decompressed, 0, VERSION_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).int
        val isKnownVersion = H3mVersion.entries.any { it.value == version }
        if (!isKnownVersion) return MapImportResult.UnknownVersion(version.toString(HEX_RADIX))

        val map = try {
            H3mReader(bytes.inputStream()).read()
        } catch (e: Exception) {
            return MapImportResult.Invalid(e.message.orEmpty())
        }
        val size = map.header.size
        if (size > GameConfig.MAX_MAP_SIZE) return MapImportResult.TooLarge(size, GameConfig.MAX_MAP_SIZE)
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
