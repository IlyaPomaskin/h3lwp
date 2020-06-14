package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapObject
import com.homm3.livewallpaper.parser.formats.H3m

class ObjectsLayer(private val engine: Engine, h3mMap: H3m) : MapLayer() {
    private val randomizer = ObjectsRandomizer()
    private var visibleSprites: List<Sprite> = mutableListOf()
    private var sprites: List<Sprite> = h3mMap
        .objects
        .sorted()
        // TODO underground rendering
        .filter { it.z == 0 }
        .map { Sprite(it, engine.assets.getObjectFrames(randomizer.replaceRandomObject(it))) }

    init {
        name = "objects"
    }

    fun updateVisibleSprites(camera: Camera) {
        visibleSprites = sprites.filter { it.inViewport(camera, Constants.VISIBLE_OBJECTS_OFFSET) }
    }

    fun render(batch: Batch) {
        visibleSprites.forEach { it.render(batch, Gdx.graphics.deltaTime) }
    }
}