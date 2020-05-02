package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.parser.formats.JsonMap
import ktx.graphics.use

class ObjectsRenderer(private val engine: Engine, h3mMap: JsonMap.ParsedMap) : Disposable {
    private val randomizer = ObjectsRandomizer()
    private val batch = SpriteBatch()
    private var sprites: List<Sprite> = h3mMap
        .objects
        .sorted()
        .asReversed()
        .filter { it.z == 0 }
        .map { Sprite(it, engine.assets.getObjectFrames(randomizer.replaceRandomObject(it))) }

    fun render(delta: Float) {
        batch.use(engine.camera) { b ->
            sprites.forEach { sprite ->
                if (!sprite.inViewport(engine.camera)) {
                    return@forEach
                }

                sprite.render(b, delta)
            }
        }
    }

    override fun dispose() {
        sprites = emptyList()
        batch.dispose()
    }
}