package com.heroes3.livewallpaper.core

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import ktx.assets.load
import java.util.*

class Assets {
    private val assetManager = AssetManager()

    init {
        val params = TextureAtlasLoader.TextureAtlasParameter(true)
        assetManager.load<TextureAtlas>("sprites/h3/assets.atlas", params)
        assetManager.finishLoading()
        Texture.setAssetManager(assetManager)
    }

    private fun getAtlasRegions(defName: String): Array<TextureAtlas.AtlasRegion> {
        val regions = assetManager
            .get<TextureAtlas>("sprites/h3/assets.atlas")
            .findRegions(defName)

        if (regions.isEmpty) {
            println("can't find def $defName")
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
}