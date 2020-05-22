package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.parser.formats.H3m
import com.homm3.livewallpaper.parser.formats.H3mObjects
import ktx.graphics.use

class ObjectsRenderer(private val engine: Engine, h3mMap: H3m) : Disposable {
    private val randomizer = ObjectsRandomizer()
    private val batch = SpriteBatch()
    private val camera = engine.camera
    private var sprites: List<Sprite> = h3mMap
        .objects
        .sorted()
        // TODO underground rendering
        .filter { it.z == 0 }
        .map { Sprite(it, engine.assets.getObjectFrames(randomizer.replaceRandomObject(it))) }
    private var visibleSprites: List<Sprite> = mutableListOf()

    fun updateVisibleSprites() {
        visibleSprites = sprites.filter { it.inViewport(camera) }
    }

    fun render(delta: Float) {
        batch.use(camera) { b ->
            visibleSprites.forEach { it.render(b, delta) }
        }
    }

    override fun dispose() {
        sprites = emptyList()
        batch.dispose()
    }
}