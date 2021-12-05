package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

class Camera : OrthographicCamera() {
    private val cameraPoint = Vector2()

    init {
        setToOrtho(true)
    }

    fun randomizeCameraPosition(mapSize: Int) {
        val mapSizeFloat = mapSize * Constants.TILE_SIZE

        val halfWidth = viewportWidth / 2
        val xStart = halfWidth
        val xEnd = mapSizeFloat - xStart - halfWidth
        val nextCameraX = xStart + Random.nextFloat() * xEnd

        val halfHeight = viewportHeight / 2
        val yStart = halfHeight
        val yEnd = mapSizeFloat - yStart - halfHeight
        val nextCameraY = yStart + Random.nextFloat() * yEnd

        cameraPoint.set(nextCameraX, nextCameraY)
        position.set(cameraPoint, 0f)
        update()
    }

    fun moveCameraByOffset(offset: Float) {
        position.x = cameraPoint.x + offset * Constants.SCROLL_OFFSET
    }
}