package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.parser.h3m.H3mHeaderReader
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import ktx.log.logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Blocking map-folder scan, meant to run off the GL thread. Map versions and sizes are cached by
 * (name, length, lastModified), so a rescan only opens files that are new or changed.
 */
class MapFileScanner(
    private val readVersion: (File) -> H3mVersion? = ::readVersionFromFile,
    private val readSize: (File) -> Int = ::readSizeFromFile,
) {
    private data class MapFileKey(val name: String, val length: Long, val lastModified: Long)

    private class CachedVersion(val version: H3mVersion?)

    private class MapFile(val file: File) {
        val key = MapFileKey(name = file.name, length = file.length(), lastModified = file.lastModified())
    }

    private val versionCache = ConcurrentHashMap<MapFileKey, CachedVersion>()
    private val sizeCache = ConcurrentHashMap<MapFileKey, Int>()

    /** Non-empty `.h3m` files in [folder], smallest first; oversized maps, and HotA maps unless [includeHota], dropped. */
    fun listMapFiles(folder: File, includeHota: Boolean): List<String> {
        val mapFiles = folder.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(MAP_EXTENSION) }
            .map(::MapFile)
            .filter { it.key.length > 0L }
        val currentKeys = mapFiles.map { it.key }.toSet()
        versionCache.keys.retainAll(currentKeys)
        sizeCache.keys.retainAll(currentKeys)
        return mapFiles
            .sortedBy { it.key.length }
            .filter { isSupported(it, includeHota) }
            .map { it.key.name }
    }

    /** Keeps the [fileNames] in [folder] that can be shown, preserving their order. */
    fun filterSupported(folder: File, fileNames: List<String>, includeHota: Boolean): List<String> =
        fileNames.filter { fileName -> isSupported(MapFile(File(folder, fileName)), includeHota) }

    private fun isSupported(mapFile: MapFile, includeHota: Boolean): Boolean {
        val isHiddenHotaMap = !includeHota && versionOf(mapFile) == H3mVersion.HOTA
        if (isHiddenHotaMap) {
            log.info { "Skipping HotA map (no HotA assets): ${mapFile.key.name}" }
            return false
        }

        val size = sizeOf(mapFile)
        val isOversized = size > GameConfig.MAX_MAP_SIZE
        if (isOversized) log.info { "Skipping map larger than ${GameConfig.MAX_MAP_SIZE}: ${mapFile.key.name} ($size)" }
        return !isOversized
    }

    private fun sizeOf(mapFile: MapFile): Int = sizeCache.getOrPut(mapFile.key) { readSize(mapFile.file) }

    private fun versionOf(mapFile: MapFile): H3mVersion? =
        versionCache.getOrPut(mapFile.key) { CachedVersion(readVersion(mapFile.file)) }.version

    companion object {
        private val log = logger<MapFileScanner>()
        const val MAP_EXTENSION = ".h3m"

        private fun readVersionFromFile(file: File): H3mVersion? {
            if (!file.isFile) return null
            return file.inputStream().use { H3mHeaderReader.readVersion(it) }
        }

        private fun readSizeFromFile(file: File): Int = file.inputStream().use { H3mHeaderReader.readMapSize(it) }
    }
}
