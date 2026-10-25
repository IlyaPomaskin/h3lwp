package com.homm3.livewallpaper.core.library

import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.assets.GameDataLocator
import com.homm3.livewallpaper.core.assets.MapDefListStore
import com.homm3.livewallpaper.core.assets.MapValidity
import com.homm3.livewallpaper.core.assets.SpriteSet
import com.homm3.livewallpaper.core.map.GameMap
import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ktx.log.logger
import kotlin.random.Random

/**
 * The one loaded map and the [SpriteSet] it draws from, which holds every DEF the candidate maps use, so changing
 * the map loads no sprites. Every call must come from the rendering thread and the suspending ones from one
 * coroutine at a time (see [SerialCommandQueue]); [maps] is stale after any call that changes the map, so re-read it.
 */
class MapLibrary(
    private val locator: GameDataLocator,
    private val defListStore: MapDefListStore = MapDefListStore(locator),
    private val random: Random = Random.Default,
) : Disposable {
    private var spriteSet: SpriteSet? = null
    private var entry: MapEntry? = null
    private var explicitMaps = emptyList<String>()
    private var candidateFiles = emptyList<String>()
    private var loadedWithHota: Boolean? = null

    val isLoaded: Boolean get() = spriteSet != null

    val hasMap: Boolean get() = entry != null

    val currentSpriteSet: SpriteSet? get() = spriteSet

    /** The levels of the loaded map: the surface, plus the underground when the map has one. */
    val maps: List<GameMap> get() = entry?.levels.orEmpty()

    val hasHotaAvailabilityChanged: Boolean get() = locator.isHotaAvailable() != loadedWithHota

    /** Loads the sprites of every candidate map and one random map; [explicitMaps] limit the candidates if given. */
    suspend fun loadInitialMap(explicitMaps: List<String>) {
        clear()
        this.explicitMaps = explicitMaps
        candidateFiles = findCandidateFiles()
        val newSpriteSet = SpriteSet(locator.spriteSources(), hasHotaAssets = locator.isHotaAvailable())
        spriteSet = newSpriteSet
        loadCandidateDefs(newSpriteSet)
        loadRandomMap()
        loadedWithHota = locator.isHotaAvailable()
    }

    /** Replaces the map with a random other candidate. Returns whether a map was loaded. */
    suspend fun loadRandomMap(): Boolean {
        if (!isLoaded) return false
        val loaded = randomLoadOrder(candidateFiles, entry?.fileName, random).firstNotNullOfOrNull { loadEntry(it) }
        if (loaded == null) {
            log.error { "loadRandomMap: no map of ${candidateFiles.size} candidates loaded; keeping the current map" }
            return false
        }
        entry = loaded
        return true
    }

    /** Rescans the candidates and drops the map when its file is gone; on a change, loads new maps' sprites. */
    suspend fun syncWithFolder(): FolderChange {
        val previousFiles = candidateFiles.toSet()
        candidateFiles = findCandidateFiles()
        val currentFiles = candidateFiles.toSet()
        val removedFile = entry?.fileName?.takeIf { it !in currentFiles }
        if (removedFile != null) entry = null

        val change = classifyFolderChange(previousFiles, currentFiles, setOfNotNull(removedFile))
        val currentSpriteSet = spriteSet
        if (change != FolderChange.UNCHANGED && currentSpriteSet != null) loadCandidateDefs(currentSpriteSet)
        return change
    }

    /** Loads the candidate file next to the current one in [direction] in its place. */
    suspend fun loadNext(direction: Direction): NextMapResult {
        if (!isLoaded || candidateFiles.isEmpty()) return NextMapResult.NotLoaded
        val currentIndex = candidateFiles.indexOf(entry?.fileName)
        val fileName = candidateFiles[Math.floorMod(currentIndex + direction.step, candidateFiles.size)]
        val loaded = loadEntry(fileName) ?: return NextMapResult.NotLoaded
        entry = loaded
        return NextMapResult.Loaded(fileName)
    }

    /** Drops the map and disposes the sprite set. */
    fun clear() {
        entry = null
        spriteSet?.dispose()
        spriteSet = null
    }

    override fun dispose() = clear()

    private suspend fun findCandidateFiles(): List<String> {
        if (explicitMaps.isNotEmpty()) return locator.filterSupportedMaps(explicitMaps)
        return locator.findMapFiles()
    }

    private suspend fun loadCandidateDefs(target: SpriteSet) {
        target.loadDefs(defListStore.update(candidateFiles, target.hasHotaAssets).defNames)
    }

    private suspend fun loadEntry(fileName: String): MapEntry? {
        val currentSpriteSet = spriteSet ?: return null
        val h3m = parseMapOrNull(fileName) ?: return null
        return MapEntry(currentSpriteSet, fileName, h3m)
    }

    /** A map that fails to parse is dropped from the candidates, so this session doesn't try it again. */
    private suspend fun parseMapOrNull(fileName: String): H3mMap? {
        val start = System.currentTimeMillis()
        val map = withContext(Dispatchers.IO) { readMapOrNull(fileName) }
        if (map == null) {
            candidateFiles = candidateFiles - fileName
            return null
        }
        log.debug { "Parsed map '$fileName' in ${System.currentTimeMillis() - start}ms" }
        return map
    }

    /** Blocking; records a map that fails to parse as invalid. */
    private fun readMapOrNull(fileName: String): H3mMap? {
        val fileHandle = locator.mapFile(fileName)
        return try {
            fileHandle.read().use { H3mReader.read(it) }
        } catch (e: Exception) {
            log.error(e) { "Skipping map '$fileName': failed to load" }
            locator.mapValidityStore().record(fileHandle.file(), MapValidity.INVALID)
            null
        }
    }

    companion object {
        private val log = logger<MapLibrary>()
    }
}

/** One map file and its levels: the surface, plus the underground when the map has one. */
private class MapEntry(spriteSet: SpriteSet, val fileName: String, h3m: H3mMap) {
    val levels: List<GameMap> = listOfNotNull(
        GameMap(spriteSet, h3m, fileName, isUnderground = false),
        GameMap(spriteSet, h3m, fileName, isUnderground = true).takeIf { h3m.header.hasUnderground },
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

/** [files] in random order with [currentFile] last, so a different map is tried first and it is only the fallback. */
internal fun randomLoadOrder(files: List<String>, currentFile: String?, random: Random): List<String> {
    val others = files.filterNot { it == currentFile }.shuffled(random)
    val isCurrentCandidate = currentFile != null && currentFile in files
    if (!isCurrentCandidate) return others
    return others + currentFile
}
