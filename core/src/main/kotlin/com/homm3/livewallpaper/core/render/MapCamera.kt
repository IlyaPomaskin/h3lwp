package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.homm3.livewallpaper.core.GameConfig
import kotlin.random.Random

class MapCamera : OrthographicCamera() {
    private val cameraPoint = Vector2()

    init {
        setToOrtho(true)
    }

    fun randomizePosition(mapSize: Int) {
        val mapSizePixels = mapSize * GameConfig.TILE_SIZE

        val halfWidth = viewportWidth / 2
        val xStart = halfWidth
        val xEnd = mapSizePixels - xStart - halfWidth
        val nextCameraX = xStart + Random.nextFloat() * xEnd

        val halfHeight = viewportHeight / 2
        val yStart = halfHeight
        val yEnd = mapSizePixels - yStart - halfHeight
        val nextCameraY = yStart + Random.nextFloat() * yEnd

        cameraPoint.set(nextCameraX, nextCameraY)
        position.set(cameraPoint, 0f)
        update()
    }

    fun moveByScrollOffset(offset: Float) {
        position.x = cameraPoint.x + offset * GameConfig.SCROLL_OFFSET
    }
}
