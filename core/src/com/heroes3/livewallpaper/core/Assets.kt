package com.heroes3.livewallpaper.core

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import ktx.assets.getAsset
import ktx.assets.load
import ktx.assets.loadAsset
import java.util.*

class Assets {
    companion object {
        const val atlasFolder = "sprites"
        const val atlasName = "images"
        const val atlasPath = "$atlasFolder/$atlasName.atlas"
        const val skinPath = "skin/uiskin.json"
    }

    val manager = AssetManager()

    init {
        Texture.setAssetManager(manager)
    }

    private fun getAtlasRegions(defName: String): Array<TextureAtlas.AtlasRegion> {
        val regions = manager
            .get<TextureAtlas>(atlasPath)
            .findRegions(defName)

        if (regions.isEmpty) {
            println("Can't find def $defName")
            return Array()
        }

        return regions
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        return getAtlasRegions(defName.toLowerCase(Locale.ROOT).removeSuffix(".def"))
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        return getAtlasRegions("$defName/$index")
    }

    fun loadSkin(): Skin {
        manager.load<Skin>(skinPath).finishLoading()
        return manager.get(skinPath)
    }
}