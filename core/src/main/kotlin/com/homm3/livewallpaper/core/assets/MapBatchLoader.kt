package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ktx.log.logger

/**
 * Picks batches from the [MapQueue] and parses their maps on [Dispatchers.IO], skipping broken ones. Sprites are
 * not loaded here: the library preloads every DEF the maps folder uses.
 */
class MapBatchLoader(
    private val locator: GameDataLocator,
    private val mapQueue: MapQueue = MapQueue(),
) {
    /** The batch to show on start: [explicitMaps] if given, otherwise the queue's current batch of [availableFiles]. */
    suspend fun loadInitialBatch(explicitMaps: List<String>, availableFiles: List<String>): List<LoadedMap> {
        val fileNames = if (explicitMaps.isNotEmpty()) {
            locator.filterSupportedMaps(explicitMaps)
        } else {
            log.info { "Available maps in folder (${availableFiles.size}): $availableFiles" }
            mapQueue.currentBatch(availableFiles)
        }
        return parseMaps(fileNames)
    }

    /** The maps of the queue's next batch. */
    suspend fun loadNextBatch(availableFiles: List<String>): List<LoadedMap> =
        parseMaps(mapQueue.advance(availableFiles))

    /** The maps of the queue's current batch that aren't in [excluded]. */
    suspend fun loadCurrentBatch(availableFiles: List<String>, excluded: Set<String>): List<LoadedMap> =
        parseMaps(mapQueue.currentBatch(availableFiles).filterNot { it in excluded })

    suspend fun parseMaps(fileNames: List<String>): List<LoadedMap> =
        fileNames.mapNotNull { fileName -> parseMapOrNull(fileName)?.let { LoadedMap(fileName, it) } }

    private suspend fun parseMapOrNull(fileName: String): H3mMap? = try {
        val start = System.currentTimeMillis()
        val fileHandle = locator.mapFile(fileName)
        withContext(Dispatchers.IO) { fileHandle.read().use { H3mReader(it).read() } }.also {
            log.debug { "Parsed map '$fileName' in ${System.currentTimeMillis() - start}ms" }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.error(e) { "Skipping map '$fileName': failed to load" }
        null
    }

    companion object {
        private val log = logger<MapBatchLoader>()
    }
}
