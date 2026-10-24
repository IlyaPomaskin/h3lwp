package com.homm3.livewallpaper.core.input

import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.GameConfig
import kotlin.math.max
import kotlin.math.min

enum class ZoomAction { IN, OUT, RESET }

private const val ZOOM_STEP = 0.1f
private const val DEFAULT_UNITS_PER_PIXEL = 1f

/** Moves the camera by whole tiles; doesn't update the viewport. */
fun ScreenViewport.panByTiles(dx: Int, dy: Int) {
    camera.translate(dx * GameConfig.TILE_SIZE, dy * GameConfig.TILE_SIZE, 0f)
}

/** Moves the camera by fractions of the visible area; doesn't update the viewport. */
fun ScreenViewport.panByScreenFraction(fractionX: Float, fractionY: Float) {
    camera.translate(fractionX * worldWidth, fractionY * worldHeight, 0f)
}

/** Steps or resets the zoom within [GameConfig.MIN_ZOOM]..[GameConfig.MAX_ZOOM]; doesn't update the viewport. */
fun ScreenViewport.zoom(action: ZoomAction) {
    unitsPerPixel = when (action) {
        ZoomAction.IN -> max(unitsPerPixel - ZOOM_STEP, GameConfig.MIN_ZOOM)
        ZoomAction.OUT -> min(unitsPerPixel + ZOOM_STEP, GameConfig.MAX_ZOOM)
        ZoomAction.RESET -> DEFAULT_UNITS_PER_PIXEL
    }
}
