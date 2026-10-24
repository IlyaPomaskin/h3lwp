package com.homm3.livewallpaper.core.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxInputAdapter

class CameraInputProcessor(private val viewport: ScreenViewport) : KtxInputAdapter {
    var onSpace: () -> Unit = {}
    var onEnter: () -> Unit = {}
    var onTap: (screenX: Int, screenY: Int) -> Unit = { _, _ -> }
    private val pressedKeys = mutableSetOf<Int>()
    private var dragged = false

    override fun keyDown(keycode: Int): Boolean {
        pressedKeys.add(keycode)
        Gdx.graphics.requestRendering()
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        pressedKeys.remove(keycode)
        return true
    }

    fun handlePressedKeys() {
        if (pressedKeys.isEmpty()) return
        Gdx.graphics.requestRendering()
        var inputOccurred = false

        for (keycode in pressedKeys) {
            when (keycode) {
                Input.Keys.NUM_0 -> {
                    viewport.zoom(ZoomAction.RESET)
                    inputOccurred = true
                }
                Input.Keys.EQUALS -> {
                    viewport.zoom(ZoomAction.IN)
                    inputOccurred = true
                }
                Input.Keys.MINUS -> {
                    viewport.zoom(ZoomAction.OUT)
                    inputOccurred = true
                }
                Input.Keys.UP -> {
                    viewport.panByTiles(0, -1)
                    inputOccurred = true
                }
                Input.Keys.DOWN -> {
                    viewport.panByTiles(0, 1)
                    inputOccurred = true
                }
                Input.Keys.LEFT -> {
                    viewport.panByTiles(-1, 0)
                    inputOccurred = true
                }
                Input.Keys.RIGHT -> {
                    viewport.panByTiles(1, 0)
                    inputOccurred = true
                }
                Input.Keys.ENTER -> onEnter()
                Input.Keys.SPACE -> onSpace()
            }
        }

        if (inputOccurred) {
            viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        dragged = false
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        dragged = true
        viewport.camera.translate(Gdx.input.deltaX.toFloat(), Gdx.input.deltaY.toFloat(), 0f)
        Gdx.graphics.requestRendering()
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!dragged) {
            onTap(screenX, screenY)
        }
        return super.touchUp(screenX, screenY, pointer, button)
    }
}
