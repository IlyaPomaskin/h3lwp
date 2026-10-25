package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.h3m.H3mReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ktx.log.logger

/**
 * Keeps [MapDefList] in two append-only files: [AssetPaths.MAP_DEFS_FILE] (one DEF name per line) and
 * [AssetPaths.SCANNED_MAPS_FILE] (one [MapFileStamp] per line). [update] scans only maps not scanned yet and
 * appends what they add. File work runs on [Dispatchers.IO]; call [update] from one coroutine at a time.
 */
class MapDefListStore(private val locator: GameDataLocator) {
    private var list: MapDefList? = null

    suspend fun update(fileNames: List<String>, hasHotaAssets: Boolean): MapDefList = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val current = list ?: readSaved()
        val collector = SpriteCollector(hasHotaAssets)
        val stamps = fileNames.map { stampOf(it, hasHotaAssets) }
        val additions = current.additionsFor(stamps) { stamp -> collectDefs(stamp, collector) }
        if (!additions.isEmpty) append(additions)
        val updated = current + additions
        list = updated
        log.info {
            "Map DEF list: ${updated.defNames.size} DEFs (+${additions.defNames.size}), " +
                "scanned ${additions.scannedMaps.size} new maps in ${System.currentTimeMillis() - start}ms"
        }
        updated
    }

    private fun stampOf(fileName: String, hasHotaAssets: Boolean): MapFileStamp {
        val file = locator.mapFile(fileName).file()
        return MapFileStamp(fileName, file.length(), file.lastModified(), hasHotaAssets)
    }

    private fun collectDefs(stamp: MapFileStamp, collector: SpriteCollector): Set<String>? {
        val mapFile = locator.mapFile(stamp.fileName).file()
        val validityStore = locator.mapValidityStore()
        val isKnownInvalid = validityStore.knownValidities(listOf(mapFile))[stamp.fileName] == MapValidity.INVALID
        if (isKnownInvalid) return null

        return try {
            val map = mapFile.inputStream().use { H3mReader.read(it) }
            validityStore.record(mapFile, MapValidity.VALID)
            collector.collectNeededSprites(listOf(map))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Map DEF list: skipping '${stamp.fileName}', failed to read" }
            validityStore.record(mapFile, MapValidity.INVALID)
            null
        }
    }

    private fun readSaved(): MapDefList {
        val defNames = readLines(AssetPaths.MAP_DEFS_FILE).toSet()
        val scannedMaps = readLines(AssetPaths.SCANNED_MAPS_FILE).mapNotNull(MapFileStamp::parse).toSet()
        return MapDefList(defNames, scannedMaps)
    }

    private fun readLines(path: String): List<String> {
        val file = Gdx.files.local(path)
        if (!file.exists()) return emptyList()
        return file.readString(CHARSET).lines().filter { it.isNotEmpty() }
    }

    /** DEFs first: a crash in between leaves maps unmarked, so they're scanned again instead of DEFs going missing. */
    private fun append(additions: MapDefList) {
        appendLines(AssetPaths.MAP_DEFS_FILE, additions.defNames.sorted())
        appendLines(AssetPaths.SCANNED_MAPS_FILE, additions.scannedMaps.map { it.serialize() })
    }

    private fun appendLines(path: String, lines: List<String>) {
        if (lines.isEmpty()) return
        Gdx.files.local(path).writeString(lines.joinToString(separator = "\n", postfix = "\n"), true, CHARSET)
    }

    companion object {
        private val log = logger<MapDefListStore>()
        private val CHARSET = Charsets.UTF_8.name()
    }
}
