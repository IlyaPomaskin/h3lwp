package com.homm3.livewallpaper.core

object GameConfig {
    const val TILE_SIZE = 32f
    const val FRAME_TIME = 0.18f
    const val BORDER_TILE_COUNT = 15
    const val FRUSTUM_PADDING_TILES = 4
    const val SCROLL_OFFSET = 3 * TILE_SIZE
    const val MIN_ZOOM = 0.1f
    const val MAX_ZOOM = 2.0f
    const val MIN_MAP_UPDATE_INTERVAL_MS = 1000f

    /** Largest supported map side in tiles (XL); bigger maps (H, XH, G) are too slow to build and are skipped. */
    const val MAX_MAP_SIZE = 144
}
