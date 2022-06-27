package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2

class Camera : OrthographicCamera() {
    private val cameraPoint = Vector2()

    init {
        setToOrtho(true)
    }

    fun set(x: Float, y: Float) {
        cameraPoint.set(x, y)
        position.set(cameraPoint, 0f)
        update()
    }

    fun moveCameraByOffset(offset: Float) {
        position.x = cameraPoint.x + offset * Constants.SCROLL_OFFSET
    }
}