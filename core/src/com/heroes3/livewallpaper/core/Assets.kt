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
        val params = TextureAtlasLoader.TextureAtlasParameter(true);
        assetManager.load<TextureAtlas>("sprites/h3/objects.atlas", params)
        assetManager.load<TextureAtlas>("sprites/h3/terrains.atlas", params)
        assetManager.finishLoading()
        Texture.setAssetManager(assetManager)
    }

    private fun getAtlasRegions(atlasName: String, defName: String): Array<TextureAtlas.AtlasRegion> {
        val regions = assetManager
            .get<TextureAtlas>(atlasName)
            .findRegions(defName)

        if (regions.isEmpty) {
            println("atlas $atlasName don't have $defName")
            return Array()
        }

        return regions
    }

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        return getAtlasRegions("sprites/h3/objects.atlas", defName.toLowerCase(Locale.ROOT).removeSuffix(".def"))
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> {
        return getAtlasRegions("sprites/h3/terrains.atlas", String.format("%s/%02d", defName, index))
    }
}