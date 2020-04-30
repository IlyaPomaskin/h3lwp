package com.heroes3.livewallpaper.core

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import com.heroes3.livewallpaper.parser.JsonMapParser
import ktx.graphics.use

class ObjectsRenderer(private val engine: Engine, h3mMap: JsonMapParser.ParsedMap) : Disposable {
    class Sprite(
        val animation: Animation<TextureAtlas.AtlasRegion>,
        val x: Float,
        val y: Float
    )

    private val randomizer = ObjectsRandomizer()
    private val batch = SpriteBatch()
    private var sprites: List<Sprite> = h3mMap
        .objects
        .sorted()
        .asReversed()
        .filter { it.z == 0 }
        .map {
            val spriteName = randomizer.replaceRandomObject(it)
            val frames = engine.assets.getObjectFrames(spriteName)
            Sprite(
                Animation(0.18f, frames),
                ((it.x + 1) * 32).toFloat(),
                ((it.y + 1) * 32).toFloat()
            )
        }

    private fun isSpriteInCameraViewport(camera: OrthographicCamera, sprite: Sprite): Boolean {
        val offset = 32 * 5

        val x = sprite.x
        val halfWidth = camera.viewportWidth / 2
        val leftSide = camera.position.x - halfWidth - offset
        val rightSide = camera.position.x + camera.viewportWidth - halfWidth + offset
        val isInViewportByX = leftSide < x && x < rightSide

        val y = sprite.y
        val halfHeight = camera.viewportHeight / 2
        val topSide = camera.position.y - halfHeight - offset
        val bottomSide = camera.position.y + camera.viewportHeight - halfHeight + offset
        val isInViewportByY = topSide < y && y < bottomSide

        return isInViewportByX && isInViewportByY
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

        if (stateTime < 0.18f) {
            return
        }

        batch.use(engine.camera) { b ->
            sprites.forEach { sprite ->
                if (!isSpriteInCameraViewport(engine.camera, sprite)) {
                    return@forEach
                }

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