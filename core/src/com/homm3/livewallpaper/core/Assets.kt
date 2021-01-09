package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.I18NBundle
import ktx.assets.load

class Assets {
    val manager = AssetManager(LocalFileHandleResolver()).also {
        it.setLoader(H3mLayer::class.java, H3mLayerLoader(it.fileHandleResolver))
    }
    private val internalManager = AssetManager(InternalFileHandleResolver())
    val skin = internalManager.load<Skin>(Constants.Assets.SKIN_PATH).let {
        it.finishLoading()
        it.asset
    }
    val i18n = internalManager.load<I18NBundle>(Constants.Assets.I18N_PATH).let {
        I18NBundle.setExceptionOnMissingKey(false)
        it.finishLoading()
        it.asset
    }

    init {
        Texture.setAssetManager(manager)
    }

    fun isWallpaperAssetsLoaded(): Boolean {
        return manager.isLoaded(Constants.Assets.ATLAS_PATH)
    }

    private fun canLoadWallpapersAssets(): Boolean {
        val isAssetsReady = Gdx.app
            .getPreferences(Constants.Preferences.PREFERENCES_NAME)
            .getBoolean(Constants.Preferences.IS_ASSETS_READY_KEY)
        val filesExists = Gdx.files.local(Constants.Assets.ATLAS_PATH).exists()

        return isAssetsReady && filesExists && !isWallpaperAssetsLoaded()
    }

    fun tryLoadWallpaperAssets() {
        if (!manager.contains(Constants.Assets.ATLAS_PATH) && canLoadWallpapersAssets()) {
            manager.load<TextureAtlas>(
                Constants.Assets.ATLAS_PATH,
                TextureAtlasLoader.TextureAtlasParameter(true)
            )
        }
    }
}