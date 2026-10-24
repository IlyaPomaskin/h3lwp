package com.homm3.livewallpaper.core

import com.homm3.livewallpaper.parser.h3m.H3mVersion

object GameConfig {
    const val TILE_SIZE = 32f
    const val FRAME_TIME = 0.18f
    const val BORDER_TILE_COUNT = 15
    const val FRUSTUM_PADDING_TILES = 4
    const val SCROLL_OFFSET = 3 * TILE_SIZE
    const val MIN_ZOOM = 0.1f
    const val MAX_ZOOM = 2.0f
    const val MIN_MAP_UPDATE_INTERVAL_MS = 1000f

    /** Map sides in tiles (M..XL) of installed maps that start enabled; S and bigger than XL start disabled. */
    val DEFAULT_ENABLED_MAP_SIZES = 72..144

    /** Map formats the wallpaper shows; RoE and AB maps are skipped. */
    val SUPPORTED_MAP_VERSIONS = setOf(H3mVersion.SOD, H3mVersion.HOTA)
}
