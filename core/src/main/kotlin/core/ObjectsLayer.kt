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

class ObjectsLayer(private val assets: Assets, h3mMap: H3m, isUnderground: Boolean) : MapLayer() {
    private val randomizer = ObjectsRandomizer()
    private var visibleSprites: List<Sprite> = mutableListOf()
    private var sprites = h3mMap
        .objects
        .sorted()
        // TODO underground rendering
        .filter { it.z == if (isUnderground) 1 else 0 }
        .map { Sprite(it, assets.getObjectFrames(randomizer.replaceRandomObject(it))) }

    init {
        name = "objects"
    }

    fun updateVisibleSprites(camera: Camera) {
        visibleSprites = sprites
            .filter { it.inViewport(camera) }

        println("visibleSprites ${visibleSprites.size}")
    }

    fun render(batch: Batch) {
        visibleSprites.forEach { it.render(batch, Gdx.graphics.deltaTime) }
    }
}