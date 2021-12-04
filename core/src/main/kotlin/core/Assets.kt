package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.I18NBundle
import com.homm3.livewallpaper.parser.formats.H3m
import ktx.assets.load
import ktx.collections.gdxArrayOf
import java.util.*

class Assets {
    val manager = AssetManager(InternalFileHandleResolver()).also {
        it.setLoader(H3m::class.java, H3mLoader(it.fileHandleResolver))
    }
    private val internalManager = AssetManager(InternalFileHandleResolver())
    lateinit var skin: Skin
    lateinit var i18n: I18NBundle

    init {
        Texture.setAssetManager(manager)
    }

    fun loadUiAssets() {
        skin = internalManager.load<Skin>(Constants.Assets.SKIN_PATH).let {
            it.finishLoading()
            it.asset
        }
        i18n = internalManager.load<I18NBundle>(Constants.Assets.I18N_PATH).let {
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

        manager.finishLoading()
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        manager
            .get<TextureAtlas>(Constants.Assets.ATLAS_PATH)
            .findRegions("$defName/$index")
            .run {
                return if (isEmpty) {
                    println("Can't find terrain def $defName/$index")
                    return gdxArrayOf(Constants.Assets.emptyPixmap)
                } else {
                    this
                }
            }
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        val name = defName.toLowerCase(Locale.ROOT).removeSuffix(".def");

        manager
            .get<TextureAtlas>(Constants.Assets.ATLAS_PATH)
            .findRegions(name)
            .run {
                return if (isEmpty) {
                    println("Can't find objects def $name")
                    return gdxArrayOf(Constants.Assets.emptyPixmap)
                } else {
                    this
                }
            }
    }
}