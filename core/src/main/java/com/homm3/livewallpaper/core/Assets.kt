package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.I18NBundle
import com.homm3.livewallpaper.parser.formats.H3m
import ktx.assets.load
import ktx.collections.gdxArrayOf

class Assets {
    val manager = AssetManager(InternalFileHandleResolver()).also {
        it.setLoader(H3m::class.java, H3mLoader(it.fileHandleResolver))
    }
    lateinit var skin: Skin
    lateinit var i18n: I18NBundle

    init {
        Texture.setAssetManager(manager)
    }

    fun loadUiAssets() {
        skin = manager.load<Skin>(Constants.Assets.SKIN_PATH).let {
            it.finishLoading()
            it.asset
        }
        i18n = manager.load<I18NBundle>(Constants.Assets.I18N_PATH).let {
            I18NBundle.setExceptionOnMissingKey(false)
            it.finishLoading()
            it.asset
        }
    }

    fun isGameAssetsLoaded(): Boolean {
        return manager.isLoaded(Constants.Assets.ATLAS_PATH)
    }

    fun isGameAssetsAvailable(): Boolean {
        val isAssetsReady = Gdx.app
            .getPreferences(Constants.Preferences.PREFERENCES_NAME)
            .getBoolean(Constants.Preferences.IS_ASSETS_READY_KEY)
        val filesExists = Gdx.files.local(Constants.Assets.ATLAS_PATH).exists()

        return isAssetsReady && filesExists
    }

    fun loadGameAssets() {
        manager
            .load<TextureAtlas>(
                Constants.Assets.ATLAS_PATH,
                TextureAtlasLoader.TextureAtlasParameter(true)
            )
    }

    fun loadMaps(onMapLoaded: (h3m: H3m) -> Unit) {
        Gdx.files
            .internal("maps")
            .list(".h3m")
            .filter { it.length() > 0L }
            .sortedBy { it.length() }
            .forEach { fileHandle -> loadH3m(fileHandle, onMapLoaded) }
    }

    private fun loadH3m(fileHandle: FileHandle, onLoad: (h3m: H3m) -> Unit) {
        val parameters = H3mLoaderParams()
        parameters.loadedCallback = AssetLoaderParameters.LoadedCallback { aManager, fileName, _ ->
            onLoad(aManager.get(fileName))
        }

        manager.load(fileHandle.file().toString(), parameters)
    }

    private fun getFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        manager
            .get<TextureAtlas>(Constants.Assets.ATLAS_PATH)
            .findRegions(defName)
            .run {
                return if (isEmpty) {
                    println("Can't find terrain def $defName")
                    return gdxArrayOf(Constants.Assets.emptyPixmap)
                } else {
                    this
                }
            }
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        return getFrames("$defName/$index")
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        return getFrames(defName.lowercase().removeSuffix(".def"))
    }
}