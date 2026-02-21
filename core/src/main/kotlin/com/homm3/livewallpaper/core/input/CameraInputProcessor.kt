package com.homm3.livewallpaper.core.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.GameConfig
import ktx.app.KtxInputAdapter
import kotlin.math.max
import kotlin.math.min

class CameraInputProcessor(private val viewport: ScreenViewport) : KtxInputAdapter {
    var onSpace: () -> Unit = {}
    var onEnter: () -> Unit = {}
    var onTap: () -> Unit = {}
    private val pressedKeys = mutableSetOf<Int>()
    private var dragged = false

    override fun keyDown(keycode: Int): Boolean {
        pressedKeys.add(keycode)
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        pressedKeys.remove(keycode)
        return true
    }

    fun handlePressedKeys() {
        var inputOccurred = false

        for (keycode in pressedKeys) {
            when (keycode) {
                Input.Keys.NUM_0 -> {
                    viewport.unitsPerPixel = 1f
                    inputOccurred = true
                }
                Input.Keys.EQUALS -> {
                    viewport.unitsPerPixel = max(
                        viewport.unitsPerPixel - 0.1f,
                        GameConfig.MIN_ZOOM
                    )
                    inputOccurred = true
                }
                Input.Keys.MINUS -> {
                    viewport.unitsPerPixel = min(
                        viewport.unitsPerPixel + 0.1f,
                        GameConfig.MAX_ZOOM
                    )
                    inputOccurred = true
                }
                Input.Keys.UP -> {
                    viewport.camera.translate(0f, -GameConfig.TILE_SIZE, 0f)
                    inputOccurred = true
                }
                Input.Keys.DOWN -> {
                    viewport.camera.translate(0f, GameConfig.TILE_SIZE, 0f)
                    inputOccurred = true
                }
                Input.Keys.LEFT -> {
                    viewport.camera.translate(-GameConfig.TILE_SIZE, 0f, 0f)
                    inputOccurred = true
                }
                Input.Keys.RIGHT -> {
                    viewport.camera.translate(GameConfig.TILE_SIZE, 0f, 0f)
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
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!dragged) {
            onTap()
        }
        return super.touchUp(screenX, screenY, pointer, button)
    }
}
