package com.heroes3.livewallpaper.core

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import com.heroes3.livewallpaper.parser.JsonMapParser
import ktx.graphics.use
import kotlin.math.abs

class ObjectsRenderer(private val engine: Engine, h3mMap: JsonMapParser.ParsedMap) : Disposable {
    class Sprite(
        val animation: Animation<TextureAtlas.AtlasRegion>,
        val x: Float,
        val y: Float
    )

    private val randomizer = ObjectsRandomizer()
    private val batch = SpriteBatch()
    private var viewBounds = Rectangle()
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

    private fun isSpriteInCameraViewport(sprite: Sprite): Boolean {
        val offset = 32 * 5

        val x = sprite.x
        val halfWidth = engine.camera.viewportWidth / 2
        val leftSide = engine.camera.position.x - halfWidth - offset
        val rightSide = engine.camera.position.x + engine.camera.viewportWidth - halfWidth + offset
        val isInViewportByX = leftSide < x && x < rightSide

        val y = sprite.y
        val halfHeight = engine.camera.viewportHeight / 2
        val topSide = engine.camera.position.y - halfHeight - offset
        val bottomSide = engine.camera.position.y + engine.camera.viewportHeight - halfHeight + offset
        val isInViewportByY = topSide < y && y < bottomSide

        return isInViewportByX && isInViewportByY
    }

    private fun setView(camera: OrthographicCamera) {
        batch.projectionMatrix = camera.combined
        val width = camera.viewportWidth * camera.zoom
        val height = camera.viewportHeight * camera.zoom
        val w = width * abs(camera.up.y) + height * abs(camera.up.x)
        val h = height * abs(camera.up.y) + width * abs(camera.up.x)
        viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h)
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
        setView(engine.camera)

        if (stateTime < 0.18f) {
            return
        }

        batch.use { b ->
            sprites.forEach { sprite ->
                if (!isSpriteInCameraViewport(sprite)) {
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