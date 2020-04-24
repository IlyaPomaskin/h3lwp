package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import ktx.assets.load
import java.util.*

class Assets {
    companion object {
        const val atlasFolder = "sprites/h3/"
        const val atlasName = "assets"
        const val atlasPath = "$atlasFolder/$atlasName.atlas"
    }

    private val assetManager = AssetManager()

    init {
        Texture.setAssetManager(assetManager)
    }

    fun loadAtlas() {
        val params = TextureAtlasLoader.TextureAtlasParameter(true)
        assetManager.load<TextureAtlas>(atlasPath, params)
        assetManager.finishLoading()
    }

    fun isReady(): Boolean {
        return Gdx.files.local(atlasPath).exists()
    }

    private fun getAtlasRegions(defName: String): Array<TextureAtlas.AtlasRegion> {
        val regions = assetManager
            .get<TextureAtlas>(atlasPath)
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