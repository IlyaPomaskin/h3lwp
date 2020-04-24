package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import ktx.graphics.use

class ObjectsRenderer(
    private val assets: Assets,
    private val camera: OrthographicCamera,
    h3mMap: JsonMapParser.ParsedMap
) {
    class Sprite(
        val animation: Animation<TextureAtlas.AtlasRegion>,
        val x: Float,
        val y: Float
    )

    private val randomizer = ObjectsRandomizer()
    private val batch = SpriteBatch()
    private var sprites = h3mMap
        .objects
        .sorted()
        .asReversed()
        .filter { it.z == 0 && it.x <= 50 && it.y <= 50 }
        .map {
            val spriteName = randomizer.replaceRandomObject(it)
            val frames = assets.getObjectFrames(spriteName)
            Sprite(
                Animation(0.18f, frames),
                ((it.x + 1) * 32).toFloat(),
                ((it.y + 1) * 32).toFloat()
            )
        }

    private fun getFrameX(frame: TextureAtlas.AtlasRegion, x: Float): Float {
        return x + frame.offsetX - frame.originalWidth
    }

    private fun getFrameY(frame: TextureAtlas.AtlasRegion, y: Float): Float {
        return y - frame.packedHeight
    }

    private var stateTime = 0f

    fun render() {
        stateTime += Gdx.graphics.deltaTime
        batch.use(camera) { b ->
            sprites.forEach { sprite ->
                val frame = sprite.animation.getKeyFrame(stateTime, true)
                b.draw(
                    frame,
                    getFrameX(frame, sprite.x),
                    getFrameY(frame, sprite.y)
                )
            }
        }
    }
}