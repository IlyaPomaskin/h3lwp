package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.I18NBundle
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.assets.indexed.IndexedSpriteLoader
import com.homm3.livewallpaper.core.assets.indexed.IndexedSpriteRegistry
import com.homm3.livewallpaper.core.assets.indexed.LodSource
import com.homm3.livewallpaper.parser.h3m.H3mHeaderReader
import com.homm3.livewallpaper.parser.lod.FileLodDataSource
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

    private var registry: IndexedSpriteRegistry? = null
    private val notFoundSprites = mutableSetOf<String>()

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
        notFoundSprites.clear()
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
        val maps = loadMaps(filesToLoad)
        loadSpritesForMaps(maps)
        log.info { "loadGameAssets: total=${System.currentTimeMillis() - totalStart}ms" }
        return LoadResult(maps, filesToLoad)
    }

    /**
     * Rotates the map queue if at least 3h has passed since the last rotation.
     * Returns the [LoadResult] for the new batch (with sprites loaded) or
     * `null` if not yet due. Caller is responsible for adding the new maps to
     * the screen and unloading the ones that fell out of the batch.
     */
    suspend fun rotateBatchIfDue(force: Boolean = false): LoadResult? {
        val allMapFiles = getAllMapFiles()
        val newBatch = MapQueue().advanceIfDue(allMapFiles, force) ?: return null
        log.info { "rotateBatchIfDue: new batch = $newBatch (force=$force)" }
        val totalStart = System.currentTimeMillis()
        val maps = loadMaps(newBatch)
        loadSpritesForMaps(maps)
        log.info { "rotateBatchIfDue: total=${System.currentTimeMillis() - totalStart}ms" }
        return LoadResult(maps, newBatch)
    }

    private suspend fun loadMaps(fileNames: List<String>): List<H3mMap> = fileNames.map { fileName ->
        val t0 = System.currentTimeMillis()
        val map = storage.load<H3mMap>(fileName)
        log.info { "  loaded map '$fileName' in ${System.currentTimeMillis() - t0}ms" }
        map
    }

    suspend fun loadMapFile(fileName: String): H3mMap {
        return storage.load(fileName)
    }

    /** Loads sprites the maps need that aren't in the registry yet; CPU decodes index pages, GL uploads. */
    suspend fun loadSpritesForMaps(maps: List<H3mMap>) {
        val needed = SpriteCollector(isHotaAvailable()).collectNeededSprites(maps)
        val toLoad = needed.filterNot { isSpriteLoaded(it) || it in notFoundSprites }.toSet()
        if (toLoad.isEmpty()) return
        log.info { "Loading ${toLoad.size} new sprites" }

        val tDecode = System.currentTimeMillis()
        val bundle = withContext(Dispatchers.IO) { IndexedSpriteLoader().load(lodSources(), toLoad) }
        val regionCount = bundle.regions.size
        log.info { "  decode+pack (CPU): $regionCount regions, ${bundle.pages.size} pages, ${bundle.palettes.size} palettes in ${System.currentTimeMillis() - tDecode}ms" }

        val tReg = System.currentTimeMillis()
        val currentRegistry = registry
        val targetRegistry = currentRegistry ?: IndexedSpriteRegistry().also { registry = it }
        targetRegistry.add(bundle)
        log.info { "  registry build (GL): $regionCount regions in ${System.currentTimeMillis() - tReg}ms" }

        val notFound = toLoad.filterNot { isSpriteLoaded(it) }
        if (notFound.isNotEmpty()) {
            notFoundSprites.addAll(notFound)
            log.error { "${notFound.size} sprites not found in HotA.lod or H3sprite.lod: $notFound" }
        }
    }

    private fun isSpriteLoaded(defName: String): Boolean = registry?.isDefLoaded(defName) ?: false

    private fun lodSources(): List<LodSource> = listOf(
        AssetPaths.HOTA_LOD_FILE to SpriteArchive.HOTA,
        AssetPaths.LOD_FILE to SpriteArchive.H3SPRITE,
    ).mapNotNull { (path, archive) ->
        val file = Gdx.files.local(path)
        if (!file.exists()) return@mapNotNull null
        LodSource(path, open = { FileLodDataSource(file.file()) }, aliases = DefAliases.targetsIn(archive))
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        val reg = registry ?: return gdxArrayOf(emptyRegion)
        val frames = reg.getTerrainFrames(defName, index)
        return if (frames.isEmpty) gdxArrayOf(emptyRegion) else frames
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        if (defName.isEmpty()) return gdxArrayOf(emptyRegion)
        val reg = registry ?: return gdxArrayOf(emptyRegion)
        val frames = reg.getObjectFrames(defName)
        return if (frames.isEmpty) gdxArrayOf(emptyRegion) else frames
    }

    internal fun findAtlasRegions(name: String): Array<TextureAtlas.AtlasRegion> {
        val reg = registry ?: return gdxArrayOf()
        return reg.findRegions(name)
    }

    fun paletteTexture(): Texture? = registry?.paletteTexture

    fun updatePalettes(delta: Float) {
        registry?.update(delta)
    }

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
