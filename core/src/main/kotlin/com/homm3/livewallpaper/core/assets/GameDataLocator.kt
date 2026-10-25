package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.assets.indexed.LodSource
import com.homm3.livewallpaper.parser.lod.FileLodDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ktx.log.logger
import java.io.File

/** Where the converted game data lives: LOD archives and user maps. Folder scans run on [Dispatchers.IO]. */
class GameDataLocator(private val scanner: MapFileScanner = MapFileScanner()) {

    fun isLodAvailable(): Boolean = Gdx.files.local(AssetPaths.LOD_FILE).exists()

    fun isHotaAvailable(): Boolean = Gdx.files.local(AssetPaths.HOTA_LOD_FILE).exists()

    fun mapFile(fileName: String): FileHandle = Gdx.files.local("${AssetPaths.USER_MAPS_FOLDER}/$fileName")

    fun mapValidityStore(): MapValidityStore = MapValidityStore(Gdx.files.local(AssetPaths.MAP_VALIDITY_FILE).file())

    /** Map file names in rotation that can be shown with the installed assets and aren't invalid, smallest first. */
    suspend fun findMapFiles(): List<String> {
        val folder = mapsFolder()
        val includeHota = isHotaAvailable()
        log.debug { "Maps folder: ${folder.absolutePath}" }
        return withContext(Dispatchers.IO) {
            val mapFiles = excludeInvalidMaps(folder, scanner.listMapFiles(folder, includeHota))
            excludeDisabledMaps(mapFiles, readDisabledMaps())
        }
    }

    /** Drops the [fileNames] that need assets which aren't installed or are invalid. */
    suspend fun filterSupportedMaps(fileNames: List<String>): List<String> {
        val folder = mapsFolder()
        val includeHota = isHotaAvailable()
        return withContext(Dispatchers.IO) {
            excludeInvalidMaps(folder, scanner.filterSupported(folder, fileNames, includeHota))
        }
    }

    /** Installed sprite archives in lookup order (HotA first). */
    fun spriteSources(): List<LodSource> = listOf(
        AssetPaths.HOTA_LOD_FILE to SpriteArchive.HOTA,
        AssetPaths.LOD_FILE to SpriteArchive.H3SPRITE,
    ).mapNotNull { (path, archive) ->
        val file = Gdx.files.local(path)
        if (!file.exists()) return@mapNotNull null
        LodSource(path, open = { FileLodDataSource(file.file()) }, aliases = DefAliases.targetsIn(archive))
    }

    private fun mapsFolder() = Gdx.files.local(AssetPaths.USER_MAPS_FOLDER).file()

    private fun excludeInvalidMaps(folder: File, fileNames: List<String>): List<String> {
        val validities = mapValidityStore().knownValidities(fileNames.map { File(folder, it) })
        return fileNames.filterNot { validities[it] == MapValidity.INVALID }
    }

    private fun readDisabledMaps(): Set<String> {
        val file = Gdx.files.local(AssetPaths.DISABLED_MAPS_FILE)
        if (!file.exists()) return emptySet()
        return file.readString(Charsets.UTF_8.name()).lines().filter { it.isNotBlank() }.toSet()
    }

    companion object {
        private val log = logger<GameDataLocator>()
    }
}

/** [mapFiles] without the [disabledMaps]; all of them when every map is disabled, so the wallpaper still has a map. */
internal fun excludeDisabledMaps(mapFiles: List<String>, disabledMaps: Set<String>): List<String> {
    val enabledMaps = mapFiles.filterNot { it in disabledMaps }
    if (enabledMaps.isEmpty()) return mapFiles
    return enabledMaps
}
