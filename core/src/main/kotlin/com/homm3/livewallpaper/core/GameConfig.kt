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
}

object AssetPaths {
    const val ATLAS_FOLDER = "sprites"
    const val ATLAS_NAME = "images"
    const val ATLAS_PATH = "$ATLAS_FOLDER/$ATLAS_NAME.atlas"
    const val HOTA_ATLAS_NAME = "hota"
    const val HOTA_ATLAS_PATH = "$ATLAS_FOLDER/$HOTA_ATLAS_NAME.atlas"
    const val SKIN_PATH = "skin/uiskin.json"
    const val I18N_PATH = "i18n/Bundle"
    const val USER_MAPS_FOLDER = "user-maps"
}
