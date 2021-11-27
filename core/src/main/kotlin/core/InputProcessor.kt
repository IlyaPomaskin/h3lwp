package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxInputAdapter

class InputProcessor(private val viewport: ScreenViewport) : KtxInputAdapter {
    var onSpace: () -> Unit = {}
    var onEnter: () -> Unit = {}
    private var pressedKeyCode = 0

    override fun keyDown(keycode: Int): Boolean {
        pressedKeyCode = keycode
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        pressedKeyCode = Input.Keys.UNKNOWN
        return true
    }

    fun scrolled(amountX: Int): Boolean {
        return false;
    }

    fun handlePressedKeys() {
        when (pressedKeyCode) {
            Input.Keys.NUM_0 -> viewport.unitsPerPixel = 1f
            Input.Keys.EQUALS -> viewport.unitsPerPixel -= 0.1f
            Input.Keys.MINUS -> viewport.unitsPerPixel += 0.1f
            Input.Keys.UP -> viewport.camera.translate(0f, -Constants.TILE_SIZE, 0f)
            Input.Keys.DOWN -> viewport.camera.translate(0f, Constants.TILE_SIZE, 0f)
            Input.Keys.LEFT -> viewport.camera.translate(-Constants.TILE_SIZE, 0f, 0f)
            Input.Keys.RIGHT -> viewport.camera.translate(Constants.TILE_SIZE, 0f, 0f)
            Input.Keys.ENTER -> onEnter()
            Input.Keys.SPACE -> onSpace()
        }

        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        viewport.camera.translate(Gdx.input.deltaX.toFloat(), Gdx.input.deltaY.toFloat(), 0f)
        return super.touchDragged(screenX, screenY, pointer)
    }
}