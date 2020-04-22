package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import ktx.app.KtxInputAdapter

class InputProcessor(private val camera: OrthographicCamera) : KtxInputAdapter {
    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.NUM_0 -> camera.zoom = 1f
            Input.Keys.EQUALS -> camera.zoom -= 0.1f
            Input.Keys.MINUS -> camera.zoom += 0.1f
            Input.Keys.UP -> camera.translate(0f, -32f, 0f)
            Input.Keys.DOWN -> camera.translate(0f, 32f, 0f)
            Input.Keys.LEFT -> camera.translate(-32f, 0f, 0f)
            Input.Keys.RIGHT -> camera.translate(32f, 0f, 0f)
        }

        return super.keyDown(keycode)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        camera.translate(Gdx.input.deltaX.toFloat(), Gdx.input.deltaY.toFloat(), 0f)
        return super.touchDragged(screenX, screenY, pointer)
    }
}