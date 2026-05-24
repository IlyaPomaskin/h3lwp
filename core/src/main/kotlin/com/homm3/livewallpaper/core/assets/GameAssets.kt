package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.I18NBundle
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.h3m.H3mHeaderReader
import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import ktx.assets.async.AssetStorage
import ktx.collections.gdxArrayOf
import ktx.log.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameAssets : Disposable {
    private val storage = AssetStorage(fileResolver = LocalFileHandleResolver()).also {
        it.setLoader<H3mMap> {
            H3mAssetLoader(LocalFileHandleResolver())
        }
    }

    lateinit var skin: Skin
    lateinit var i18n: I18NBundle

    private var registry: SpriteRegistry? = null

    private val lazyEmptyTexture = lazy {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        Texture(pixmap).also { pixmap.dispose() }
    }
    private val emptyTexture: Texture by lazyEmptyTexture
    private val emptyRegion: TextureAtlas.AtlasRegion by lazy {
        TextureAtlas.AtlasRegion(TextureRegion(emptyTexture))
    }

    fun loadUiAssets() {
        skin = Skin(Gdx.files.internal(AssetPaths.SKIN_PATH))
        I18NBundle.setExceptionOnMissingKey(false)
        i18n = I18NBundle.createBundle(Gdx.files.internal(AssetPaths.I18N_PATH))
    }

    fun isGameAssetsLoaded(): Boolean {
        return registry != null
    }

    fun resetRegistry() {
        registry?.dispose()
        registry = null
    }

    fun isLodAvailable(): Boolean {
        return Gdx.files.local(AssetPaths.LOD_FILE).exists()
    }

    fun isHotaAvailable(): Boolean {
        return Gdx.files.local(AssetPaths.HOTA_LOD_FILE).exists()
    }

    fun getAllMapFiles(): List<String> {
        val folder = Gdx.files.local(AssetPaths.USER_MAPS_FOLDER)
        log.info { "Maps folder: ${folder.file().absolutePath}" }
        val hotaAvailable = isHotaAvailable()
        return folder
            .list(".h3m")
            .filter { it.length() > 0L }
            .sortedBy { it.length() }
            .filter { fh ->
                if (hotaAvailable) return@filter true
                val version = H3mHeaderReader.readVersion(fh.read())
                val keep = version != H3mVersion.HOTA
                if (!keep) log.info { "Skipping HotA map (no HotA assets): ${fh.file().name}" }
                keep
            }
            .map { it.file().name }
    }

    data class LoadResult(val maps: List<H3mMap>, val loadedFileNames: List<String>)

    suspend fun loadGameAssets(explicitMaps: List<String> = emptyList()): LoadResult {
        val hotaAvailable = isHotaAvailable()
        val filesToLoad = if (explicitMaps.isNotEmpty()) {
            explicitMaps.filter { fileName ->
                if (hotaAvailable) return@filter true
                val fileHandle = Gdx.files.local("${AssetPaths.USER_MAPS_FOLDER}/$fileName")
                val keep = H3mHeaderReader.readVersion(fileHandle.read()) != H3mVersion.HOTA
                if (!keep) log.info { "Skipping HotA map (no HotA assets): $fileName" }
                keep
            }
        } else {
            // Load maps selected by queue
            val allMapFiles = getAllMapFiles()
            log.info { "Available maps in folder (${allMapFiles.size}): $allMapFiles" }
            MapQueue().currentBatch(allMapFiles)
        }

        val totalStart = System.currentTimeMillis()
        val maps = filesToLoad.map { fileName ->
            val t0 = System.currentTimeMillis()
            val map = storage.load<H3mMap>(fileName)
            log.info { "  loaded map '$fileName' in ${System.currentTimeMillis() - t0}ms" }
            map
        }

        // 2. Load sprites for all maps
        val spritesStart = System.currentTimeMillis()
        loadSpritesForMaps(maps)
        log.info { "loadGameAssets: sprites=${System.currentTimeMillis() - spritesStart}ms, total=${System.currentTimeMillis() - totalStart}ms" }
        return LoadResult(maps, filesToLoad)
    }

    /**
     * Rotates the map queue if at least 3h has passed since the last rotation.
     * Returns the [LoadResult] for the new batch (with sprites preloaded) or
     * `null` if not yet due. Caller is responsible for adding the new maps to
     * the screen and unloading the ones that fell out of the batch.
     */
    suspend fun rotateBatchIfDue(force: Boolean = false): LoadResult? {
        val allMapFiles = getAllMapFiles()
        val newBatch = MapQueue().advanceIfDue(allMapFiles, force) ?: return null
        log.info { "rotateBatchIfDue: new batch = $newBatch (force=$force)" }
        val totalStart = System.currentTimeMillis()
        val maps = newBatch.map { fileName ->
            val t0 = System.currentTimeMillis()
            val map = storage.load<H3mMap>(fileName)
            log.info { "  loaded map '$fileName' in ${System.currentTimeMillis() - t0}ms" }
            map
        }
        val spritesStart = System.currentTimeMillis()
        loadSpritesForMaps(maps)
        log.info { "rotateBatchIfDue: sprites=${System.currentTimeMillis() - spritesStart}ms, total=${System.currentTimeMillis() - totalStart}ms" }
        return LoadResult(maps, newBatch)
    }

    suspend fun loadMapFile(fileName: String): H3mMap {
        return storage.load(fileName)
    }

    /** CPU phase result: a packer with all pixmaps drawn and the region descriptors.
     *  Safe to compute off the render thread — no GL calls happen here. */
    private data class SpriteBundle(val packer: PixmapPacker, val regionInfos: List<RegionInfo>)

    /** Two-phase sprite load:
     *  1. **CPU phase** (off-thread via `Dispatchers.IO`): read lods, parse DEF/PCX,
     *     create `Pixmap`s, pack them into `PixmapPacker` pages.
     *  2. **GL phase** (caller's context — must be the render thread): upload packer
     *     pages to `Texture`s, register `AtlasRegion`s, dispose packer.
     *
     *  Frees the render thread during the heavy CPU work so the previous batch keeps
     *  rendering smoothly until the new atlas is ready. */
    suspend fun loadSpritesForMaps(maps: List<H3mMap>) {
        val reg = registry
        val allNeeded = SpriteCollector(isHotaAvailable()).collectNeededSprites(maps)
        // Filter out sprites already loaded
        val neededSprites = if (reg != null) {
            allNeeded.filterNot { reg.isDefLoaded(it) }.toSet()
        } else {
            allNeeded
        }

        if (neededSprites.isEmpty()) return
        log.info { "Loading ${neededSprites.size} new sprites" }

        val bundle = withContext(Dispatchers.IO) {
            buildSpriteBundle(neededSprites)
        }

        val tReg = System.currentTimeMillis()
        val r = registry
        if (r != null) {
            r.addFromPacker(bundle.packer, bundle.regionInfos)
        } else {
            registry = SpriteRegistry.fromPacker(bundle.packer, bundle.regionInfos)
        }
        bundle.packer.dispose()
        log.info { "  registry build (GL): ${bundle.regionInfos.size} regions in ${System.currentTimeMillis() - tReg}ms" }
    }

    /** CPU-only sprite parsing + packing. No GL calls — safe on any thread. */
    private fun buildSpriteBundle(neededSprites: Set<String>): SpriteBundle {
        val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 2, true)
        val loader = LodSpriteLoader()
        val allRegionInfos = mutableListOf<RegionInfo>()
        val loadedDefNames = mutableSetOf<String>()

        val hotaFile = Gdx.files.local(AssetPaths.HOTA_LOD_FILE)
        if (hotaFile.exists()) {
            val t0 = System.currentTimeMillis()
            val hotaRegions = loader.loadSprites(hotaFile.read(), neededSprites, packer)
            allRegionInfos.addAll(hotaRegions)
            hotaRegions.forEach { loadedDefNames.add(it.packerName.substringBefore("/")) }
            log.info { "  HotA.lod sprites (CPU): ${hotaRegions.size} regions in ${System.currentTimeMillis() - t0}ms" }
        }

        val remaining = neededSprites.filterNot { it in loadedDefNames }.toSet()
        val lodFile = Gdx.files.local(AssetPaths.LOD_FILE)
        if (lodFile.exists()) {
            val t0 = System.currentTimeMillis()
            val h3Regions = loader.loadSprites(lodFile.read(), remaining, packer)
            allRegionInfos.addAll(h3Regions)
            log.info { "  H3sprite.lod sprites (CPU): ${h3Regions.size} regions in ${System.currentTimeMillis() - t0}ms" }
        }

        return SpriteBundle(packer, allRegionInfos)
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        val reg = registry ?: return gdxArrayOf(emptyRegion)
        val frames = reg.getTerrainFrames(defName, index)
        if (frames.isEmpty) {
            log.error { "Can't find terrain $defName/$index" }
            return gdxArrayOf(emptyRegion)
        }
        return frames
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        if (defName.isEmpty()) return gdxArrayOf(emptyRegion)
        val reg = registry ?: return gdxArrayOf(emptyRegion)
        val frames = reg.getObjectFrames(defName)
        if (frames.isEmpty) {
            log.error { "Can't find def $defName" }
            return gdxArrayOf(emptyRegion)
        }
        return frames
    }

    internal fun findAtlasRegions(name: String): Array<TextureAtlas.AtlasRegion> {
        val reg = registry ?: return gdxArrayOf()
        return reg.findRegions(name)
    }

    fun companionFor(color: Texture): Texture? = registry?.companionFor(color)

    override fun dispose() {
        if (::skin.isInitialized) skin.dispose()
        if (lazyEmptyTexture.isInitialized()) emptyTexture.dispose()
        registry?.dispose()
        registry = null
        kotlinx.coroutines.runBlocking { storage.dispose() }
    }

    companion object {
        private val log = logger<GameAssets>()
    }
}
