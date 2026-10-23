package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ktx.log.logger

/**
 * Parses maps on [Dispatchers.IO] and loads their sprites. Call from a coroutine on the GL thread
 * (KtxAsync), since sprite sets create and upload textures.
 */
class MapBatchLoader(
    private val locator: GameDataLocator,
    private val mapQueue: MapQueue = MapQueue(),
) {

    /** The batch to show on start: [explicitMaps] if given, otherwise the queue's current batch of [availableFiles]. */
    suspend fun loadInitialBatch(explicitMaps: List<String>, availableFiles: List<String>): MapBatch {
        val fileNames = if (explicitMaps.isNotEmpty()) {
            locator.filterSupportedMaps(explicitMaps)
        } else {
            log.info { "Available maps in folder (${availableFiles.size}): $availableFiles" }
            mapQueue.currentBatch(availableFiles)
        }
        return loadBatch(fileNames)
    }

    /** A new batch with its own sprite set if the queue rotation is due (or [force]d), otherwise `null`. */
    suspend fun rotateBatchIfDue(availableFiles: List<String>, force: Boolean = false): MapBatch? {
        val fileNames = mapQueue.advanceIfDue(availableFiles, force) ?: return null
        log.info { "rotateBatchIfDue: new batch = $fileNames (force=$force)" }
        return loadBatch(fileNames)
    }

    /** Maps of the queue's current batch that aren't in [excluded], with sprites added to [spriteSet]. */
    suspend fun loadCurrentBatchInto(
        spriteSet: SpriteSet,
        availableFiles: List<String>,
        excluded: Set<String>,
    ): List<LoadedMap> {
        val fileNames = mapQueue.currentBatch(availableFiles).filterNot { it in excluded }
        return loadMapsInto(spriteSet, fileNames)
    }

    /** Parses [fileNames] (skipping broken ones) and adds their sprites to [spriteSet]. */
    suspend fun loadMapsInto(spriteSet: SpriteSet, fileNames: List<String>): List<LoadedMap> {
        val maps = parseMaps(fileNames)
        spriteSet.loadSpritesFor(maps.map { it.h3m })
        return maps
    }

    private suspend fun loadBatch(fileNames: List<String>): MapBatch {
        val totalStart = System.currentTimeMillis()
        val maps = parseMaps(fileNames)
        val spriteSet = SpriteSet(locator.spriteSources(), hasHotaAssets = locator.isHotaAvailable())
        try {
            spriteSet.loadSpritesFor(maps.map { it.h3m })
        } catch (e: Throwable) {
            spriteSet.dispose()
            throw e
        }
        log.info { "loadBatch: ${maps.size}/${fileNames.size} maps in ${System.currentTimeMillis() - totalStart}ms" }
        return MapBatch(maps, spriteSet)
    }

    private suspend fun parseMaps(fileNames: List<String>): List<LoadedMap> =
        fileNames.mapNotNull { fileName -> parseMapOrNull(fileName)?.let { LoadedMap(fileName, it) } }

    private suspend fun parseMapOrNull(fileName: String): H3mMap? = try {
        val start = System.currentTimeMillis()
        val fileHandle = locator.mapFile(fileName)
        withContext(Dispatchers.IO) { fileHandle.read().use { H3mReader(it).read() } }.also {
            log.info { "  loaded map '$fileName' in ${System.currentTimeMillis() - start}ms" }
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
