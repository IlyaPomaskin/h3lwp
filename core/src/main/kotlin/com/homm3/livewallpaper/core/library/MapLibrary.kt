package com.homm3.livewallpaper.core.library

import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.assets.GameDataLocator
import com.homm3.livewallpaper.core.assets.LoadedMap
import com.homm3.livewallpaper.core.assets.MapBatch
import com.homm3.livewallpaper.core.assets.MapBatchLoader
import com.homm3.livewallpaper.core.assets.SpriteSet
import com.homm3.livewallpaper.core.map.GameMap
import ktx.log.logger

/**
 * The loaded maps and the [SpriteSet] they draw from. Every call must come from the rendering thread and
 * the suspending ones from one coroutine at a time (see [SerialCommandQueue]); [maps] is stale after any
 * call that changes the entries, so re-read it.
 */
class MapLibrary(
    private val locator: GameDataLocator,
    private val batchLoader: MapBatchLoader = MapBatchLoader(locator),
) : Disposable {
    private var spriteSet: SpriteSet? = null
    private val entries = LinkedHashMap<String, MapEntry>()
    private var allMapFiles = emptyList<String>()
    private var browseIndex = 0
    private var loadedWithHota: Boolean? = null

    val isLoaded: Boolean get() = spriteSet != null

    val currentSpriteSet: SpriteSet? get() = spriteSet

    /** Every level of every loaded map, in load order. */
    val maps: List<GameMap> get() = entries.values.flatMap { it.levels }

    val hasHotaAvailabilityChanged: Boolean get() = locator.isHotaAvailable() != loadedWithHota

    suspend fun loadInitialBatch(explicitMaps: List<String>) {
        clear()
        allMapFiles = locator.findMapFiles()
        replaceBatch(batchLoader.loadInitialBatch(explicitMaps, allMapFiles))
        loadedWithHota = locator.isHotaAvailable()
    }

    /** Swaps in the next batch when due (or [force]d). Returns whether the maps changed. */
    suspend fun rotateBatchIfDue(force: Boolean): Boolean {
        if (!isLoaded) return false
        val batch = batchLoader.rotateBatchIfDue(locator.findMapFiles(), force) ?: return false
        if (batch.maps.isEmpty()) {
            log.error { "rotateBatchIfDue: no map of the new batch loaded; keeping the current batch" }
            batch.spriteSet.dispose()
            return false
        }
        replaceBatch(batch)
        return true
    }

    /** Rescans the maps folder and drops loaded maps whose files are gone. */
    suspend fun syncWithFolder(): FolderChange {
        val currentFiles = locator.findMapFiles()
        val currentFileSet = currentFiles.toSet()
        val removedFiles = entries.keys.filterNot { it in currentFileSet }.toSet()
        entries.keys.removeAll(removedFiles)
        val previousFileSet = allMapFiles.toSet()
        allMapFiles = currentFiles
        return classifyFolderChange(previousFileSet, currentFileSet, removedFiles)
    }

    /** Loads the maps of the queue's current batch that aren't loaded yet into the current sprite set. */
    suspend fun loadMissingFromCurrentBatch() {
        val currentSpriteSet = spriteSet ?: return
        val maps = batchLoader.loadCurrentBatchInto(
            spriteSet = currentSpriteSet,
            availableFiles = allMapFiles,
            excluded = entries.keys.toSet(),
        )
        addEntries(currentSpriteSet, maps)
        loadedWithHota = locator.isHotaAvailable()
    }

    suspend fun loadNext(direction: Direction): NextMapResult {
        val currentSpriteSet = spriteSet ?: return NextMapResult.NotLoaded
        if (allMapFiles.isEmpty()) return NextMapResult.NotLoaded
        val index = findNextUnloadedIndex(allMapFiles, browseIndex, direction.step) { it in entries }
            ?: return NextMapResult.AllLoaded
        browseIndex = index
        val fileName = allMapFiles[index]
        val maps = batchLoader.loadMapsInto(currentSpriteSet, listOf(fileName))
        if (maps.isEmpty()) return NextMapResult.NotLoaded
        addEntries(currentSpriteSet, maps)
        return NextMapResult.Loaded(fileName)
    }

    /** Drops every map and disposes the sprite set. */
    fun clear() {
        entries.clear()
        spriteSet?.dispose()
        spriteSet = null
    }

    override fun dispose() = clear()

    private fun replaceBatch(batch: MapBatch) {
        val previousSpriteSet = spriteSet
        entries.clear()
        spriteSet = batch.spriteSet
        addEntries(batch.spriteSet, batch.maps)
        previousSpriteSet?.dispose()
    }

    private fun addEntries(entrySpriteSet: SpriteSet, maps: List<LoadedMap>) {
        maps.filterNot { it.fileName in entries }.forEach { map ->
            entries[map.fileName] = MapEntry(entrySpriteSet, map)
        }
    }

    companion object {
        private val log = logger<MapLibrary>()
    }
}

/** One map file and its levels: the surface, plus the underground when the map has one. */
private class MapEntry(spriteSet: SpriteSet, map: LoadedMap) {
    val levels: List<GameMap> = listOfNotNull(
        GameMap(spriteSet, map.h3m, map.fileName, isUnderground = false),
        GameMap(spriteSet, map.h3m, map.fileName, isUnderground = true).takeIf { map.h3m.header.hasUnderground },
    )
}

internal fun classifyFolderChange(
    previousFiles: Set<String>,
    currentFiles: Set<String>,
    removedLoadedFiles: Set<String>,
): FolderChange {
    val isUnchanged = currentFiles == previousFiles && removedLoadedFiles.isEmpty()
    if (isUnchanged) return FolderChange.UNCHANGED
    if (currentFiles.isEmpty()) return FolderChange.EMPTY
    return FolderChange.CHANGED
}

/** Index of the first file after [fromIndex] in [step] direction (wrapping, [fromIndex] last) that isn't loaded. */
internal fun findNextUnloadedIndex(
    files: List<String>,
    fromIndex: Int,
    step: Int,
    isLoaded: (String) -> Boolean,
): Int? {
    if (files.isEmpty()) return null
    return (1..files.size)
        .map { offset -> Math.floorMod(fromIndex + step * offset, files.size) }
        .firstOrNull { index -> !isLoaded(files[index]) }
}
