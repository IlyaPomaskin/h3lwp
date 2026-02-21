package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver
import com.badlogic.gdx.files.FileHandle
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
import ktx.assets.load
import ktx.collections.gdxArrayOf
import ktx.log.logger

class GameAssets : Disposable {
    val manager = AssetManager(InternalFileHandleResolver()).also {
        it.setLoader(
            TextureAtlas::class.java,
            AssetPaths.ATLAS_PATH,
            TextureAtlasLoader(LocalFileHandleResolver())
        )
        it.setLoader(H3mMap::class.java, H3mAssetLoader(LocalFileHandleResolver()))
    }

    lateinit var skin: Skin
    lateinit var i18n: I18NBundle

    private val emptyRegion: TextureAtlas.AtlasRegion by lazy {
        TextureAtlas.AtlasRegion(
            TextureRegion(Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888)))
        )
    }

    private val _loadedMaps: MutableList<H3mMap> = mutableListOf()
    val loadedMaps: List<H3mMap> get() = _loadedMaps

    init {
        Texture.setAssetManager(manager)
    }

    fun loadUiAssets() {
        skin = manager.load<Skin>(AssetPaths.SKIN_PATH).let {
            it.finishLoading()
            it.asset
        }
        i18n = manager.load<I18NBundle>(AssetPaths.I18N_PATH).let {
            I18NBundle.setExceptionOnMissingKey(false)
            it.finishLoading()
            it.asset
        }
    }

    fun isGameAssetsLoaded(): Boolean {
        return manager.isLoaded(AssetPaths.ATLAS_PATH)
    }

    fun isGameAssetsAvailable(): Boolean {
        return Gdx.files.local(AssetPaths.ATLAS_PATH).exists()
    }

    fun loadGameAssets() {
        manager.load<TextureAtlas>(
            AssetPaths.ATLAS_PATH,
            TextureAtlasLoader.TextureAtlasParameter(true)
        )
    }

    fun loadMaps() {
        Gdx.files
            .local(AssetPaths.USER_MAPS_FOLDER)
            .list(".h3m")
            .filter { it.length() > 0L }
            .sortedBy { it.length() }
            .forEach { fileHandle -> loadH3m(fileHandle) }
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        return getFrames("$defName/$index")
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        return getFrames(defName.lowercase().removeSuffix(".def"))
    }

    override fun dispose() {
        manager.dispose()
    }

    private fun loadH3m(fileHandle: FileHandle) {
        val parameters = H3mLoaderParams()
        parameters.loadedCallback = AssetLoaderParameters.LoadedCallback { aManager, fileName, _ ->
            _loadedMaps.add(aManager.get(fileName))
        }
        manager.load(fileHandle.file().name, parameters)
    }

    private fun getFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        return manager
            .get<TextureAtlas>(AssetPaths.ATLAS_PATH)
            .findRegions(defName)
            .let { regions ->
                if (regions.isEmpty) {
                    log.error { "Can't find def $defName" }
                    gdxArrayOf(emptyRegion)
                } else {
                    regions
                }
            }
    }

    companion object {
        private val log = logger<GameAssets>()
    }
}
