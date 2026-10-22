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

        // One-shot at startup: load every DEF listed in def-list.txt, pack & ETC1-encode
        // them, then never load sprites on the fly. Map rotation / next-map only swap
        // H3mMap instances; the sprite registry is frozen after this.
        if (registry == null) {
            val preStart = System.currentTimeMillis()
            preloadDefList()
            log.info { "loadGameAssets: preloadDefList=${System.currentTimeMillis() - preStart}ms" }
        }

        val maps = filesToLoad.map { fileName ->
            val t0 = System.currentTimeMillis()
            val map = storage.load<H3mMap>(fileName)
            log.info { "  loaded map '$fileName' in ${System.currentTimeMillis() - t0}ms" }
            map
        }

        warnMissingSprites(maps)
        log.info { "loadGameAssets: total=${System.currentTimeMillis() - totalStart}ms" }
        return LoadResult(maps, filesToLoad)
    }

    /**
     * Read def-list.txt, build the ETC1 atlas from those DEFs, and freeze the sprite
     * registry. If the file is missing (fresh install), scan every map once to derive
     * the list and write it out — so the next startup is a cache hit.
     */
    private suspend fun preloadDefList() {
        val listFile = Gdx.files.local("def-list.txt")
        val needed: Set<String> = if (listFile.exists()) {
            val entries = listFile.readString()
                .splitToSequence('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            log.info { "preloadDefList: ${entries.size} entries from ${listFile.file().absolutePath}" }
            entries
        } else {
            log.info { "preloadDefList: def-list.txt missing — scanning maps to seed it" }
            scanMapsForSprites().also { derived ->
                if (derived.isNotEmpty()) {
                    listFile.writeString(derived.sorted().joinToString("\n"), false)
                    log.info { "preloadDefList: wrote ${derived.size} entries to ${listFile.file().absolutePath}" }
                }
            }
        }

        if (needed.isEmpty()) return
        val bundle = withContext(Dispatchers.IO) { buildEtc1Bundle(needed) }

        val tReg = System.currentTimeMillis()
        registry = SpriteRegistry.fromEtc1(bundle)
        log.info { "  registry build (GL): ${bundle.regionInfos.size} regions in ${System.currentTimeMillis() - tReg}ms" }
    }

    private suspend fun scanMapsForSprites(): Set<String> {
        val allMapFiles = getAllMapFiles()
        if (allMapFiles.isEmpty()) return emptySet()
        // Stream maps one at a time: parse → collect sprite names → drop the map.
        // Holding 50+ H3mMap instances simultaneously is enough to OOM on Android heap.
        return withContext(Dispatchers.IO) {
            val collector = SpriteCollector(isHotaAvailable())
            val acc = mutableSetOf<String>()
            for (fileName in allMapFiles) {
                try {
                    val fh = Gdx.files.local("${AssetPaths.USER_MAPS_FOLDER}/$fileName")
                    val map = H3mReader(fh.read()).read()
                    acc.addAll(collector.collectNeededSprites(listOf(map)))
                } catch (e: Exception) {
                    log.error { "Skipping unreadable map '$fileName' during sprite scan: ${e.message}" }
                }
            }
            acc
        }
    }

    private fun warnMissingSprites(maps: List<H3mMap>) {
        val reg = registry ?: return
        val needed = SpriteCollector(isHotaAvailable()).collectNeededSprites(maps)
        val missing = needed.filterNot { reg.isDefLoaded(it) }
        if (missing.isNotEmpty()) {
            log.error { "${missing.size} DEFs needed by current maps are missing from def-list.txt (will render empty): $missing" }
        }
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
        warnMissingSprites(maps)
        log.info { "rotateBatchIfDue: total=${System.currentTimeMillis() - totalStart}ms" }
        return LoadResult(maps, newBatch)
    }

    suspend fun loadMapFile(fileName: String): H3mMap {
        return storage.load(fileName)
    }

    /**
     * No-op since the sprite atlas is fully built from def-list.txt at startup
     * and frozen for the session. Kept so callers (Engine.loadNextMap) don't break;
     * logs an error if a map needs DEFs that weren't in the preload set.
     */
    @Suppress("RedundantSuspendModifier")
    suspend fun loadSpritesForMaps(maps: List<H3mMap>) {
        warnMissingSprites(maps)
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
            h3Regions.forEach { loadedDefNames.add(it.packerName.substringBefore("/")) }
            log.info { "  H3sprite.lod sprites (CPU): ${h3Regions.size} regions in ${System.currentTimeMillis() - t0}ms" }
        }
        val notFound = neededSprites.filterNot { it in loadedDefNames }
        if (notFound.isNotEmpty()) {
            log.error { "${notFound.size} sprites not found in HotA.lod or H3sprite.lod: $notFound" }
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
