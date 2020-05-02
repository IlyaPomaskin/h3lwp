package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import ktx.collections.gdxArrayOf
import java.util.*

class Assets {
    companion object {
        const val atlasFolder = "sprites"
        const val atlasName = "images"
        const val atlasPath = "$atlasFolder/$atlasName.atlas"
        const val skinPath = "skin/uiskin.json"
    }

    private val emptyPixmap = TextureAtlas.AtlasRegion(
        TextureRegion(
            Texture(
                Pixmap(0, 0, Pixmap.Format.RGBA8888)
            )
        )
    )
    val manager = AssetManager(LocalFileHandleResolver())

    init {
        Texture.setAssetManager(manager)
    }

    private fun getAtlasRegions(defName: String): Array<TextureAtlas.AtlasRegion> {
        val regions = manager
            .get<TextureAtlas>(atlasPath)
            .findRegions(defName)

        if (regions.isEmpty) {
            println("Can't find def $defName")
            return gdxArrayOf(emptyPixmap)
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
        return Skin(Gdx.files.internal(skinPath))
    }
}