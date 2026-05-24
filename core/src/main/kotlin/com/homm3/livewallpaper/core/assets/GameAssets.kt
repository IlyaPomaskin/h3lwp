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
import com.homm3.livewallpaper.parser.h3m.H3mReader
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

        // One-shot: on first load, scan every map in the folder and pack the union of
        // sprites into the atlas. Subsequent batch rotations hit SpriteRegistry.isDefLoaded
        // and skip re-encoding.
        if (registry == null) {
            val preStart = System.currentTimeMillis()
            preloadAllMapSprites()
            log.info { "loadGameAssets: preloadAllMapSprites=${System.currentTimeMillis() - preStart}ms" }
        }

        val maps = filesToLoad.map { fileName ->
            val t0 = System.currentTimeMillis()
            val map = storage.load<H3mMap>(fileName)
            log.info { "  loaded map '$fileName' in ${System.currentTimeMillis() - t0}ms" }
            map
        }

        // 2. Load sprites for all maps (no-op after preloadAllMapSprites unless a new
        // map appeared between the preload scan and now).
        val spritesStart = System.currentTimeMillis()
        loadSpritesForMaps(maps)
        log.info { "loadGameAssets: sprites=${System.currentTimeMillis() - spritesStart}ms, total=${System.currentTimeMillis() - totalStart}ms" }
        return LoadResult(maps, filesToLoad)
    }

    /**
     * Scan every map file in the user folder, parse each transiently (no AssetStorage
     * caching), and pack the union of every sprite they need into the atlas. Called
     * once at first cold start so batch rotation never has to re-encode the atlas.
     */
    private suspend fun preloadAllMapSprites() {
        val allMapFiles = getAllMapFiles()
        if (allMapFiles.isEmpty()) return

        val needed = withContext(Dispatchers.IO) {
            val maps = allMapFiles.mapNotNull { fileName ->
                val t0 = System.currentTimeMillis()
                try {
                    val fh = Gdx.files.local("${AssetPaths.USER_MAPS_FOLDER}/$fileName")
                    val map = H3mReader(fh.read()).read()
                    log.info { "  preload-parsed '$fileName' in ${System.currentTimeMillis() - t0}ms" }
                    map
                } catch (e: Exception) {
                    log.error { "Skipping unreadable map '$fileName' during sprite preload: ${e.message}" }
                    null
                }
            }
            SpriteCollector(isHotaAvailable()).collectNeededSprites(maps)
        }

        if (needed.isEmpty()) return
        log.info { "preloadAllMapSprites: ${needed.size} unique sprites across ${allMapFiles.size} maps" }

        val bundle = withContext(Dispatchers.IO) { buildEtc1Bundle(needed) }

        val tReg = System.currentTimeMillis()
        registry = SpriteRegistry.fromEtc1(bundle)
        log.info { "  registry build (GL): ${bundle.regionInfos.size} regions in ${System.currentTimeMillis() - tReg}ms" }
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

    /** Two-phase sprite load: CPU encodes RGBA4444 packer pages to ETC1; GL uploads. */
    suspend fun loadSpritesForMaps(maps: List<H3mMap>) {
        val reg = registry
        val allNeeded = SpriteCollector(isHotaAvailable()).collectNeededSprites(maps)
        val neededSprites = if (reg != null) allNeeded.filterNot { reg.isDefLoaded(it) }.toSet() else allNeeded
        if (neededSprites.isEmpty()) return
        log.info { "Loading ${neededSprites.size} new sprites" }

        val bundle = withContext(Dispatchers.IO) { buildEtc1Bundle(neededSprites) }

        val tReg = System.currentTimeMillis()
        val r = registry
        if (r != null) r.addFromEtc1(bundle) else registry = SpriteRegistry.fromEtc1(bundle)
        log.info { "  registry build (GL): ${bundle.regionInfos.size} regions in ${System.currentTimeMillis() - tReg}ms" }
    }

    private fun lodFingerprint(): String {
        val lod = Gdx.files.local(AssetPaths.LOD_FILE).file()
        val hota = Gdx.files.local(AssetPaths.HOTA_LOD_FILE).file()
        fun fp(f: java.io.File) = if (f.exists()) "${f.length()}:${f.lastModified()}" else "absent"
        return "lod=${fp(lod)};hota=${fp(hota)}"
    }

    private fun buildEtc1Bundle(neededSprites: Set<String>): Etc1Bundle {
        val cache = Etc1PageCache()
        val key = cache.cacheKey(neededSprites, lodFingerprint())
        cache.read(key)?.let {
            log.info { "  ETC1 atlas: cache hit key=$key (${it.pages.size} pages)" }
            return it
        }

        val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 4, true)
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

        val packerRects = mutableMapOf<String, PackedRect>()
        packer.pages.forEachIndexed { i, page ->
            page.rects.forEach { entry ->
                val rect = entry.value
                packerRects[entry.key] = PackedRect(i, rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt())
            }
        }

        val tEnc = System.currentTimeMillis()
        val pages = Etc1AtlasEncoder().encodePages(packer)
        log.info { "  ETC1 encode (CPU): ${pages.size} pages in ${System.currentTimeMillis() - tEnc}ms" }
        packer.dispose()

        val bundle = Etc1Bundle(pages, allRegionInfos, packerRects)
        cache.write(key, bundle)
        return bundle
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
