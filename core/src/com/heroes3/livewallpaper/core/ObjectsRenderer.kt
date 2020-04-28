package com.heroes3.livewallpaper.core

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Disposable
import com.heroes3.livewallpaper.parser.JsonMapParser
import ktx.graphics.use

class ObjectsRenderer(private val engine: Engine, private val h3mMap: JsonMapParser.ParsedMap) : Disposable {
    class Sprite(
        val animation: Animation<TextureAtlas.AtlasRegion>,
        val x: Float,
        val y: Float
    )

    private val randomizer = ObjectsRandomizer()
    private val batch = SpriteBatch()
    private var sprites: List<Sprite> = emptyList()

    fun updateVisibleSprites() {
        // TODO make 2d array, filter in render method like terrain renderer do, remove this method
        sprites = h3mMap
            .objects
            .sorted()
            .asReversed()
            .filter {
                val offset = 32 * 5
                val isUnderground = it.z == 0

                val x = it.x * 32
                val halfWidth = engine.camera.viewportWidth / 2
                val leftSide = engine.camera.position.x - halfWidth - offset
                val rightSide = engine.camera.position.x + engine.camera.viewportWidth - halfWidth + offset
                val isInViewportByX =  leftSide < x && x < rightSide

                val y = it.y * 32
                val halfHeight = engine.camera.viewportHeight / 2
                val topSide = engine.camera.position.y - halfHeight - offset
                val bottomSide = engine.camera.position.y + engine.camera.viewportHeight - halfHeight + offset
                val isInViewportByY = topSide < y && y < bottomSide

                isUnderground && isInViewportByX && isInViewportByY
            }
            .map {
                val spriteName = randomizer.replaceRandomObject(it)
                val frames = engine.assets.getObjectFrames(spriteName)
                Sprite(
                    Animation(0.18f, frames),
                    ((it.x + 1) * 32).toFloat(),
                    ((it.y + 1) * 32).toFloat()
                )
            }
    }

    private fun getFrameX(frame: TextureAtlas.AtlasRegion, x: Float): Float {
        return x + frame.offsetX - frame.originalWidth
    }

    private fun getFrameY(frame: TextureAtlas.AtlasRegion, y: Float): Float {
        return y - frame.packedHeight
    }

    private var stateTime = 0f

    fun render(delta: Float) {
        stateTime += delta
        batch.use(engine.camera) { b ->
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

    override fun dispose() {
        sprites = emptyList()
        batch.dispose()
    }
}