package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
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
import com.homm3.livewallpaper.parser.h3m.H3mMap
import ktx.assets.async.AssetStorage
import ktx.collections.gdxArrayOf
import ktx.log.logger
import kotlinx.coroutines.runBlocking

class GameAssets : Disposable {
    private val storage = AssetStorage(fileResolver = LocalFileHandleResolver()).also {
        it.setLoader<TextureAtlas> {
            TextureAtlasLoader(LocalFileHandleResolver())
        }
        it.setLoader<H3mMap> {
            H3mAssetLoader(LocalFileHandleResolver())
        }
    }

    lateinit var skin: Skin
    lateinit var i18n: I18NBundle

    private lateinit var atlas: TextureAtlas
    private var hotaAtlas: TextureAtlas? = null

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
        return storage.isLoaded<TextureAtlas>(AssetPaths.ATLAS_PATH)
    }

    fun isGameAssetsAvailable(): Boolean {
        return Gdx.files.local(AssetPaths.ATLAS_PATH).exists()
    }

    fun isHotaAssetsAvailable(): Boolean {
        return Gdx.files.local(AssetPaths.HOTA_ATLAS_PATH).exists()
    }

    suspend fun loadGameAssets(): List<H3mMap> {
        atlas = storage.load<TextureAtlas>(
            AssetPaths.ATLAS_PATH,
            TextureAtlasLoader.TextureAtlasParameter(true)
        )

        if (isHotaAssetsAvailable()) {
            hotaAtlas = storage.load<TextureAtlas>(
                AssetPaths.HOTA_ATLAS_PATH,
                TextureAtlasLoader.TextureAtlasParameter(true)
            )
        }

        val mapFiles = Gdx.files.local(AssetPaths.USER_MAPS_FOLDER)
            .list(".h3m")
            .filter { it.length() > 0L }
            .sortedBy { it.length() }
        return mapFiles.map { storage.load<H3mMap>(it.file().name) }
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        return getFrames("$defName/$index")
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        return getFrames(defName.lowercase().removeSuffix(".def"))
    }

    internal fun findAtlasRegions(name: String): Array<TextureAtlas.AtlasRegion> {
        val regions = atlas.findRegions(name)
        if (regions.isEmpty) {
            val hotaRegions = hotaAtlas?.findRegions(name)
            if (hotaRegions != null && !hotaRegions.isEmpty) return hotaRegions
        }
        return regions
    }

    override fun dispose() {
        if (::skin.isInitialized) skin.dispose()
        if (lazyEmptyTexture.isInitialized()) emptyTexture.dispose()
        hotaAtlas = null
        runBlocking { storage.dispose() }
    }

    private fun getFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        if (defName.isEmpty()) return gdxArrayOf(emptyRegion)

        val regions = atlas.findRegions(defName)
        if (!regions.isEmpty) return regions

        val hotaRegions = hotaAtlas?.findRegions(defName)
        if (hotaRegions != null && !hotaRegions.isEmpty) return hotaRegions

        log.error { "Can't find def $defName" }
        return gdxArrayOf(emptyRegion)
    }

    companion object {
        private val log = logger<GameAssets>()
    }
}
