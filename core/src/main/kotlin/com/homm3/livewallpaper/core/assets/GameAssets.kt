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
import ktx.assets.async.AssetStorage
import ktx.collections.gdxArrayOf
import ktx.log.logger

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

    fun isLodAvailable(): Boolean {
        return Gdx.files.local(AssetPaths.LOD_FILE).exists()
    }

    suspend fun loadGameAssets(): List<H3mMap> {
        // 1. Load maps
        val mapFiles = Gdx.files.local(AssetPaths.USER_MAPS_FOLDER)
            .list(".h3m")
            .filter { it.length() > 0L }
            .sortedBy { it.length() }

        val sizeCache = mutableMapOf<String, Int>()
        val mapQueue = MapQueue()
        val filesToLoad = mapQueue.getMapsForToday(
            mapFiles.map { it.file().name }
        ) { fileName ->
            sizeCache.getOrPut(fileName) {
                val fileHandle = Gdx.files.local("${AssetPaths.USER_MAPS_FOLDER}/$fileName")
                H3mHeaderReader.readMapSize(fileHandle.read())
            }
        }

        val maps = filesToLoad.map { storage.load<H3mMap>(it) }

        // 2. Collect needed sprite names from maps
        val neededSprites = SpriteCollector().collectNeededSprites(maps)
        log.info { "Need ${neededSprites.size} unique sprites" }

        // 3. Load sprites from LOD files (HotA first — its sprites take priority)
        val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)
        val loader = LodSpriteLoader()
        val allRegionInfos = mutableListOf<RegionInfo>()
        val loadedDefNames = mutableSetOf<String>()

        val hotaFile = Gdx.files.local(AssetPaths.HOTA_LOD_FILE)
        if (hotaFile.exists()) {
            val hotaRegions = loader.loadSprites(hotaFile.read(), neededSprites, packer)
            allRegionInfos.addAll(hotaRegions)
            hotaRegions.forEach { loadedDefNames.add(it.packerName.substringBefore("/")) }
        }

        // Search H3sprite.lod for sprites not found in HotA
        val remaining = neededSprites.filterNot { it in loadedDefNames }.toSet()
        val lodFile = Gdx.files.local(AssetPaths.LOD_FILE)
        if (lodFile.exists()) {
            allRegionInfos.addAll(loader.loadSprites(lodFile.read(), remaining, packer))
        }

        // 4. Build sprite registry
        registry = SpriteRegistry.fromPacker(packer, allRegionInfos)
        packer.dispose()

        log.info { "Sprite registry built" }
        return maps
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
