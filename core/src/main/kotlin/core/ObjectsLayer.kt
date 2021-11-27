package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.parser.formats.H3m
import ktx.collections.gdxArrayOf
import java.util.*

class ObjectsLayer(private val manager: AssetManager, h3mMap: H3m, isUnderground: Boolean) : MapLayer() {
    private val randomizer = ObjectsRandomizer()
    private var visibleSprites: List<Sprite> = mutableListOf()
    private var sprites: List<Sprite> = h3mMap
        .objects
        .sorted()
        // TODO underground rendering
        .filter { it.z == if (isUnderground) 1 else 0 }
        .map { Sprite(it, getObjectFrames(randomizer.replaceRandomObject(it))) }

    init {
        name = "objects"
    }

    private fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
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

    fun updateVisibleSprites(camera: Camera) {
        visibleSprites = sprites.filter { it.inViewport(camera) }
    }

    fun render(batch: Batch) {
        visibleSprites.forEach { it.render(batch, Gdx.graphics.deltaTime) }
    }
}